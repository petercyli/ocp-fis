package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
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

import java.util.ArrayList;
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
    public HealthCareServiceServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FisProperties fisProperties, FhirValidator fhirValidator, LookUpService lookUpService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fisProperties = fisProperties;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
    }

    @Override
    public PageDto<HealthCareServiceDto> getAllHealthCareServices(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfHealthCareServicesPerPage = pageSize.filter(s -> s > 0 &&
                s <= fisProperties.getHealthCareService().getPagination().getMaxSize()).orElse(fisProperties.getHealthCareService().getPagination().getDefaultSize());

        Bundle firstPageHealthCareServiceSearchBundle;
        Bundle otherPageHealthCareServiceSearchBundle;
        boolean firstPage = true;

        IQuery healthCareServicesSearchQuery = fhirClient.search().forResource(HealthcareService.class);

        //Check for HealthCareService status
        if (statusList.isPresent() && statusList.get().size() > 0) {
            log.info("Searching for ALL HealthCare Services with the following specific status(es).");
            statusList.get().forEach(log::info);
            healthCareServicesSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for HealthCare Services with ALL statuses");
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
            healthCareServicesSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            healthCareServicesSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            healthCareServicesSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }

        //The following bundle only contains Page 1 of the resultSet
        firstPageHealthCareServiceSearchBundle = (Bundle) healthCareServicesSearchQuery.count(numberOfHealthCareServicesPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageHealthCareServiceSearchBundle == null || firstPageHealthCareServiceSearchBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No HealthCare Services were found in the FHIR server");
        }

        log.info("FHIR HealthCare Service(s) bundle retrieved " + firstPageHealthCareServiceSearchBundle.getTotal() + " Healthcare Service(s) from FHIR server successfully");
        otherPageHealthCareServiceSearchBundle = firstPageHealthCareServiceSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageHealthCareServiceSearchBundle = getHealthcareServiceSearchBundleAfterFirstPage(firstPageHealthCareServiceSearchBundle, pageNumber.get(), numberOfHealthCareServicesPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedHealthCareServices = otherPageHealthCareServiceSearchBundle.getEntry();

        //Arrange Page related info
        List<HealthCareServiceDto> healthCareServicesList = retrievedHealthCareServices.stream().map(this::convertHealthCareServiceBundleEntryToHealthCareServiceDto).collect(Collectors.toList());
        double totalPages = Math.ceil((double) otherPageHealthCareServiceSearchBundle.getTotal() / numberOfHealthCareServicesPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(healthCareServicesList, numberOfHealthCareServicesPerPage, totalPages, currentPage, healthCareServicesList.size(), otherPageHealthCareServiceSearchBundle.getTotal());
    }

    @Override
    public PageDto<HealthCareServiceDto> getAllHealthCareServicesByOrganization(String organizationResourceId, Optional<String> locationResourceId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        //Note to Ming: If locationResourceId.isPresent(), then set appropriate value to HealthCareServiceDto.assignedToCurrentLocation

        int numberOfHealthCareServicesPerPage = pageSize.filter(s -> s > 0 &&
                s <= fisProperties.getHealthCareService().getPagination().getMaxSize()).orElse(fisProperties.getHealthCareService().getPagination().getDefaultSize());

        Bundle firstPageHealthCareServiceSearchBundle;
        Bundle otherPageHealthCareServiceSearchBundle;
        boolean firstPage = true;

        IQuery healthCareServicesSearchQuery = fhirClient.search().forResource(HealthcareService.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId));

        //Check for healthcare service status
        if (statusList.isPresent() && statusList.get().size() > 0) {
            log.info("Searching for health care service with the following specific status(es) for the given OrganizationID:" + organizationResourceId);
            statusList.get().forEach(log::info);
            healthCareServicesSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for health care services with ALL statuses for the given OrganizationID:" + organizationResourceId);
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
            healthCareServicesSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            healthCareServicesSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            healthCareServicesSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }

        //The following bundle only contains Page 1 of the resultSet
        firstPageHealthCareServiceSearchBundle = (Bundle) healthCareServicesSearchQuery.count(numberOfHealthCareServicesPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageHealthCareServiceSearchBundle == null || firstPageHealthCareServiceSearchBundle.getEntry().size() < 1) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            return new PageDto<>(new ArrayList<>(), numberOfHealthCareServicesPerPage, 0, 0, 0, 0);
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageHealthCareServiceSearchBundle.getTotal() + " healthcare service(s) from FHIR server successfully");

        otherPageHealthCareServiceSearchBundle = firstPageHealthCareServiceSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageHealthCareServiceSearchBundle = getHealthcareServiceSearchBundleAfterFirstPage(otherPageHealthCareServiceSearchBundle, pageNumber.get(), numberOfHealthCareServicesPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedHealthCareServices = otherPageHealthCareServiceSearchBundle.getEntry();

        //Arrange Page related info
        List<HealthCareServiceDto> healthCareServicesList = retrievedHealthCareServices.stream().map(this::convertHealthCareServiceBundleEntryToHealthCareServiceDto).collect(Collectors.toList());
        double totalPages = Math.ceil((double) otherPageHealthCareServiceSearchBundle.getTotal() / numberOfHealthCareServicesPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(healthCareServicesList, numberOfHealthCareServicesPerPage, totalPages, currentPage, healthCareServicesList.size(), otherPageHealthCareServiceSearchBundle.getTotal());
    }

    @Override
    public HealthCareServiceDto getHealthCareService(String healthCareServiceId) {
        log.info("Searching for Health Care Service Id:" + healthCareServiceId);

        Bundle healthCareServiceBundle = fhirClient.search().forResource(HealthcareService.class)
                .where(new TokenClientParam("_id").exactly().code(healthCareServiceId))
                .returnBundle(Bundle.class)
                .execute();

        if (healthCareServiceBundle == null || healthCareServiceBundle.getEntry().size() < 1) {
            log.info("No health care service was found for the given Health Care Service ID:" + healthCareServiceId);
            throw new ResourceNotFoundException("No health care service was found for the given Health Care Service ID:" + healthCareServiceId);
        }

        log.info("FHIR Health Care Service bundle retrieved from FHIR server successfully for Health Care Service Id:" + healthCareServiceId);

        Bundle.BundleEntryComponent retrievedHealthCareService = healthCareServiceBundle.getEntry().get(0);

        return convertHealthCareServiceBundleEntryToHealthCareServiceDto(retrievedHealthCareService);
    }


    private HealthCareServiceDto convertHealthCareServiceBundleEntryToHealthCareServiceDto(Bundle.BundleEntryComponent fhirHealthcareServiceModel) {
        HealthCareServiceDto tempHealthCareServiceDto = modelMapper.map(fhirHealthcareServiceModel.getResource(), HealthCareServiceDto.class);
        tempHealthCareServiceDto.setLogicalId(fhirHealthcareServiceModel.getResource().getIdElement().getIdPart());
        HealthcareService loc = (HealthcareService) fhirHealthcareServiceModel.getResource();
        //if (loc.hasPhysicalType()) {
        //   tempLocationDto.setPhysicalType(loc.getPhysicalType().getCoding().get(0).getDisplay());
        // }
        return tempHealthCareServiceDto;
    }

    private Bundle getHealthcareServiceSearchBundleAfterFirstPage(Bundle healthCareServiceSearchBundle, int pageNumber, int pageSize) {
        if (healthCareServiceSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((pageNumber >= 1 ? pageNumber : 1) - 1) * pageSize;

            if (offset >= healthCareServiceSearchBundle.getTotal()) {
                throw new ResourceNotFoundException("No HealthCare Services were found in the FHIR server for the page number: " + pageNumber);
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + healthCareServiceSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + pageSize
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            throw new ResourceNotFoundException("No HealthCare services were found in the FHIR server for the page number: " + pageNumber);
        }
    }


}
