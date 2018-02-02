package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Reference;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthCareServiceServiceImpl implements HealthCareServiceService {

    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;

    @Autowired
    public HealthCareServiceServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FisProperties fisProperties, FhirValidator fhirValidator) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fisProperties = fisProperties;
        this.fhirValidator = fhirValidator;
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


    @Override
    public void assignLocationToHealthCareService(String healthCareServiceId, String organizationResourceId, List<String> locationIdList) {
        boolean allChecksPassed = false;

        //First, validate if the given location(s) belong to the given organization Id
        Bundle locationSearchBundle = getLocationBundle(organizationResourceId);

        if (locationSearchBundle == null || locationSearchBundle.getEntry().size() < 1) {
            log.info("Assign location to a HealthCareService: No location found for the given organization ID:" + organizationResourceId);
            throw new ResourceNotFoundException("Cannot assign the given location(s) to healthCareService, because we did not find any location(s) under the organization ID: " + organizationResourceId);
        }

        List<String> retrievedLocationsList = locationSearchBundle.getEntry().stream().map(fhirLocationModel -> fhirLocationModel.getResource().getIdElement().getIdPart()).collect(Collectors.toList());

        if (retrievedLocationsList.containsAll(locationIdList)) {
            log.info("Assign location to a HealthCareService: Successful Check 1: The given location(s) belong to the given organization ID: " + organizationResourceId);

            HealthcareService existingHealthCareService = readHealthCareServiceFromServer(healthCareServiceId);
            List<Reference> assignedLocations = existingHealthCareService.getLocation();

            //Next, avoid adding redundant location data
            Set<String> existingAssignedLocations = assignedLocations.stream().map(locReference -> locReference.getReference().substring(9)).collect(Collectors.toSet());
            locationIdList.removeIf(existingAssignedLocations::contains);

            if (locationIdList.size() == 0) {
                log.info("Assign location to a HealthCareService: All location(s) from the query params have already been assigned to belonged to health care Service ID:" + healthCareServiceId + ". Nothing to do!");
            } else {
                log.info("Assign location to a HealthCareService: Successful Check 2: Found some location(s) from the query params that CAN be assigned to belonged to health care Service ID:" + healthCareServiceId);
                allChecksPassed = true;
            }

            if (allChecksPassed) {
                locationIdList.forEach(locationId -> assignedLocations.add(new Reference("Location/" + locationId)));

                // Validate the resource
                validateHealthCareServiceResource(existingHealthCareService, Optional.of(healthCareServiceId), "Assign location to a HealthCareService: ");

                //Update
                try {
                    MethodOutcome serverResponse = fhirClient.update().resource(existingHealthCareService).execute();
                    log.info("Successfully assigned location(s) to HealthCareService ID :" + serverResponse.getId().getIdPart());
                }
                catch (BaseServerResponseException e) {
                    log.error("Assign location to a HealthCareService: Could NOT update location for HealthCareService ID:" + healthCareServiceId);
                    throw new FHIRClientException("FHIR Client returned with an error while updating the location:" + e.getMessage());
                }
            }
        } else {
            throw new BadRequestException("Cannot assign the given location(s) to healthCareService, because not all location(s) from the query params belonged to the organization ID: " + organizationResourceId);
        }
    }

    private HealthcareService readHealthCareServiceFromServer(String healthCareServiceId) {
        HealthcareService existingHealthCareService;

        try {
            existingHealthCareService = fhirClient.read().resource(HealthcareService.class).withId(healthCareServiceId.trim()).execute();
        }
        catch (BaseServerResponseException e) {
            log.error("FHIR Client returned with an error while reading the HealthCareService with ID: " + healthCareServiceId);
            throw new ResourceNotFoundException("FHIR Client returned with an error while reading the HealthCareService: " + e.getMessage());
        }
        return existingHealthCareService;
    }

    private void validateHealthCareServiceResource(HealthcareService healthCareService, Optional<String> healthCareServiceId, String functionName) {
        ValidationResult validationResult = fhirValidator.validateWithResult(healthCareService);

        if (healthCareServiceId.isPresent()) {
            log.info(functionName + "Validation successful? " + validationResult.isSuccessful() + " for health care Service ID: " + healthCareServiceId);
        } else {
            log.info(functionName + "Validation successful? " + validationResult.isSuccessful());
        }

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("HealthCareService validation was not successful" + validationResult.getMessages());
        }
    }

    private Bundle getLocationBundle(String organizationResourceId) {
        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId.trim()));

        return (Bundle) locationsSearchQuery.count(1000)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
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
