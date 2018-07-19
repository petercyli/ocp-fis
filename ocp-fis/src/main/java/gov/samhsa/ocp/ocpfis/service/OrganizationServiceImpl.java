package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ca.uhn.fhir.rest.api.Constants.PARAM_LASTUPDATED;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {


    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;

    private final LookUpService lookUpService;

    @Autowired
    public OrganizationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService = lookUpService;
    }

    @Override
    public OrganizationDto getOrganization(String organizationId) {
        final Organization retrievedOrganization = fhirClient.read().resource(Organization.class).withId(organizationId).execute();
        if (retrievedOrganization == null || retrievedOrganization.isEmpty()) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server.");
        }
        final OrganizationDto organizationDto = modelMapper.map(retrievedOrganization, OrganizationDto.class);
        organizationDto.setLogicalId(retrievedOrganization.getIdElement().getIdPart());
        return organizationDto;
    }

    @Override
    public PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class);

        //Set Sort order
        organizationIQuery = FhirUtil.setLastUpdatedTimeSortOrder(organizationIQuery, true);

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPageOrganizationSearchBundle;
        Bundle otherPageOrganizationSearchBundle;
        boolean firstPage = true;

        firstPageOrganizationSearchBundle = PaginationUtil.getSearchBundleFirstPage(organizationIQuery, numberOfOrganizationsPerPage, Optional.empty());

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            log.info("No organizations were found for the given criteria.");
            return new PageDto<>(new ArrayList<>(), numberOfOrganizationsPerPage, 0, 0, 0, 0);
        }

        otherPageOrganizationSearchBundle = firstPageOrganizationSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPageOrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            // Load the required page
            otherPageOrganizationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedOrganizations = otherPageOrganizationSearchBundle.getEntry();

        List<OrganizationDto> organizationsList = retrievedOrganizations.stream().map(retrievedOrganization -> {
            OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    @Override
    public PageDto<OrganizationDto> searchOrganizations(Optional<OrganizationController.SearchType> type, Optional<String> value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class).sort().descending(PARAM_LASTUPDATED);

        type.ifPresent(t -> {
                    if (t.equals(OrganizationController.SearchType.name))
                        value.ifPresent(v -> organizationIQuery.where(new RichStringClientParam("name").contains().value(v.trim())));

                    if (t.equals(OrganizationController.SearchType.identifier))
                        value.ifPresent(v -> organizationIQuery.where(new TokenClientParam("identifier").exactly().code(v)));

                    if (t.equals(OrganizationController.SearchType.logicalId))
                        value.ifPresent(v -> organizationIQuery.where(new TokenClientParam("_id").exactly().code(v)));
                }
        );

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPageOrganizationSearchBundle;
        Bundle otherPageOrganizationSearchBundle;
        boolean firstPage = true;

        firstPageOrganizationSearchBundle = (Bundle) organizationIQuery.count(numberOfOrganizationsPerPage).returnBundle(Bundle.class)
                .execute();

        if (showAll.isPresent() && showAll.get()) {
            List<OrganizationDto> organizationDtos = convertAllBundleToSingleOrganizationDtoList(firstPageOrganizationSearchBundle, numberOfOrganizationsPerPage);
            return (PageDto<OrganizationDto>) PaginationUtil.applyPaginationForCustomArrayList(organizationDtos, organizationDtos.size(), Optional.of(1), false);
        }

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.isEmpty() || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server.");
        }

        otherPageOrganizationSearchBundle = firstPageOrganizationSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPageOrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;

            otherPageOrganizationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedOrganizations = otherPageOrganizationSearchBundle.getEntry();

        List<OrganizationDto> organizationsList = retrievedOrganizations.stream().map(retrievedOrganization -> {
            OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    private int getOrganizationsCountByIdentifier(String system, String code) {
        log.info("Searching organizations with identifier.system : " + system + " and code : " + code);

        Bundle searchBundle = fhirClient.search().forResource(Organization.class).where(new TokenClientParam("identifier").exactly().systemAndCode(system, code))
                .returnBundle(Bundle.class).execute();
        if (searchBundle.getTotal() == 0) {
            Bundle organizationBundle = fhirClient.search().forResource(Organization.class).returnBundle(Bundle.class).execute();
            return FhirUtil.getAllBundleComponentsAsList(organizationBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(organization -> {
                Organization o = (Organization) organization.getResource();
                return o.getIdentifier().stream().anyMatch(identifier ->
                        (identifier.getSystem().equalsIgnoreCase(system) &&
                                identifier.getValue().replaceAll(" ", "")
                                        .replaceAll("-", "").trim()
                                        .equalsIgnoreCase(code.replaceAll(" ", "").replaceAll("-", "").trim()))
                );
            }).collect(toList()).size();
        } else {
            return searchBundle.getTotal();
        }
    }


    @Override
    public void createOrganization(OrganizationDto organizationDto) {

        //Check Duplicate Identifier
        int existingNumberOfOrganizations = this.getOrganizationsCountByIdentifier(organizationDto.getIdentifiers().get(0).getSystem(), organizationDto.getIdentifiers().get(0).getValue());
        String identifier = organizationDto.getIdentifiers().get(0).getValue();

        //When there is no duplicate identifier, the organization gets created
        if (existingNumberOfOrganizations == 0) {
            // Map
            Organization fhirOrganization = modelMapper.map(organizationDto, Organization.class);
            fhirOrganization.setActive(Boolean.TRUE);

            // Validate
            if (fisProperties.getFhir().isValidateResourceAgainstStructureDefinition()) {
                //Set Profile Meta Data
                FhirProfileUtil.setOrganizationProfileMetaData(fhirClient, fhirOrganization);
            }
            FhirUtil.validateFhirResource(fhirValidator, fhirOrganization, Optional.empty(), ResourceType.Organization.name(), "Create Organization");

            //Create
            MethodOutcome serverResponse = FhirUtil.createFhirResource(fhirClient, fhirOrganization, ResourceType.Organization.name());

            // Add TO DO Activity Definition
            ActivityDefinition activityDefinition = FhirUtil.createToDoActivityDefinition(serverResponse.getId().getIdPart(), fisProperties, lookUpService, fhirClient);

            // Validate TO DO Activity Definition
            if (fisProperties.getFhir().isValidateResourceAgainstStructureDefinition()) {
                //Set Profile Meta Data
                FhirProfileUtil.setActivityDefinitionProfileMetaData(fhirClient, activityDefinition);
            }
            FhirUtil.validateFhirResource(fhirValidator, activityDefinition, Optional.empty(), ResourceType.ActivityDefinition.name(), "Create ActivityDefinition (when creating an Organization)");

            //Create TO DO Activity Definition
            FhirUtil.createFhirResource(fhirClient, activityDefinition, ResourceType.ActivityDefinition.name());


            fhirClient.create().resource(activityDefinition).execute();

        } else {
            throw new DuplicateResourceFoundException("Organization with the Identifier " + identifier + " is already present.");
        }
    }

    @Override
    public void updateOrganization(String organizationId, OrganizationDto organizationDto) {
        log.info("Updating the Organization with Id:" + organizationId);

        Organization existingOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();

        if (!isDuplicateWhileUpdate(organizationDto)) {
            Organization updatedOrganization = modelMapper.map(organizationDto, Organization.class);
            existingOrganization.setIdentifier(updatedOrganization.getIdentifier());
            existingOrganization.setName(updatedOrganization.getName());
            existingOrganization.setTelecom(updatedOrganization.getTelecom());
            existingOrganization.setAddress(updatedOrganization.getAddress());
            existingOrganization.setActive(updatedOrganization.getActive());

            // Validate
            if (fisProperties.getFhir().isValidateResourceAgainstStructureDefinition()) {
                //Set Profile Meta Data
                FhirProfileUtil.setOrganizationProfileMetaData(fhirClient, existingOrganization);
            }
            FhirUtil.validateFhirResource(fhirValidator, existingOrganization, Optional.of(organizationId), ResourceType.Organization.name(), "Update Organization");

            //Update
            FhirUtil.updateFhirResource(fhirClient, existingOrganization, "Update Organization");
        } else {
            throw new DuplicateResourceFoundException("Organization with the Identifier " + organizationId + " is already present.");
        }
    }

    @Override
    public void inactivateOrganization(String organizationId) {
        log.info("Inactivating the organization Id: " + organizationId);
        Organization existingFhirOrganization = readOrganizationFromServer(organizationId);
        setOrganizationStatusToInactive(existingFhirOrganization);
    }

    @Override
    public List<ReferenceDto> getOrganizationsByPractitioner(String practitioner) {
        List<ReferenceDto> organizations = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("practitioner").hasId(ResourceType.Practitioner + "/" + practitioner))
                .include(PractitionerRole.INCLUDE_ORGANIZATION)
                .sort().descending(PARAM_LASTUPDATED)
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> organizationComponents = FhirUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);

            if (organizationComponents != null) {
                organizations = organizationComponents.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.PractitionerRole))
                        .map(it -> (PractitionerRole) it.getResource())
                        .map(it -> (Organization) it.getOrganization().getResource())
                        .map(FhirDtoUtil::mapOrganizationToReferenceDto)
                        .distinct()
                        .collect(toList());

            }
        }

        return organizations;
    }

    private Organization readOrganizationFromServer(String organizationId) {
        Organization existingFhirOrganization;

        try {
            existingFhirOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();
        } catch (BaseServerResponseException e) {
            log.error("FHIR Client returned with an error while reading the organization with ID: " + organizationId);
            throw new ResourceNotFoundException("FHIR Client returned with an error while reading the organization:" + e.getMessage());
        }
        return existingFhirOrganization;
    }

    private void setOrganizationStatusToInactive(Organization existingFhirOrganization) {
        existingFhirOrganization.setActive(false);

        // Validate
        if (fisProperties.getFhir().isValidateResourceAgainstStructureDefinition()) {
            //Set Profile Meta Data
            FhirProfileUtil.setOrganizationProfileMetaData(fhirClient, existingFhirOrganization);
        }
        FhirUtil.validateFhirResource(fhirValidator, existingFhirOrganization, Optional.of(existingFhirOrganization.getId()), ResourceType.Organization.name(), "Update Organization");

        //Update
        FhirUtil.updateFhirResource(fhirClient, existingFhirOrganization, "Inactivate Organization");
    }

    private List<OrganizationDto> convertAllBundleToSingleOrganizationDtoList(Bundle firstPageOrganizationSearchBundle, int numberOBundlePerPage) {
        return FhirUtil.getAllBundleComponentsAsList(firstPageOrganizationSearchBundle, Optional.of(numberOBundlePerPage), fhirClient, fisProperties)
                .stream()
                .map(retrievedOrganization -> {
                    OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
                    organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
                    return organizationDto;
                })
                .collect(toList());
    }

    private boolean isDuplicateWhileUpdate(OrganizationDto organizationDto) {
        final Organization organization = fhirClient.read().resource(Organization.class).withId(organizationDto.getLogicalId()).execute();

        Bundle searchOrganization = (Bundle) FhirUtil.setNoCacheControlDirective(fhirClient.search().forResource(Organization.class)
                .where(Organization.IDENTIFIER.exactly().systemAndIdentifier(organizationDto.getIdentifiers().stream().findFirst().get().getSystem(), organizationDto.getIdentifiers().stream().findFirst().get().getValue())))
                .returnBundle(Bundle.class).execute();

        if (!searchOrganization.getEntry().isEmpty()) {
            return !organization.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getSystem())
                    && identifier.getValue().equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getValue()));
        } else {
            Bundle organizationBundle = (Bundle) FhirUtil.setNoCacheControlDirective(fhirClient.search().forResource(Organization.class)).returnBundle(Bundle.class).execute();
            List<Bundle.BundleEntryComponent> bundleEntryComponents = FhirUtil.getAllBundleComponentsAsList(organizationBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(org -> {
                Organization o = (Organization) org.getResource();
                return o.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getSystem()) && identifier.getValue().replaceAll(" ", "")
                        .replaceAll("-", "").trim()
                        .equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getValue().replaceAll(" ", "").replaceAll("-", "").trim()));
            }).collect(toList());
            if (bundleEntryComponents.isEmpty()) {
                return false;
            } else {
                return !bundleEntryComponents.stream().anyMatch(resource -> {
                    Organization oRes = (Organization) resource.getResource();
                    return oRes.getIdElement().getIdPart().equalsIgnoreCase(organization.getIdElement().getIdPart());
                });
            }
        }
    }
}
