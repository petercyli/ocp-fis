package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthCareServiceServiceImpl implements HealthCareServiceService {

    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;
    private final LookUpService lookUpService;

    @Autowired
    public HealthCareServiceServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FisProperties fisProperties, FhirValidator fhirValidator,LookUpService lookUpService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fisProperties = fisProperties;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
    }

    @Override
    public PageDto<HealthCareServiceDto> getAllHealthCareServices(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfHeathcareServicesPerPage = pageSize.filter(s -> s > 0 &&
                s <= fisProperties.getHealthCareService().getPagination().getMaxSize()).orElse(fisProperties.getHealthCareService().getPagination().getDefaultSize());

        Bundle firstPageHeathcareServiceSearchBundle;
        Bundle otherPageHealthcareServiceSearchBundle;
        boolean firstPage = true;

        IQuery heathcareServicesSearchQuery = fhirClient.search().forResource(HealthcareService.class);

        //Check for HeathcareService status
        if (statusList.isPresent() && statusList.get().size() > 0) {
            log.info("Searching for ALL Heathcare Services with the following specific status(es).");
            statusList.get().forEach(log::info);
            heathcareServicesSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for Heathcare Services with ALL statuses");
        }

        //Check for bad requests
        if (searchKey.isPresent() && !SearchKeyEnum.HealthcareServiceSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check if there are any additional search criteria
        if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.NAME.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.NAME.name() + " = " + searchValue.get().trim());
            heathcareServicesSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            heathcareServicesSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            heathcareServicesSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }

        //The following bundle only contains Page 1 of the resultSet
        firstPageHeathcareServiceSearchBundle = (Bundle) heathcareServicesSearchQuery.count(numberOfHeathcareServicesPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageHeathcareServiceSearchBundle == null || firstPageHeathcareServiceSearchBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No locations were found in the FHIR server");
        }

        log.info("FHIR Healthcare Service(s) bundle retrieved " + firstPageHeathcareServiceSearchBundle.getTotal() + " Healthcare Service(s) from FHIR server successfully");
        otherPageHealthcareServiceSearchBundle = firstPageHeathcareServiceSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageHealthcareServiceSearchBundle = getHealthcareServiceSearchBundleAfterFirstPage(firstPageHeathcareServiceSearchBundle, pageNumber.get(), numberOfHeathcareServicesPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedHealthcareServices = otherPageHealthcareServiceSearchBundle.getEntry();

        //Arrange Page related info
        List<HealthCareServiceDto> healthcareServicesList = retrievedHealthcareServices.stream().map(this::convertHealthcareServiceBundleEntryToHealthcareServiceDto).collect(Collectors.toList());
        double totalPages = Math.ceil((double) otherPageHealthcareServiceSearchBundle.getTotal() / numberOfHeathcareServicesPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(healthcareServicesList, numberOfHeathcareServicesPerPage, totalPages, currentPage, healthcareServicesList.size(), otherPageHealthcareServiceSearchBundle.getTotal());
    }

    @Override
    public PageDto<HealthCareServiceDto> getAllHealthCareServicesByOrganization(String organizationResourceId, Optional<String> locationResourceId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size) {
        return null;

        //Note to Ming: If locationResourceId.isPresent(), then set appropriate value to HealthCareServiceDto.assignedToCurrentLocation
    }

    private HealthCareServiceDto convertHealthcareServiceBundleEntryToHealthcareServiceDto(Bundle.BundleEntryComponent fhirHealthcareServiceModel) {
        HealthCareServiceDto tempHealthCareServiceDto = modelMapper.map(fhirHealthcareServiceModel.getResource(), HealthCareServiceDto.class);
        tempHealthCareServiceDto.setLogicalId(fhirHealthcareServiceModel.getResource().getIdElement().getIdPart());
        HealthcareService loc = (HealthcareService) fhirHealthcareServiceModel.getResource();
        //if (loc.hasPhysicalType()) {
         //   tempLocationDto.setPhysicalType(loc.getPhysicalType().getCoding().get(0).getDisplay());
       // }
        return tempHealthCareServiceDto;
    }

    private Bundle getHealthcareServiceSearchBundleAfterFirstPage(Bundle healthcareServiceSearchBundle, int pageNumber, int pageSize) {
        if (healthcareServiceSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((pageNumber >= 1 ? pageNumber : 1) - 1) * pageSize;

            if (offset >= healthcareServiceSearchBundle.getTotal()) {
                throw new ResourceNotFoundException("No healthcare services were found in the FHIR server for the page number: " + pageNumber);
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + healthcareServiceSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + pageSize
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            throw new ResourceNotFoundException("No healthcare services were found in the FHIR server for the page number: " + pageNumber);
        }
    }


}
