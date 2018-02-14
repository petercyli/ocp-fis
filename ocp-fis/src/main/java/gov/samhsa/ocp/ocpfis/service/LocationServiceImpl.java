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
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public LocationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<LocationDto> getAllLocations(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {

        int numberOfLocationsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Location.name());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;
        boolean firstPage = true;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class);

        //Check for location status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            log.info("Searching for locations with the following specific status(es).");
            statusList.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for locations with ALL statuses");
        }

       // Check if there are any additional search criteria
        locationsSearchQuery = addAdditionalLocationSearchConditions(locationsSearchQuery, searchKey, searchValue);

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = (Bundle) locationsSearchQuery.count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No locations were found in the FHIR server");
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");
        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageLocationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageLocationSearchBundle, pageNumber.get(), numberOfLocationsPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedLocations = otherPageLocationSearchBundle.getEntry();

        //Arrange Page related info
        List<LocationDto> locationsList = retrievedLocations.stream().map(this::convertLocationBundleEntryToLocationDto).collect(Collectors.toList());
        double totalPages = Math.ceil((double) otherPageLocationSearchBundle.getTotal() / numberOfLocationsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(locationsList, numberOfLocationsPerPage, totalPages, currentPage, locationsList.size(), otherPageLocationSearchBundle.getTotal());
    }

    @Override
    public PageDto<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfLocationsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Location.name());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;
        boolean firstPage = true;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId));

        //Check for location status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            log.info("Searching for location with the following specific status(es) for the given OrganizationID:" + organizationResourceId);
            statusList.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for locations with ALL statuses for the given OrganizationID:" + organizationResourceId);
        }

        // Check if there are any additional search criteria
        locationsSearchQuery = addAdditionalLocationSearchConditions(locationsSearchQuery, searchKey, searchValue);

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = (Bundle) locationsSearchQuery.count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().isEmpty()) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            return new PageDto<>(new ArrayList<>(), numberOfLocationsPerPage, 0, 0, 0, 0);
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageLocationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, otherPageLocationSearchBundle, pageNumber.get(), numberOfLocationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedLocations = otherPageLocationSearchBundle.getEntry();

        //Arrange Page related info
        List<LocationDto> locationsList = retrievedLocations.stream().map(this::convertLocationBundleEntryToLocationDto).collect(Collectors.toList());
        double totalPages = Math.ceil((double) otherPageLocationSearchBundle.getTotal() / numberOfLocationsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(locationsList, numberOfLocationsPerPage, totalPages, currentPage, locationsList.size(), otherPageLocationSearchBundle.getTotal());
    }

    @Override
    public LocationDto getLocation(String locationId) {
        log.info("Searching for Location Id:" + locationId);

        Bundle locationBundle = fhirClient.search().forResource(Location.class)
                .where(new TokenClientParam("_id").exactly().code(locationId))
                .returnBundle(Bundle.class)
                .execute();

        if (locationBundle == null || locationBundle.getEntry().isEmpty()) {
            log.info("No location was found for the given LocationID:" + locationId);
            throw new ResourceNotFoundException("No location was found for the given LocationID:" + locationId);
        }

        log.info("FHIR Location bundle retrieved from FHIR server successfully for location Id:" + locationId);

        Bundle.BundleEntryComponent retrievedLocation = locationBundle.getEntry().get(0);

        return convertLocationBundleEntryToLocationDto(retrievedLocation);
    }

    @Override
    public LocationDto getChildLocation(String locationId) {
        log.info("Searching Child Location for Location Id:" + locationId);
        Bundle childLocationBundle = getChildLocationBundleFromServer(locationId);

        if (childLocationBundle == null || childLocationBundle.getEntry().isEmpty()) {
            log.info("No child location found for the given LocationID:" + locationId);
            throw new ResourceNotFoundException("No child location found for the given LocationID:" + locationId);
        }

        log.info("FHIR Location(Child) bundle retrieved from FHIR server successfully for locationId: " + locationId);

        Bundle.BundleEntryComponent retrievedLocation = childLocationBundle.getEntry().get(0);

        return convertLocationBundleEntryToLocationDto(retrievedLocation);
    }

    @Override
    public void createLocation(String organizationId, LocationDto locationDto) {
        log.info("Creating location for Organization Id:" + organizationId);
        log.info("But first, checking if a duplicate location(active/inactive/suspended) exists based on the Identifiers provided.");
        checkForDuplicateLocationBasedOnIdentifiersDuringCreate(locationDto);

        Location fhirLocation = modelMapper.map(locationDto, Location.class);
        fhirLocation.setStatus(getLocationStatusFromDto(locationDto));
        fhirLocation.setPhysicalType(getLocationPhysicalTypeFromDto(locationDto));
        fhirLocation.setManagingOrganization(new Reference("Organization/" + organizationId.trim()));

        if (locationDto.getManagingLocationLogicalId() != null && !locationDto.getManagingLocationLogicalId().trim().isEmpty()) {
            fhirLocation.setPartOf(new Reference("Location/" + locationDto.getManagingLocationLogicalId().trim()));
        }

        // Validate the resource
        validateLocationResource(fhirLocation, Optional.empty(), "Create Location: ");

        try {
            MethodOutcome serverResponse = fhirClient.create().resource(fhirLocation).execute();
            log.info("Created a new location :" + serverResponse.getId().getIdPart() + " for Organization Id:" + organizationId);
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT create location for Organization Id:" + organizationId);
            throw new FHIRClientException("FHIR Client returned with an error while creating the location:" + e.getMessage());
        }

    }

    @Override
    public void updateLocation(String organizationId, String locationId, LocationDto locationDto) {
        log.info("Updating location Id: " + locationId + " for Organization Id:" + organizationId);
        log.info("But first, checking if a duplicate location(active/inactive/suspended) exists based on the Identifiers provided.");
        checkForDuplicateLocationBasedOnIdentifiersDuringUpdate(locationId, locationDto);

        //First, get the existing resource from the server
        Location existingFhirLocation = readLocationFromServer(locationId);

        Location updatedFhirLocation = modelMapper.map(locationDto, Location.class);
        //Overwrite values from the dto
        existingFhirLocation.setName(updatedFhirLocation.getName());
        existingFhirLocation.setIdentifier(updatedFhirLocation.getIdentifier());
        existingFhirLocation.setAddress(updatedFhirLocation.getAddress());
        existingFhirLocation.setTelecom(updatedFhirLocation.getTelecom());
        existingFhirLocation.setStatus(getLocationStatusFromDto(locationDto));
        existingFhirLocation.setPhysicalType(getLocationPhysicalTypeFromDto(locationDto));
        existingFhirLocation.setManagingOrganization(new Reference("Organization/" + organizationId.trim()));

        if (locationDto.getManagingLocationLogicalId() != null && !locationDto.getManagingLocationLogicalId().trim().isEmpty()) {
            existingFhirLocation.setPartOf(new Reference("Location/" + locationDto.getManagingLocationLogicalId().trim()));
        } else {
            existingFhirLocation.setPartOf(null);
        }

        // Validate the resource
        validateLocationResource(existingFhirLocation, Optional.of(locationId), "Update Location: ");

        //Update the resource
        updateLocationResource(existingFhirLocation, "Update Location");
    }

    @Override
    public void inactivateLocation(String locationId) {
        log.info("Inactivating the location Id: " + locationId);
        Location existingFhirLocation = readLocationFromServer(locationId);
        existingFhirLocation.setStatus(Location.LocationStatus.INACTIVE);

        //Update the resource
        updateLocationResource(existingFhirLocation, "Inactivate Location");
    }


    private  IQuery addAdditionalLocationSearchConditions(IQuery locationsSearchQuery, Optional<String> searchKey, Optional<String> searchValue){
        //Check for bad requests
        if (searchKey.isPresent() && !SearchKeyEnum.LocationSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.NAME.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.NAME.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }
        return locationsSearchQuery;
    }

    private void validateLocationResource(Location fhirLocation, Optional<String> locationId, String createOrUpdateLocation) {
        ValidationResult validationResult = fhirValidator.validateWithResult(fhirLocation);

        if (locationId.isPresent()) {
            log.info(createOrUpdateLocation + "Validation successful? " + validationResult.isSuccessful() + " for LocationID: " + locationId);
        } else {
            log.info(createOrUpdateLocation + "Validation successful? " + validationResult.isSuccessful());
        }

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("Location Validation was not successful" + validationResult.getMessages());
        }

    }

    private void updateLocationResource(Location fhirLocation, String logMessage){
        try {
            MethodOutcome serverResponse = fhirClient.update().resource(fhirLocation).execute();
            log.info(logMessage + " was successful for Location Id: " + serverResponse.getId().getIdPart());
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT " + logMessage + " for Location Id: " + fhirLocation.getIdElement().getIdPart());
            throw new FHIRClientException("FHIR Client returned with an error during" + logMessage + " : " + e.getMessage());
        }
    }


    private Location readLocationFromServer(String locationId) {
        Location existingFhirLocation;

        try {
            existingFhirLocation = fhirClient.read().resource(Location.class).withId(locationId.trim()).execute();
        }
        catch (BaseServerResponseException e) {
            log.error("FHIR Client returned with an error while reading the location with ID: " + locationId);
            throw new ResourceNotFoundException("FHIR Client returned with an error while reading the location:" + e.getMessage());
        }
        return existingFhirLocation;
    }

    private Bundle getChildLocationBundleFromServer(String locationId) {
        return fhirClient.search().forResource(Location.class)
                .where(new ReferenceClientParam("partof").hasId(locationId))
                .returnBundle(Bundle.class)
                .execute();
    }

    private void checkForDuplicateLocationBasedOnIdentifiersDuringCreate(LocationDto locationDto) {
        List<IdentifierDto> identifiersList = locationDto.getIdentifiers();
        log.info("Create Location: Current locationDto has " + identifiersList.size() + " identifiers.");

        for (IdentifierDto tempIdentifierDto : identifiersList) {
            String identifierSystem = tempIdentifierDto.getSystem();
            String identifierValue = tempIdentifierDto.getValue();
            checkDuplicateLocationExistsDuringCreate(identifierSystem, identifierValue);
        }
        log.info("Create Location: Found no duplicate location.");
    }

    private void checkDuplicateLocationExistsDuringCreate(String identifierSystem, String identifierValue) {
        Bundle bundle = getLocationBundleBasedOnIdentifierSystemAndIdentifierValue(identifierSystem, identifierValue);

        if (bundle != null && !bundle.getEntry().isEmpty()) {
            throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
        }
    }

    private void checkForDuplicateLocationBasedOnIdentifiersDuringUpdate(String locationId, LocationDto locationDto) {
        List<IdentifierDto> identifiersList = locationDto.getIdentifiers();
        log.info("Update Location: Current locationDto has " + identifiersList.size() + " identifiers.");

        for (IdentifierDto tempIdentifierDto : identifiersList) {
            String identifierSystem = tempIdentifierDto.getSystem();
            String identifierValue = tempIdentifierDto.getValue();
            checkDuplicateLocationExistsDuringUpdate(locationId, identifierSystem, identifierValue);
        }
        log.info("Update Location: Found no duplicate location.");
    }

    private void checkDuplicateLocationExistsDuringUpdate(String locationId, String identifierSystem, String identifierValue) {
        Bundle bundle = getLocationBundleBasedOnIdentifierSystemAndIdentifierValue(identifierSystem, identifierValue);

        if (bundle != null && bundle.getEntry().size() > 1) {
            throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
        } else if (bundle != null && bundle.getEntry().size() == 1) {
            String logicalId = bundle.getEntry().get(0).getResource().getIdElement().getIdPart();
            if (!logicalId.trim().equalsIgnoreCase(locationId.trim())) {
                throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
            }
        }
    }

    private Bundle getLocationBundleBasedOnIdentifierSystemAndIdentifierValue(String identifierSystem, String identifierValue) {
        Bundle bundle;
        if (identifierSystem != null && !identifierSystem.trim().isEmpty()
                && identifierValue != null && !identifierValue.trim().isEmpty()) {
            bundle = fhirClient.search().forResource(Location.class)
                    .where(new TokenClientParam("identifier").exactly().systemAndCode(identifierSystem.trim(), identifierValue.trim()))
                    .returnBundle(Bundle.class)
                    .execute();
        } else if (identifierValue != null && !identifierValue.trim().isEmpty()) {
            bundle = fhirClient.search().forResource(Location.class)
                    .where(new TokenClientParam("identifier").exactly().code(identifierValue.trim()))
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            throw new BadRequestException("Found no valid identifierSystem and/or identifierValue");
        }
        return bundle;
    }

    private LocationDto convertLocationBundleEntryToLocationDto(Bundle.BundleEntryComponent fhirLocationModel) {
        LocationDto tempLocationDto = modelMapper.map(fhirLocationModel.getResource(), LocationDto.class);
        tempLocationDto.setLogicalId(fhirLocationModel.getResource().getIdElement().getIdPart());
        Location loc = (Location) fhirLocationModel.getResource();
        if (loc.hasPhysicalType()) {
            tempLocationDto.setPhysicalType(loc.getPhysicalType().getCoding().get(0).getDisplay());
        }
        return tempLocationDto;
    }

    private Location.LocationStatus getLocationStatusFromDto(LocationDto locationDto) {
        List<ValueSetDto> validLocationStatuses = lookUpService.getLocationStatuses();

        if (locationDto == null) {
            log.info("Can't read status of the location - LocationDto is NULL!. Setting Location as ACTIVE.");
            return Location.LocationStatus.ACTIVE;
        } else if (locationDto.getStatus() == null || locationDto.getStatus().isEmpty()) {
            return Location.LocationStatus.ACTIVE;
        } else {
            for (ValueSetDto validLocationStatus : validLocationStatuses) {
                if (validLocationStatus.getDisplay().equalsIgnoreCase(locationDto.getStatus())) {
                    try {
                        return Location.LocationStatus.fromCode(locationDto.getStatus());
                    }
                    catch (FHIRException fe) {
                        log.error("Could not convert Location Status");
                    }
                }
            }
        }
        return Location.LocationStatus.ACTIVE;
    }

    private CodeableConcept getLocationPhysicalTypeFromDto(LocationDto locationDto) {
        String physicalType = locationDto.getPhysicalType();
        List<ValueSetDto> availablePhysicalTypes = lookUpService.getLocationPhysicalTypes();
        Coding coding = new Coding();

        if (physicalType != null && !physicalType.trim().isEmpty()) {
            availablePhysicalTypes.stream().filter(physicalTypeDto -> physicalTypeDto.getDisplay().equalsIgnoreCase(physicalType.trim())).forEach(physicalTypeDto -> {
                coding.setCode(physicalTypeDto.getCode());
                coding.setSystem(physicalTypeDto.getSystem());
                coding.setDisplay(physicalTypeDto.getDisplay());
            });
            return new CodeableConcept().addCoding(coding);
        }
        log.warn("Location physical type is empty or NULL.");
        return null;
    }


}
