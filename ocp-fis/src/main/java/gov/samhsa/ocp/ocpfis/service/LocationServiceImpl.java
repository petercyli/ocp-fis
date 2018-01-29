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
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    private final LookUpService lookUpService;

    @Autowired
    public LocationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService = lookUpService;
    }

    @Override
    public PageDto<LocationDto> getAllLocations(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {

        int numberOfLocationsPerPage = pageSize.filter(s -> s > 0 &&
                s <= fisProperties.getLocation().getPagination().getMaxSize()).orElse(fisProperties.getLocation().getPagination().getDefaultSize());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;
        boolean firstPage = true;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class);

        //Check for location status
        if (statusList.isPresent() && statusList.get().size() > 0) {
            log.info("Searching for ALL locations with the following specific status(es).");
            statusList.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for locations with ALL statuses");
        }

        //Check for bad requests
        if (searchKey.isPresent() && !SearchKeyEnum.LocationSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check if there are any additional search criteria
        if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.NAME.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.NAME.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = (Bundle) locationsSearchQuery.count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No locations were found in the FHIR server");
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");
        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageLocationSearchBundle = getLocationSearchBundleAfterFirstPage(firstPageLocationSearchBundle, pageNumber.get(), numberOfLocationsPerPage);
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
        int numberOfLocationsPerPage = pageSize.filter(s -> s > 0 &&
                s <= fisProperties.getLocation().getPagination().getMaxSize()).orElse(fisProperties.getLocation().getPagination().getDefaultSize());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;
        boolean firstPage = true;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId));

        //Check for location status
        if (statusList.isPresent() && statusList.get().size() > 0) {
            log.info("Searching for location with the following specific status(es) for the given OrganizationID:" + organizationResourceId);
            statusList.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for locations with ALL statuses for the given OrganizationID:" + organizationResourceId);
        }

        //Check for bad requests
        if (searchKey.isPresent() && !SearchKeyEnum.LocationSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check if there are any additional search criteria
        if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.NAME.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.NAME.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchValue.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = (Bundle) locationsSearchQuery.count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().size() < 1) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            throw new ResourceNotFoundException("No location found for the given OrganizationID:" + organizationResourceId);
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageLocationSearchBundle = getLocationSearchBundleAfterFirstPage(otherPageLocationSearchBundle, pageNumber.get(), numberOfLocationsPerPage);
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

        if (locationBundle == null || locationBundle.getEntry().size() < 1) {
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

        if (childLocationBundle == null || childLocationBundle.getEntry().size() < 1) {
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
        ValidationResult validationResult = fhirValidator.validateWithResult(fhirLocation);
        log.info("Create Location: Validation successful? " + validationResult.isSuccessful());

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("Location Validation was not successful" + validationResult.getMessages());
        }

        try {
            MethodOutcome serverResponse = fhirClient.create().resource(fhirLocation).execute();
            log.info("Created a new location :" + serverResponse.getId().getIdPart() + " for Organization Id:" + organizationId);
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT create location for Organization Id:" + organizationId);
            throw new FHIRClientException("FHIR Client returned with an error while creating the location:" + e.getMessage());
        }

    }

    /**
     * @param organizationId
     * @param locationId
     * @param locationDto
     */
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
        ValidationResult validationResult = fhirValidator.validateWithResult(existingFhirLocation);
        log.info("Update Location: Validation successful? " + validationResult.isSuccessful() + " for LocationID:" + locationId);

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("Location Validation was not successful" + validationResult.getMessages());
        }

        try {
            MethodOutcome serverResponse = fhirClient.update().resource(existingFhirLocation).execute();
            log.info("Updated the location :" + serverResponse.getId().getIdPart() + " for Organization Id:" + organizationId);
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT update location for Organization Id:" + organizationId);
            throw new FHIRClientException("FHIR Client returned with an error while updating the location:" + e.getMessage());
        }

    }

    @Override
    public void inactivateLocation(String locationId) {
        log.info("Inactivating the location Id: " + locationId);
        Location existingFhirLocation = readLocationFromServer(locationId);
        setLocationStatusToInactive(existingFhirLocation);
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

    private void setLocationStatusToInactive(Location existingFhirLocation) {
        existingFhirLocation.setStatus(Location.LocationStatus.INACTIVE);
        try {
            MethodOutcome serverResponse = fhirClient.update().resource(existingFhirLocation).execute();
            log.info("Inactivated the location :" + serverResponse.getId().getIdPart());
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT inactivate location");
            throw new FHIRClientException("FHIR Client returned with an error while inactivating the location:" + e.getMessage());
        }
    }

    private Bundle getChildLocationBundleFromServer(String locationId) {
        return fhirClient.search().forResource(Location.class)
                .where(new ReferenceClientParam("partof").hasId(locationId))
                .returnBundle(Bundle.class)
                .execute();
    }

    private Bundle getLocationSearchBundleAfterFirstPage(Bundle locationSearchBundle, int pageNumber, int pageSize) {
        if (locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((pageNumber >= 1 ? pageNumber : 1) - 1) * pageSize;

            if (offset >= locationSearchBundle.getTotal()) {
                throw new ResourceNotFoundException("No locations were found in the FHIR server for the page number: " + pageNumber);
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + locationSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + pageSize
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            throw new ResourceNotFoundException("No locations were found in the FHIR server for the page number: " + pageNumber);
        }
    }

    private void checkForDuplicateLocationBasedOnIdentifiersDuringCreate(LocationDto locationDto) {
        List<IdentifierDto> identifiersList = locationDto.getIdentifiers();
        log.info("Current locationDto has " + identifiersList.size() + " identifiers.");

        for (IdentifierDto tempIdentifierDto : identifiersList) {
            String identifierSystem = tempIdentifierDto.getSystem();
            String identifierValue = tempIdentifierDto.getValue();
            checkDuplicateLocationExists(identifierSystem, identifierValue);
        }
        log.info("Create Location: Found no duplicate location.");
    }

    private void checkDuplicateLocationExists(String identifierSystem, String identifierValue) {
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

        if (bundle != null && bundle.getEntry().size() > 0) {
            throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
        }
    }

    private void checkForDuplicateLocationBasedOnIdentifiersDuringUpdate(String locationId, LocationDto locationDto) {
        List<IdentifierDto> identifiersList = locationDto.getIdentifiers();
        log.info("Current locationDto has " + identifiersList.size() + " identifiers.");

        for (IdentifierDto tempIdentifierDto : identifiersList) {
            String identifierSystem = tempIdentifierDto.getSystem();
            String identifierValue = tempIdentifierDto.getValue();
            checkDuplicateLocationExistsDuringUpdate(locationId, identifierSystem, identifierValue);
        }
        log.info("Update Location: Found no duplicate location.");
    }

    private void checkDuplicateLocationExistsDuringUpdate(String locationId, String identifierSystem, String identifierValue) {
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

        if (bundle != null && bundle.getEntry().size() > 1) {
            throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
        } else if (bundle != null && bundle.getEntry().size() == 1) {
            LocationDto temp = convertLocationBundleEntryToLocationDto(bundle.getEntry().get(0));

            if (!temp.getLogicalId().trim().equalsIgnoreCase(locationId.trim())) {
                throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
            }
        }
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
