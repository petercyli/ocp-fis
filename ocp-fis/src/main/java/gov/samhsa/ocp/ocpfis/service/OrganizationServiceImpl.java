package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.OcpFisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService{

    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final OcpFisProperties ocpFisProperties;


    public OrganizationServiceImpl(ModelMapper modelMapper , IGenericClient fhirClient, OcpFisProperties ocpFisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.ocpFisProperties = ocpFisProperties;

    }

    @Override
    public List<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
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

        firstPageOrganizationSearchBundle = (Bundle) organizationIQuery
                .count(numberOfOrganizationsPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server");
        }

        otherPageOrganizationSearchBundle = firstPageOrganizationSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPageOrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            otherPageOrganizationSearchBundle = getOrganizationSearchBundleAfterFirstPage(firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedOrganizations = otherPageOrganizationSearchBundle.getEntry();

        return retrievedOrganizations.stream().map(retrievedOrganization -> {
            OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        } ).collect(Collectors.toList());

    }

    @Override
    public List<OrganizationDto> searchOrganizations(OrganizationController.SearchType type, String value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
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

        firstPageOrganizationSearchBundle = (Bundle) organizationIQuery.count(numberOfOrganizationsPerPage).returnBundle(Bundle.class)
                .execute();

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.isEmpty() || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server.");
        }

        otherPageOrganizationSearchBundle = firstPageOrganizationSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPageOrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            otherPageOrganizationSearchBundle = getOrganizationSearchBundleAfterFirstPage(firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedOrganizations = otherPageOrganizationSearchBundle.getEntry();

        return retrievedOrganizations.stream().map(retrievedOrganization -> {
            OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        } ).collect(Collectors.toList());

    }


    private Bundle getOrganizationSearchBundleAfterFirstPage(Bundle OrganizationSearchBundle, int page, int size) {
        if (OrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page >= 1 ? page : 1) - 1) * size;

            if (offset >= OrganizationSearchBundle.getTotal()) {
                throw new OrganizationNotFoundException("No practitioners were found in the FHIR server for this page number");
            }

            String pageUrl = ocpFisProperties.getFhir().getPublish().getServerUrl().getResource()
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
