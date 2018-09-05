package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.NoDataFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.ProvenanceUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static ca.uhn.fhir.rest.api.Constants.PARAM_LASTUPDATED;
import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class PractitionerServiceImpl implements PractitionerService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    private final ProvenanceUtil provenanceUtil;

    @Autowired
    public PractitionerServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService, ProvenanceUtil provenanceUtil) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.provenanceUtil = provenanceUtil;
    }

    @Override
    public PageDto<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Practitioner.name());
        boolean firstPage = true;

        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);

        //Set Sort order
        practitionerIQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(practitionerIQuery, true);

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPagePractitionerBundle;
        Bundle otherPagePractitionerBundle;

        firstPagePractitionerBundle = (Bundle) practitionerIQuery
                .count(numberOfPractitionersPerPage)
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPagePractitionerBundle == null || firstPagePractitionerBundle.getEntry().size() < 1) {
            log.info("No practitioners were found for the given criteria.");
            return new PageDto<>(new ArrayList<>(), numberOfPractitionersPerPage, 0, 0, 0, 0);
        }

        otherPagePractitionerBundle = firstPagePractitionerBundle;

        if (page.isPresent() && page.get() > 1 && otherPagePractitionerBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            firstPage = false;
            otherPagePractitionerBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPagePractitionerBundle, page.get(), numberOfPractitionersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = otherPagePractitionerBundle.getEntry();

        return practitionersInPage(retrievedPractitioners, otherPagePractitionerBundle, numberOfPractitionersPerPage, firstPage, page);

    }


    @Override
    public PageDto<PractitionerDto> searchPractitioners(Optional<PractitionerController.SearchType> type, Optional<String> value, Optional<String> organization, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {
        int numberOfPractitionersPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Practitioner.name());
        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class).sort().descending(PARAM_LASTUPDATED);


        boolean firstPage = true;
        type.ifPresent(t -> {
                    if (t.equals(PractitionerController.SearchType.name))
                        value.ifPresent(v -> practitionerIQuery.where(new RichStringClientParam("name").contains().value(v.trim())));

                    if (t.equals(PractitionerController.SearchType.identifier))
                        value.ifPresent(v -> practitionerIQuery.where(new TokenClientParam("identifier").exactly().code(v.trim())));
                }
        );

        if (organization.isPresent()) {
            if (!practitionersFromOrg(organization.get()).isEmpty()) {
                practitionerIQuery.where(new TokenClientParam("_id").exactly().codes(practitionersFromOrg(organization.get())));
            } else {
                return new PageDto<>(new ArrayList<>(), numberOfPractitionersPerPage, 0, 0, 0, 0);
            }
        }

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPagePractitionerSearchBundle;
        Bundle otherPagePractitionerSearchBundle;

        firstPagePractitionerSearchBundle = (Bundle) practitionerIQuery.count(numberOfPractitionersPerPage)
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
                .returnBundle(Bundle.class)
                .execute();

        if (showAll.isPresent() && showAll.get()) {
            List<PractitionerDto> patientDtos = convertAllBundleToSinglePractitionerDtoList(firstPagePractitionerSearchBundle, numberOfPractitionersPerPage);
            return (PageDto<PractitionerDto>) PaginationUtil.applyPaginationForCustomArrayList(patientDtos, patientDtos.size(), Optional.of(1), false);
        }

        if (firstPagePractitionerSearchBundle == null || firstPagePractitionerSearchBundle.isEmpty() || firstPagePractitionerSearchBundle.getEntry().size() < 1) {
            return new PageDto<>(new ArrayList<>(), numberOfPractitionersPerPage, 0, 0, 0, 0);
        }

        otherPagePractitionerSearchBundle = firstPagePractitionerSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPagePractitionerSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPagePractitionerSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPagePractitionerSearchBundle, page.get(), numberOfPractitionersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = otherPagePractitionerSearchBundle.getEntry();

        return practitionersInPage(retrievedPractitioners, otherPagePractitionerSearchBundle, numberOfPractitionersPerPage, firstPage, page);
    }

    @Override
    public PractitionerDto findPractitioner(Optional<String> organization, String firstName, Optional<String> middleName, String lastName, String identifierType, String identifier) {
        IQuery iQuery = fhirClient.search().forResource(Practitioner.class).where(new StringClientParam("family").matches().value(lastName))
                .where(new TokenClientParam("identifier").exactly().systemAndCode(identifierType, identifier));

        //In Ocp both first name and middle name are saved in single column
        String givenName;
        if (middleName.isPresent()) {
            givenName = firstName + " " + middleName.get();
        } else {
            givenName = firstName;
        }
        iQuery.where(new StringClientParam("given").matches().value(givenName));

        organization.ifPresent(org -> iQuery.where(new TokenClientParam("_id").exactly().codes(practitionersInOrganization(org))));

        Bundle bundle = (Bundle) iQuery
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
                .returnBundle(Bundle.class)
                .execute();

        List<PractitionerDto> practitioners = bundle.getEntry().stream().filter(pr -> pr.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(prac -> this.covertEntryComponentToPractitioner(prac, bundle.getEntry())).collect(toList());
        if (!practitioners.isEmpty() && practitioners != null) {
            return practitioners.stream().findFirst().get();
        } else {
            throw new NoDataFoundException("No Patient with such data");
        }

    }

    @Override
    public List<ReferenceDto> getPractitionersInOrganizationByPractitionerId(Optional<String> practitioner, Optional<String> organization, Optional<String> location, Optional<String> role) {
        List<ReferenceDto> organizations = new ArrayList<>();
        if (organization.isPresent()) {
            IQuery iQuery = fhirClient.search().forResource(PractitionerRole.class)
                    .where(new ReferenceClientParam("organization").hasId(organization.get()));

            location.ifPresent(loc -> iQuery.where(new ReferenceClientParam("location").hasId(loc)));

            Bundle bundle = (Bundle) iQuery.include(PractitionerRole.INCLUDE_PRACTITIONER)
                    .sort().descending(PARAM_LASTUPDATED)
                    .returnBundle(Bundle.class).execute();

            if (bundle != null && !bundle.getEntry().isEmpty()) {
                return FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties)
                        .stream().filter(it -> it.getResource().getResourceType().equals(ResourceType.Practitioner))
                        .map(it -> {
                            Practitioner pr = (Practitioner) it.getResource();
                            ReferenceDto referenceDto = new ReferenceDto();
                            referenceDto.setReference("Practitioner/" + pr.getIdElement().getIdPart());
                            pr.getName().stream().findAny().ifPresent(name -> {
                                String ln = name.getFamily();
                                StringType fn = name.getGiven().stream().findAny().orElse(null);
                                assert ln != null;
                                referenceDto.setDisplay(fn+" "+ln.toString());
                            });
                            return referenceDto;
                        }).distinct().collect(toList());


            } else {
                log.info("No Practitioner available for this organization.");
                return new ArrayList<>();
            }

        } else if (practitioner.isPresent()) {
            IQuery iQuery = fhirClient.search().forResource(PractitionerRole.class)
                    .where(new ReferenceClientParam("practitioner").hasId(ResourceType.Practitioner + "/" + practitioner.get()));

            //role.ifPresent(r -> iQuery.where(new TokenClientParam("role").exactly().code(r)));

            Bundle bundle = (Bundle) iQuery.include(PractitionerRole.INCLUDE_ORGANIZATION)
                    .returnBundle(Bundle.class).execute();

            if (bundle != null) {
                List<Bundle.BundleEntryComponent> practitionerComponents = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);

                if (practitionerComponents != null) {
                    organizations = practitionerComponents.stream()
                            .filter(it -> it.getResource().getResourceType().equals(ResourceType.PractitionerRole))
                            .map(it -> (PractitionerRole) it.getResource())
                            .map(it -> (Organization) it.getOrganization().getResource())
                            .map(FhirDtoUtil::mapOrganizationToReferenceDto)
                            .collect(toList());

                }
            }

            //retrieve practitioners for each of the organizations retrieved above.

            return organizations.stream()
                    .map(it -> FhirDtoUtil.getIdFromReferenceDto(it, ResourceType.Practitioner))
                    .flatMap(id -> getPractitionersByOrganization(id).stream())
                    .map(FhirDtoUtil::mapPractitionerDtoToReferenceDto)
                    .distinct()
                    .collect(toList());
        }
        log.info("No Practitioner is found for this organization.");
        return new ArrayList<>();
    }

    @Override
    public PageDto<PractitionerDto> getPractitionersByOrganizationAndRole(String organization, Optional<String> role, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfPractitionersPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Practitioner.name());

        //Get the practitioners
        IQuery query = fhirClient.search().forResource(PractitionerRole.class).sort().descending(PARAM_LASTUPDATED);

        role.ifPresent(s -> query.where(new TokenClientParam("role").exactly().code(s)));

        List<Bundle.BundleEntryComponent> practitionerEntry = getBundleForPractitioners(organization, query);

        //Get the practitioners belonging to the organization
        List<String> practitionerIds = practitionerEntry.stream()
                .filter(retrievedPractitionerAndPractitionerRoles -> retrievedPractitionerAndPractitionerRoles.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(practitioner -> ((Practitioner) practitioner.getResource()).getIdElement().getIdPart())
                .collect(toList());


        //Get the practitioners along with the practitioner Roles and organizations in dto
        Bundle bundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().codes(practitionerIds))
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
                .sort().descending(PARAM_LASTUPDATED)
                .returnBundle(Bundle.class)
                .execute();


        List<Bundle.BundleEntryComponent> practitionerBundleEntry = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.ofNullable(numberOfPractitionersPerPage), fhirClient, fisProperties);

        List<PractitionerDto> practitioners = practitionerBundleEntry.stream()
                .filter(retrievedPractitionerAndPractitionerRoles -> retrievedPractitionerAndPractitionerRoles.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(retrievedPractitioner -> covertEntryComponentToPractitioner(retrievedPractitioner, practitionerBundleEntry))
                .collect(toList());

        return (PageDto<PractitionerDto>) PaginationUtil.applyPaginationForCustomArrayList(practitioners, numberOfPractitionersPerPage, pageNumber, false);
    }

    @Override
    public String getPractitionerByName(String name) {
        Bundle bundle = fhirClient.search().forResource(Practitioner.class)
                .where(new RichStringClientParam("name").matches().value(name))
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> bundleEntryComponents = bundle.getEntry();

        if (!bundleEntryComponents.isEmpty()) {
            return bundleEntryComponents.get(0).getResource().getIdElement().getIdPart();
        } else {
            return null;
        }
    }

    @Override
    public void assignLocationToPractitioner(String practitionerId, String organizationId, String locationId) {
        Location location = fhirClient.read().resource(Location.class).withId(locationId).execute();
        Bundle practitionerRoleBundle = fhirClient.search().forResource(PractitionerRole.class).where(new ReferenceClientParam("practitioner").hasId(practitionerId))
                .where(new ReferenceClientParam("organization").hasId(organizationId)).returnBundle(Bundle.class).execute();

        practitionerRoleBundle.getEntry().stream().findAny().ifPresent(role -> {
            PractitionerRole practitionerRole = (PractitionerRole) role.getResource();
            List<Reference> locations = practitionerRole.getLocation();
            ReferenceDto referenceDto = new ReferenceDto();
            referenceDto.setReference("Location/" + locationId);
            referenceDto.setDisplay(location.getName());
            locations.add(FhirDtoUtil.mapReferenceDtoToReference(referenceDto));
            practitionerRole.setLocation(locations);
            fhirClient.update().resource(practitionerRole).execute();
        });
    }

    @Override
    public void unassignLocationToPractitioner(String practitionerId, String organizationId, String locationId) {
        Bundle practitionerRoleBundle = fhirClient.search().forResource(PractitionerRole.class).where(new ReferenceClientParam("practitioner").hasId(practitionerId))
                .where(new ReferenceClientParam("organization").hasId(organizationId)).returnBundle(Bundle.class).execute();

        practitionerRoleBundle.getEntry().stream().findAny().ifPresent(role -> {
            PractitionerRole practitionerRole = (PractitionerRole) role.getResource();
            List<Reference> locations = practitionerRole.getLocation();
            locations.removeIf(location -> location.getReference().split("/")[1].equalsIgnoreCase(locationId));
            practitionerRole.setLocation(locations);
            fhirClient.update().resource(practitionerRole).execute();
        });
    }


    private List<PractitionerDto> getPractitionersByOrganization(String organizationId) {
        List<PractitionerDto> practitioners = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("organization").hasId(organizationId))
                .include(PractitionerRole.INCLUDE_PRACTITIONER)
                .sort().descending(PARAM_LASTUPDATED)
                .count(fisProperties.getResourceSinglePageLimit())
                .returnBundle(Bundle.class).execute();

        return getPractitionerDtos(practitioners, bundle.getEntry());
    }

    private List<Bundle.BundleEntryComponent> getBundleForPractitioners(String organization, IQuery query) {
        Bundle practitionerBundle = (Bundle) query.where(new ReferenceClientParam("organization").hasId(organization))
                .include(new Include("PractitionerRole:practitioner"))
                .returnBundle(Bundle.class)
                .execute();
        return FhirOperationUtil.getAllBundleComponentsAsList(practitionerBundle, Optional.empty(), fhirClient, fisProperties);
    }

    private List<PractitionerDto> getPractitionerDtos(List<PractitionerDto> practitioners, List<Bundle.BundleEntryComponent> bundleEntry) {
        if (bundleEntry != null && !bundleEntry.isEmpty()) {
            practitioners = bundleEntry.stream()
                    .filter(it -> it.getResource().getResourceType().equals(ResourceType.Practitioner))
                    .map(it -> (Practitioner) it.getResource())
                    .map(it -> {
                        PractitionerDto practitionerDto = modelMapper.map(it, PractitionerDto.class);
                        practitionerDto.setLogicalId(it.getIdElement().getIdPart());
                        return practitionerDto;
                    }).distinct()
                    .collect(toList());
        }
        return practitioners;
    }

    @Override
    public void createPractitioner(PractitionerDto practitionerDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();
        //Check Duplicate Identifier
        //When there is no duplicate identifier, practitioner gets created
        if (!hasDuplicateIdentifier(practitionerDto)) {
            //Create Fhir Practitioner
            Practitioner practitioner = modelMapper.map(practitionerDto, Practitioner.class);
            practitioner.setActive(true);

            //Set Profile Meta Data
            FhirProfileUtil.setPractitionerProfileMetaData(fhirClient, practitioner);

            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, practitioner, Optional.empty(), ResourceType.Practitioner.name(), "Create Practitioner");

            //Create
            MethodOutcome methodOutcome = FhirOperationUtil.createFhirResource(fhirClient, practitioner, ResourceType.Practitioner.name());
            idList.add(ResourceType.Practitioner.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

            //Assign fhir Practitioner resource id.
            Reference practitionerId = new Reference();
            practitionerId.setReference("Practitioner/" + methodOutcome.getId().getIdPart());

            //Create PractitionerRole for the practitioner
            practitionerDto.getPractitionerRoles().forEach(practitionerRoleDto -> {
                        PractitionerRole practitionerRole;
                        practitionerRole = modelMapper.map(practitionerRoleDto, PractitionerRole.class);

                        //Set practitioner
                        practitionerRole.setPractitioner(practitionerId);

                        //set Code
                        CodeableConcept codeCodeableConcept = new CodeableConcept();
                        codeCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getCode().get(0), Coding.class));
                        practitionerRole.setCode(Collections.singletonList(codeCodeableConcept));

                        //set Specialty
                        CodeableConcept specialtyCodeableConcept = new CodeableConcept();
                        specialtyCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getSpecialty().get(0), Coding.class));
                        practitionerRole.setSpecialty(Collections.singletonList(specialtyCodeableConcept));

                        //Set Profile Meta Data
                        FhirProfileUtil.setPractitionerRoleProfileMetaData(fhirClient, practitionerRole);

                        //Validate
                        FhirOperationUtil.validateFhirResource(fhirValidator, practitionerRole, Optional.empty(), ResourceType.PractitionerRole.name(), "Create Practitioner Role");

                        //Create
                        MethodOutcome practitionerMethodOutcome = FhirOperationUtil.createFhirResource(fhirClient, practitionerRole, ResourceType.PractitionerRole.name());
                        idList.add(ResourceType.PractitionerRole.name() + "/" + FhirOperationUtil.getFhirId(practitionerMethodOutcome));
                    }
            );

            if (fisProperties.isProvenanceEnabled()) {
                provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.CREATE, loggedInUser);
            }

        } else {
            throw new DuplicateResourceFoundException("Practitioner with the same Identifier is already present.");
        }
    }

    @Override
    public void updatePractitioner(String practitionerId, PractitionerDto practitionerDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();

        Practitioner existingPractitioner = fhirClient.read().resource(Practitioner.class).withId(practitionerId.trim()).execute();
        practitionerDto.setLogicalId(practitionerId.trim());

        if (!isDuplicateWhileUpdate(practitionerDto)) {
            Practitioner updatedPractitioner = modelMapper.map(practitionerDto, Practitioner.class);
            existingPractitioner.setIdentifier(updatedPractitioner.getIdentifier());
            existingPractitioner.setName(updatedPractitioner.getName());
            existingPractitioner.setTelecom(updatedPractitioner.getTelecom());
            existingPractitioner.setAddress(updatedPractitioner.getAddress());

            //Set Profile Meta Data
            FhirProfileUtil.setPractitionerProfileMetaData(fhirClient, existingPractitioner);

            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, existingPractitioner, Optional.of(practitionerId), ResourceType.Practitioner.name(), "Update Practitioner");

            //Update
            MethodOutcome methodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, existingPractitioner, "Update Practitioner");
            idList.add(ResourceType.Practitioner.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

            //Assign fhir Practitioner resource id.
            Reference practitionerReference = new Reference();
            practitionerReference.setReference("Practitioner/" + methodOutcome.getId().getIdPart());

            //Update PractitionerRole for the practitioner
            practitionerDto.getPractitionerRoles().forEach(practitionerRoleDto -> {
                        PractitionerRole practitionerRole;
                        practitionerRole = modelMapper.map(practitionerRoleDto, PractitionerRole.class);

                        //Set Practitioner
                        practitionerRole.setPractitioner(practitionerReference);

                        //Set Code
                        CodeableConcept codeCodeableConcept = new CodeableConcept();
                        codeCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getCode().get(0), Coding.class));
                        practitionerRole.setCode(Collections.singletonList(codeCodeableConcept));

                        //Set Specialty
                        CodeableConcept specialtyCodeableConcept = new CodeableConcept();
                        specialtyCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getSpecialty().get(0), Coding.class));
                        practitionerRole.setSpecialty(Collections.singletonList(specialtyCodeableConcept));

                        //Set Profile Meta Data
                        FhirProfileUtil.setPractitionerRoleProfileMetaData(fhirClient, practitionerRole);

                        if (practitionerRoleDto.getLogicalId() != null) {
                            practitionerRole.setId(practitionerRoleDto.getLogicalId());
                            // Validate
                            FhirOperationUtil.validateFhirResource(fhirValidator, practitionerRole, Optional.empty(), ResourceType.PractitionerRole.name(), "Update Practitioner Role(When updating Practitioner)");

                            //Update
                            MethodOutcome practitionerRoleMethodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, practitionerRole, "Update Practitioner Role");
                            idList.add(ResourceType.PractitionerRole.name() + "/" + FhirOperationUtil.getFhirId(practitionerRoleMethodOutcome));

                        } else {
                            // Validate
                            FhirOperationUtil.validateFhirResource(fhirValidator, practitionerRole, Optional.empty(), ResourceType.PractitionerRole.name(), "Create Practitioner Role(When updating Practitioner)");

                            //Create
                            MethodOutcome practitionerRoleMethodOutcome = FhirOperationUtil.createFhirResource(fhirClient, practitionerRole, ResourceType.PractitionerRole.name());
                            idList.add(ResourceType.PractitionerRole.name() + "/" + FhirOperationUtil.getFhirId(practitionerRoleMethodOutcome));

                        }
                    }
            );

            if (fisProperties.isProvenanceEnabled()) {
                provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.UPDATE, loggedInUser);
            }

        } else {
            throw new DuplicateResourceFoundException("Practitioner with the same Identifier is already present");
        }
    }

    @Override
    public PractitionerDto getPractitioner(String practitionerId) {
        Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().code(practitionerId))
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
                .sort().descending(PARAM_LASTUPDATED)
                .returnBundle(Bundle.class)
                .execute();

        if (practitionerBundle == null || practitionerBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No practitioner was found for the givecn practitionerID:" + practitionerId);
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = practitionerBundle.getEntry();
        Bundle.BundleEntryComponent retrievedPractitioner = practitionerBundle.getEntry().get(0);

        PractitionerDto practitionerDto = modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class);
        practitionerDto.setLogicalId(retrievedPractitioner.getResource().getIdElement().getIdPart());

        //Get Practitioner Role for the practitioner.
        List<PractitionerRoleDto> practitionerRoleDtos = getPractitionerRolesForEachPractitioner(retrievedPractitioners, retrievedPractitioner.getResource().getIdElement().getIdPart());
        practitionerDto.setPractitionerRoles(practitionerRoleDtos);

        return practitionerDto;
    }

    @Override
    public PractitionerDto getPractitionerDemographicsOnly(String practitionerId) {
        Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().code(practitionerId))
                .returnBundle(Bundle.class)
                .execute();

        if (practitionerBundle == null || practitionerBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No practitioner was found for the givecn practitionerID:" + practitionerId);
        }

        Bundle.BundleEntryComponent retrievedPractitioner = practitionerBundle.getEntry().get(0);

        PractitionerDto practitionerDto = modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class);
        practitionerDto.setLogicalId(retrievedPractitioner.getResource().getIdElement().getIdPart());

        return practitionerDto;
    }

    private PageDto<PractitionerDto> practitionersInPage(List<Bundle.BundleEntryComponent> retrievedPractitioners, Bundle otherPagePractitionerBundle, int numberOfPractitionersPerPage, boolean firstPage, Optional<Integer> page) {
        List<PractitionerDto> practitionersList = retrievedPractitioners.stream()
                .filter(retrievedPractitionerAndPractitionerRoles -> retrievedPractitionerAndPractitionerRoles.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(retrievedPractitioner -> covertEntryComponentToPractitioner(retrievedPractitioner, retrievedPractitioners))
                .collect(toList());

        double totalPages = Math.ceil((double) otherPagePractitionerBundle.getTotal() / numberOfPractitionersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(practitionersList, numberOfPractitionersPerPage, totalPages, currentPage, practitionersList.size(),
                otherPagePractitionerBundle.getTotal());
    }

    private List<PractitionerRoleDto> getPractitionerRolesForEachPractitioner(List<Bundle.BundleEntryComponent> practitionersWithAllReferenceBundle, String practitionerId) {
        return practitionersWithAllReferenceBundle.stream()
                .filter(practitionerWithAllReference -> practitionerWithAllReference.getResource().getResourceType().equals(ResourceType.PractitionerRole))
                .map(practitionerRoleBundle -> (PractitionerRole) practitionerRoleBundle.getResource())
                .filter(practitionerRole -> practitionerRole.getPractitioner().getReference().equalsIgnoreCase("Practitioner/" + practitionerId))
                .map(practitionerRole -> {
                    PractitionerRoleDto practitionerRoleDto;
                    practitionerRoleDto = modelMapper.map(practitionerRole, PractitionerRoleDto.class);
                    practitionerRoleDto.setLogicalId(practitionerRole.getIdElement().getIdPart());
                    return practitionerRoleDto;
                }).collect(toList());
    }

    private List<String> practitionersFromOrg(String org) {
        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("organization").hasId("Organization/" + org))
                .sort().descending(PARAM_LASTUPDATED)
                .returnBundle(Bundle.class)
                .execute();
        return FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties)
                .stream().map(pr -> {
                    PractitionerRole practitionerRole = (PractitionerRole) pr.getResource();
                    return practitionerRole.getPractitioner().getReference().split("/")[1];
                }).collect(toList());
    }

    private List<PractitionerDto> convertAllBundleToSinglePractitionerDtoList(Bundle firstPageSearchBundle, int numberOfBundlePerPage) {
        List<Bundle.BundleEntryComponent> bundleEntryComponents = FhirOperationUtil.getAllBundleComponentsAsList(firstPageSearchBundle, Optional.of(numberOfBundlePerPage), fhirClient, fisProperties);
        return bundleEntryComponents.stream().filter(pr -> pr.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(prac -> this.covertEntryComponentToPractitioner(prac, bundleEntryComponents)).collect(toList());
    }

    private PractitionerDto covertEntryComponentToPractitioner(Bundle.BundleEntryComponent practitionerComponent, List<Bundle.BundleEntryComponent> practitionerAndPractitionerRoleList) {
        PractitionerDto practitionerDto = modelMapper.map(practitionerComponent.getResource(), PractitionerDto.class);
        practitionerDto.setLogicalId(practitionerComponent.getResource().getIdElement().getIdPart());
        //Getting practitioner role into practitioner dto
        List<PractitionerRoleDto> practitionerRoleDtos = getPractitionerRolesForEachPractitioner(practitionerAndPractitionerRoleList, practitionerComponent.getResource().getIdElement().getIdPart());
        practitionerDto.setPractitionerRoles(practitionerRoleDtos);
        return practitionerDto;
    }

    private boolean hasDuplicateIdentifier(PractitionerDto practitionerDto) {
        return practitionerDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            if (fhirClient.search()
                    .forResource(Practitioner.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()))
                    .returnBundle(Bundle.class).execute().getTotal() > 0) {
                return true;
            } else {
                Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class).returnBundle(Bundle.class).execute();
                return !FhirOperationUtil.getAllBundleComponentsAsList(practitionerBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(practitioner -> {
                    Practitioner p = (Practitioner) practitioner.getResource();
                    return p.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(identifierDto.getSystem()) && identifier.getValue().replaceAll(" ", "")
                            .replaceAll("-", "").trim()
                            .equalsIgnoreCase(identifierDto.getValue().replaceAll(" ", "").replaceAll("-", "").trim()));
                }).collect(toList()).isEmpty();
            }
        });
    }

    private boolean isDuplicateWhileUpdate(PractitionerDto practitionerDto) {
        final Practitioner practitioner = fhirClient.read().resource(Practitioner.class).withId(practitionerDto.getLogicalId()).execute();

        Bundle searchPractitioner = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(Practitioner.class).where(Practitioner.IDENTIFIER.exactly().systemAndIdentifier(practitionerDto.getIdentifiers().stream().findFirst().get().getSystem(), practitionerDto.getIdentifiers().stream().findFirst().get().getValue())))
                .returnBundle(Bundle.class).execute();

        if (!searchPractitioner.getEntry().isEmpty()) {
            return !practitioner.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(practitionerDto.getIdentifiers().stream().findFirst().get().getSystem())
                    && identifier.getValue().equalsIgnoreCase(practitionerDto.getIdentifiers().stream().findFirst().get().getValue()));
        } else {
            Bundle praBundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(Practitioner.class)).returnBundle(Bundle.class).execute();
            List<Bundle.BundleEntryComponent> bundleEntryComponents = FhirOperationUtil.getAllBundleComponentsAsList(praBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(pat -> {
                Practitioner p = (Practitioner) pat.getResource();
                return p.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(practitionerDto.getIdentifiers().stream().findFirst().get().getSystem()) && identifier.getValue().replaceAll(" ", "")
                        .replaceAll("-", "").trim()
                        .equalsIgnoreCase(practitionerDto.getIdentifiers().stream().findFirst().get().getValue().replaceAll(" ", "").replaceAll("-", "").trim()));
            }).collect(toList());
            if (bundleEntryComponents.isEmpty()) {
                return false;
            } else {
                return !bundleEntryComponents.stream().anyMatch(resource -> {
                    Practitioner pRes = (Practitioner) resource.getResource();
                    return pRes.getIdElement().getIdPart().equalsIgnoreCase(practitioner.getIdElement().getIdPart());
                });
            }
        }
    }

    private List<String> practitionersInOrganization(String organization) {
        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("organization").hasId(organization))
                .returnBundle(Bundle.class)
                .execute();
        return bundle.getEntry().stream()
                .map(e -> ((PractitionerRole) e.getResource()).getPractitioner().getReference().split("/")[1])
                .distinct().collect(toList());
    }
}
