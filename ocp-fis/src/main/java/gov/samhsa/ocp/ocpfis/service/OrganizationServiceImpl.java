package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;

    public OrganizationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class);

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPageOrganizationSearchBundle;
        Bundle otherPageOrganizationSearchBundle;
        boolean firstPage = true;

        firstPageOrganizationSearchBundle = (Bundle) organizationIQuery
                .count(numberOfOrganizationsPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server");
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
        }).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    @Override
    public PageDto<OrganizationDto> searchOrganizations(OrganizationController.SearchType type, String value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class);

        if (type.equals(OrganizationController.SearchType.name))
            organizationIQuery.where(new StringClientParam("name").matches().value(value.trim()));

        if (type.equals(OrganizationController.SearchType.identifier))
            organizationIQuery.where(new TokenClientParam("identifier").exactly().code(value));

        if (type.equals(OrganizationController.SearchType.logicalId))
            organizationIQuery.where(new TokenClientParam("_id").exactly().code(value));

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
        }).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    private int getOrganizationsCountByIdentifier(String system, String code) {
        log.info("Searching organizations with identifier.system : " + system + " and code : " + code);
        IQuery searchQuery = fhirClient.search().forResource(Organization.class)
                .where(new TokenClientParam("identifier").exactly().systemAndCode(system, code));
        Bundle searchBundle = (Bundle) searchQuery.returnBundle(Bundle.class).execute();
        log.info("Total " + searchBundle.getTotal());
        return searchBundle.getTotal();
    }


    @Override
    public void createOrganization(OrganizationDto organizationDto) {

        //Check Duplicate Identifier
        int existingNumberOfOrganizations = this.getOrganizationsCountByIdentifier(organizationDto.getIdentifiers().get(0).getSystem(), organizationDto.getIdentifiers().get(0).getValue());
        String identifier = organizationDto.getIdentifiers().get(0).getValue();

        //When there is no duplicate identifier, the organization gets created
        if(existingNumberOfOrganizations == 0) {
            //Create Fhir Organization
            Organization fhirOrganization = modelMapper.map(organizationDto,Organization.class);
            fhirOrganization.setActive(Boolean.TRUE);

            final ValidationResult validationResult = fhirValidator.validateWithResult(fhirOrganization);
            if (validationResult.isSuccessful()) {
                MethodOutcome serverResponse = fhirClient.create().resource(fhirOrganization).execute();
                log.info("Created a new organization :" + serverResponse.getId().getIdPart());
            } else {
                throw new FHIRFormatErrorException("FHIR Organization Validation is not successful" + validationResult.getMessages());
            }
        }
        else{
            throw new DuplicateResourceFoundException("Organization with the Identifier " + identifier + " is already present.");
        }
    }

    @Override
    public void updateOrganization(String organizationId, OrganizationDto organizationDto) {
        log.info("Updating for Organization Id:" + organizationId);
        //Check Duplicate Identifier
        boolean hasDuplicateIdentifier = organizationDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            IQuery organizationsWithUpdatedIdentifierQuery = fhirClient.search()
                    .forResource(Organization.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()));
            Bundle organizationWithUpdatedIdentifierBundle = (Bundle) organizationsWithUpdatedIdentifierQuery.returnBundle(Bundle.class).execute();
            Bundle organizationWithUpdatedIdentifierAndSameResourceIdBundle = (Bundle) organizationsWithUpdatedIdentifierQuery.where(new TokenClientParam("_id").exactly().code(organizationId)).returnBundle(Bundle.class).execute();
            if (organizationWithUpdatedIdentifierBundle.getTotal() > 0) {
                return organizationWithUpdatedIdentifierBundle.getTotal() != organizationWithUpdatedIdentifierAndSameResourceIdBundle.getTotal();
            }
            return false;
        });

        Organization existingOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();

        if (!hasDuplicateIdentifier) {
            Organization updatedorganization = modelMapper.map(organizationDto, Organization.class);
            existingOrganization.setIdentifier(updatedorganization.getIdentifier());
            existingOrganization.setName(updatedorganization.getName());
            existingOrganization.setTelecom(updatedorganization.getTelecom());
            existingOrganization.setAddress(updatedorganization.getAddress());
            existingOrganization.setActive(updatedorganization.getActive());

            // Validate the resource
            final ValidationResult validationResult = fhirValidator.validateWithResult(existingOrganization);
            if (validationResult.isSuccessful()) {
                log.info("Update Organization: Validation successful? " + validationResult.isSuccessful() + " for OrganizationID:" + organizationId);

                fhirClient.update().resource(existingOrganization)
                        .execute();
            } else {
                throw new FHIRFormatErrorException("FHIR Organization Validation is not successful" + validationResult.getMessages());
            }
        }
        else {
            throw new DuplicateResourceFoundException("Organization with the Identifier " + organizationId + " is already present.");
        }
    }

    @Override
    public void inactivateOrganization(String organizationId) {
        log.info("Inactivating the organization Id: " + organizationId);
        Organization existingFhirOrganization = readOrganizationFromServer(organizationId);
        setOrganizationStatusToInactive(existingFhirOrganization);
    }

    private Organization readOrganizationFromServer(String organizationId) {
        Organization existingFhirOrganization;

        try {
            existingFhirOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();
        }
        catch (BaseServerResponseException e) {
            log.error("FHIR Client returned with an error while reading the organization with ID: " + organizationId);
            throw new ResourceNotFoundException("FHIR Client returned with an error while reading the organization:" + e.getMessage());
        }
        return existingFhirOrganization;
    }

    private void setOrganizationStatusToInactive(Organization existingFhirOrganization) {
        existingFhirOrganization.setActive(false);
        try {
            MethodOutcome serverResponse = fhirClient.update().resource(existingFhirOrganization).execute();
            log.info("Inactivated the organization :" + serverResponse.getId().getIdPart());
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT inactivate organization");
            throw new FHIRClientException("FHIR Client returned with an error while inactivating the organization:" + e.getMessage());
        }
    }
}
