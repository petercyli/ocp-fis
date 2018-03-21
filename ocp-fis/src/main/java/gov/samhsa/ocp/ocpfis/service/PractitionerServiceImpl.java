package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.*;
import gov.samhsa.ocp.ocpfis.service.exception.*;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;


@Service
@Slf4j
public class PractitionerServiceImpl implements PractitionerService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    @Autowired
    private LookUpService lookUpService;

    @Autowired
    public PractitionerServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Practitioner.name());
        boolean firstPage = true;


        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);

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
    public PageDto<PractitionerDto> searchPractitioners(PractitionerController.SearchType type, String value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Practitioner.name());

        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);
        boolean firstPage = true;

        if (type.equals(PractitionerController.SearchType.name))
            practitionerIQuery.where(new StringClientParam("name").matches().value(value.trim()));

        if (type.equals(PractitionerController.SearchType.identifier))
            practitionerIQuery.where(new TokenClientParam("identifier").exactly().code(value.trim()));

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

        if (firstPagePractitionerSearchBundle == null || firstPagePractitionerSearchBundle.isEmpty() || firstPagePractitionerSearchBundle.getEntry().size() < 1) {
            throw new PractitionerNotFoundException("No practitioners were found in the FHIR server.");
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
    public List<ReferenceDto> getPractitionersInOrganizationByPractitionerId(String practitioner) {
        List<ReferenceDto> organizations = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("practitioner").hasId(ResourceType.Practitioner + "/" + practitioner))
                .include(PractitionerRole.INCLUDE_ORGANIZATION)
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> practitionerComponents = bundle.getEntry();

            if (practitionerComponents != null) {
                organizations = practitionerComponents.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.PractitionerRole))
                        .map(it -> (PractitionerRole) it.getResource())
                        .map(it -> (Organization) it.getOrganization().getResource())
                        .map(it -> FhirDtoUtil.mapOrganizationToReferenceDto(it))
                        .collect(toList());

            }
        }

        //retrieve practitioners for each of the organizations retrieved above.
        List<ReferenceDto> practitioners = organizations.stream()
                .map(it -> FhirDtoUtil.getIdFromReferenceDto(it, ResourceType.Practitioner))
                .flatMap(id -> getPractitionersByOrganization(id).stream())
                .map(practitionerDto -> FhirDtoUtil.mapPractitionerDtoToReferenceDto(practitionerDto))
                .distinct()
                .collect(toList());

        return practitioners;
    }

    public List<PractitionerDto> getPractitionersByOrganization(String organization) {
        List<PractitionerDto> practitioners = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("organization").hasId( organization))
                .include(PractitionerRole.INCLUDE_PRACTITIONER)
                .count(Integer.parseInt(fisProperties.getResourceSinglePageLimit()))
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> practitionerComponents = bundle.getEntry();

            if (practitionerComponents != null) {
                practitioners = practitionerComponents.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.Practitioner))
                        .map(it -> (Practitioner) it.getResource())
                        .map(it -> {
                            PractitionerDto dto = modelMapper.map(it, PractitionerDto.class);
                            dto.setLogicalId(it.getIdElement().getIdPart());
                            return dto;
                        })
                        .collect(toList());

            }
        }

        return practitioners;
    }

    @Override
    public List<ReferenceDto> getPractitionersByRole(String role) {
        List<ReferenceDto> practitioners = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(CareTeam.class)
                .include(CareTeam.INCLUDE_PARTICIPANT)
                .returnBundle(Bundle.class)
                .execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> components = bundle.getEntry();
            if (components != null) {
                practitioners = components.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.CareTeam))
                        .map(it -> (CareTeam) it.getResource())
                        .filter(it -> FhirUtil.checkParticipantRole(it.getParticipant(), role))
                        .flatMap(it -> it.getParticipant().stream())
                        .map(it -> {
                            Practitioner practitioner = (Practitioner) it.getMember().getResource();
                            return FhirDtoUtil.mapPractitionerToReferenceDto(practitioner);
                        })
                        .collect(toList());
            }
        }

        return practitioners;
    }

    @Override
    public void createPractitioner(PractitionerDto practitionerDto) {
        //Check Duplicate Identifier
        boolean hasDuplicateIdentifier = practitionerDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            if (fhirClient.search()
                    .forResource(Practitioner.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()))
                    .returnBundle(Bundle.class).execute().getTotal() > 0) {
                return true;
            }
            return false;
        });

        //When there is no duplicate identifier, practitioner gets created
        if (!hasDuplicateIdentifier) {
            //Create Fhir Practitioner
            Practitioner practitioner = modelMapper.map(practitionerDto, Practitioner.class);
            practitioner.setActive(true);

            final ValidationResult validationResult = fhirValidator.validateWithResult(practitioner);

            if (!validationResult.isSuccessful()) {
                throw new FHIRFormatErrorException("FHIR Practitioner Validation was not successful " + validationResult.getMessages());
            }

            MethodOutcome methodOutcome = fhirClient.create().resource(practitioner).execute();
            //Assign fhir Practitioner resource id.
            Reference practitionerId = new Reference();
            practitionerId.setReference("Practitioner/" + methodOutcome.getId().getIdPart());

            //Create PractitionerRole for the practitioner
            List<PractitionerRole> practitionerRoles = new ArrayList<>();
            practitionerDto.getPractitionerRoles().forEach(practitionerRoleDto -> {
                        PractitionerRole practitionerRole = new PractitionerRole();
                        practitionerRole = modelMapper.map(practitionerRoleDto, PractitionerRole.class);

                        //Set practitioner
                        practitionerRole.setPractitioner(practitionerId);

                        //set Code
                        CodeableConcept codeCodeableConcept = new CodeableConcept();
                        codeCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getCode().get(0), Coding.class));
                        practitionerRole.setCode(Arrays.asList(codeCodeableConcept));

                        //set Specialty
                        CodeableConcept specialtyCodeableConcept = new CodeableConcept();
                        specialtyCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getSpecialty().get(0), Coding.class));
                        practitionerRole.setSpecialty(Arrays.asList(specialtyCodeableConcept));
                        practitionerRoles.add(practitionerRole);
                        fhirClient.create().resource(practitionerRole).execute();
                    }
            );
        } else {
            throw new DuplicateResourceFoundException("Practitioner with the same Identifier is already present.");
        }
    }

    @Override
    public void updatePractitioner(String practitionerId, PractitionerDto practitionerDto) {
        //Check Duplicate Identifier
        boolean hasDuplicateIdentifier = practitionerDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            IQuery practitionersWithUpdatedIdentifierQuery = fhirClient.search()
                    .forResource(Practitioner.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()));
            Bundle practitionerWithUpdatedIdentifierBundle = (Bundle) practitionersWithUpdatedIdentifierQuery.returnBundle(Bundle.class).execute();
            Bundle practitionerWithUpdatedIdentifierAndSameResourceIdBundle = (Bundle) practitionersWithUpdatedIdentifierQuery.where(new TokenClientParam("_id").exactly().code(practitionerId)).returnBundle(Bundle.class).execute();
            if (practitionerWithUpdatedIdentifierBundle.getTotal() > 0) {
                if (practitionerWithUpdatedIdentifierBundle.getTotal() == practitionerWithUpdatedIdentifierAndSameResourceIdBundle.getTotal()) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        });


        Practitioner existingPractitioner = fhirClient.read().resource(Practitioner.class).withId(practitionerId.trim()).execute();

        if (!hasDuplicateIdentifier) {
            Practitioner updatedpractitioner = modelMapper.map(practitionerDto, Practitioner.class);
            existingPractitioner.setIdentifier(updatedpractitioner.getIdentifier());
            existingPractitioner.setName(updatedpractitioner.getName());
            existingPractitioner.setTelecom(updatedpractitioner.getTelecom());
            existingPractitioner.setAddress(updatedpractitioner.getAddress());

            final ValidationResult validationResult = fhirValidator.validateWithResult(existingPractitioner);

            if (!validationResult.isSuccessful()) {
                throw new FHIRFormatErrorException("FHIR Practitioner Validation was not successful " + validationResult.getMessages());
            }

            MethodOutcome methodOutcome = fhirClient.update().resource(existingPractitioner)
                    .execute();

           //Assign fhir Practitioner resource id.
            Reference practitionerReference = new Reference();
            practitionerReference.setReference("Practitioner/" + methodOutcome.getId().getIdPart());

            //Update PractitionerRole for the practitioner
            List<PractitionerRole> practitionerRoles = new ArrayList<>();
            practitionerDto.getPractitionerRoles().forEach(practitionerRoleDto -> {
                        PractitionerRole practitionerRole = new PractitionerRole();
                        practitionerRole = modelMapper.map(practitionerRoleDto, PractitionerRole.class);

                        //Set practitioner
                        practitionerRole.setPractitioner(practitionerReference);

                        //set Code
                        CodeableConcept codeCodeableConcept = new CodeableConcept();
                        codeCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getCode().get(0), Coding.class));
                        practitionerRole.setCode(Arrays.asList(codeCodeableConcept));

                        //set Specialty
                        CodeableConcept specialtyCodeableConcept = new CodeableConcept();
                        specialtyCodeableConcept.addCoding(modelMapper.map(practitionerRoleDto.getSpecialty().get(0), Coding.class));
                        practitionerRole.setSpecialty(Arrays.asList(specialtyCodeableConcept));
                        practitionerRoles.add(practitionerRole);
                        if(practitionerRoleDto.getLogicalId()!=null)
                        {
                            practitionerRole.setId(practitionerRoleDto.getLogicalId());
                            fhirClient.update().resource(practitionerRole).execute();
                        }
                        else
                            fhirClient.create().resource(practitionerRole).execute();

                    }
            );


        } else {
            throw new DuplicateResourceFoundException("Practitioner with the same Identifier is already present");
        }
    }

    @Override
    public PractitionerDto getPractitioner(String practitionerId) {
        Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().code(practitionerId))
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
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

    private PageDto<PractitionerDto> practitionersInPage(List<Bundle.BundleEntryComponent> retrievedPractitioners, Bundle otherPagePractitionerBundle, int numberOfPractitionersPerPage, boolean firstPage, Optional<Integer> page) {
        List<PractitionerDto> practitionersList = retrievedPractitioners.stream()
                .filter(retrievedPractitionerAndPractitionerRoles -> retrievedPractitionerAndPractitionerRoles.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(retrievedPractitioner -> {
                    PractitionerDto practitionerDto = modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class);
                    practitionerDto.setLogicalId(retrievedPractitioner.getResource().getIdElement().getIdPart());

                    //Getting practitioner role into practitioner dto
                    List<PractitionerRoleDto> practitionerRoleDtos = getPractitionerRolesForEachPractitioner(retrievedPractitioners, retrievedPractitioner.getResource().getIdElement().getIdPart());
                    practitionerDto.setPractitionerRoles(practitionerRoleDtos);

                    return practitionerDto;
                }).collect(toList());

        double totalPages = Math.ceil((double) otherPagePractitionerBundle.getTotal() / numberOfPractitionersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(practitionersList, numberOfPractitionersPerPage, totalPages, currentPage, practitionersList.size(),
                otherPagePractitionerBundle.getTotal());
    }

    private List<PractitionerRoleDto> getPractitionerRolesForEachPractitioner(List<Bundle.BundleEntryComponent> practitionersWithAllReferenceBundle, String practitionerId) {
        return practitionersWithAllReferenceBundle.stream()
                .filter(practionerWithAllReference -> practionerWithAllReference.getResource().getResourceType().equals(ResourceType.PractitionerRole))
                .map(practitionerRoleBundle -> {
                    PractitionerRole practitionerRole = (PractitionerRole) practitionerRoleBundle.getResource();
                    return practitionerRole;
                })
                .filter(practitionerRole -> practitionerRole.getPractitioner().getReference().equalsIgnoreCase("Practitioner/" + practitionerId))
                .map(practitionerRole -> {
                    PractitionerRoleDto practitionerRoleDto = new PractitionerRoleDto();
                    practitionerRoleDto = modelMapper.map(practitionerRole, PractitionerRoleDto.class);
                    practitionerRoleDto.setLogicalId(practitionerRole.getIdElement().getIdPart());
                    return practitionerRoleDto;
                }).collect(toList());
    }

    private Coding mapPractitionerRoleToCode(PractitionerRoleDto practitionerRoleDto) {
        ValueSetDto practitionerValueSet = getValueSetDtoByCode(practitionerRoleDto.getCode().get(0).getCode());
        return new Coding().setCode(practitionerValueSet.getCode()).setDisplay(practitionerValueSet.getDisplay()).setSystem(practitionerValueSet.getSystem());
    }

    private ValueSetDto getValueSetDtoByCode(String code) {
        List<ValueSetDto> practitionerRoleLookups = lookUpService.getPractitionerRoles();
        return practitionerRoleLookups.stream()
                .filter(practitionerRole -> practitionerRole.getCode().equalsIgnoreCase(code))
                .findAny()
                .orElseThrow(PractitionerRoleNotFoundException::new);
    }

}
