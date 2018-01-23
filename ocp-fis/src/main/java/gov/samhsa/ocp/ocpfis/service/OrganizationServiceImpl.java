package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
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
    private final FisProperties ocpFisProperties;
    private final FhirValidator fhirValidator;

    public OrganizationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FisProperties fisProperties, FhirValidator fhirValidator) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.ocpFisProperties = fisProperties;
        this.fhirValidator = fhirValidator;
    }

    @Override
    public PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getOrganization().getPagination().getMaxSize()).orElse(ocpFisProperties.getOrganization().getPagination().getDefaultSize());
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
            otherPageOrganizationSearchBundle = getOrganizationSearchBundleAfterFirstPage(firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
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
        int numberOfOrganizationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getOrganization().getPagination().getMaxSize()).orElse(ocpFisProperties.getOrganization().getPagination().getDefaultSize());

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

            otherPageOrganizationSearchBundle = getOrganizationSearchBundleAfterFirstPage(firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
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
    public void updateOrganization(OrganizationDto organizationDto) {
        final Organization organization = modelMapper.map(organizationDto, Organization.class);
        organization.setId(new IdType(organizationDto.getLogicalId()));

        final ValidationResult validationResult = fhirValidator.validateWithResult(organization);
        if (validationResult.isSuccessful()) {
            log.debug("Calling FHIR Organization Update");

            fhirClient.update().resource(organization)
                    .execute();
        } else {
            throw new FHIRFormatErrorException("FHIR Organization Validation is not successful" + validationResult.getMessages());
        }
    }


    private Bundle getOrganizationSearchBundleAfterFirstPage(Bundle OrganizationSearchBundle, int page, int size) {
        if (OrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page >= 1 ? page : 1) - 1) * size;

            if (offset >= OrganizationSearchBundle.getTotal()) {
                throw new OrganizationNotFoundException("No organizations were found in the FHIR server for this page number");
            }

            String pageUrl = ocpFisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + OrganizationSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + size
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return OrganizationSearchBundle;
    }

}
