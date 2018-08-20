package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.KnownIdentifierSystemEnum;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.ProvenanceUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.PractitionerRole;
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

    private final ProvenanceUtil provenanceUtil;

    @Autowired
    public LocationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties, ProvenanceUtil provenanceUtil) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.provenanceUtil = provenanceUtil;
    }

    @Override
    public PageDto<LocationDto> getAllLocations(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {

        int numberOfLocationsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Location.name());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class);

        //Set Sort order
        locationsSearchQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(locationsSearchQuery, true);

        // Check if there are any additional search criteria
        locationsSearchQuery = addAdditionalLocationSearchConditions(locationsSearchQuery, statusList, searchKey, searchValue);

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = PaginationUtil.getSearchBundleFirstPage(locationsSearchQuery, numberOfLocationsPerPage, Optional.empty());

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().isEmpty()) {
            log.info("No locations were found in the FHIR server");
            return new PageDto<>(new ArrayList<>(), numberOfLocationsPerPage, 0, 0, 0, 0);
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");
        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            otherPageLocationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageLocationSearchBundle, pageNumber.get(), numberOfLocationsPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedLocations = otherPageLocationSearchBundle.getEntry();

        // Map to DTO
        List<LocationDto> locationsList = retrievedLocations.stream().map(this::convertLocationBundleEntryToLocationDto).collect(Collectors.toList());
        return (PageDto<LocationDto>) PaginationUtil.applyPaginationForSearchBundle(locationsList, otherPageLocationSearchBundle.getTotal(), numberOfLocationsPerPage, pageNumber);
    }

    @Override
    public PageDto<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<String> assignedToPractitioner, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfLocationsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Location.name());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId));

        //Set Sort order
        locationsSearchQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(locationsSearchQuery, true);

        // Check if there are any additional search criteria
        locationsSearchQuery = addAdditionalLocationSearchConditions(locationsSearchQuery, statusList, searchKey, searchValue);

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = PaginationUtil.getSearchBundleFirstPage(FhirOperationUtil.setNoCacheControlDirective(locationsSearchQuery), numberOfLocationsPerPage, Optional.empty());

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().isEmpty()) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            return new PageDto<>(new ArrayList<>(), numberOfLocationsPerPage, 0, 0, 0, 0);
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            otherPageLocationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, otherPageLocationSearchBundle, pageNumber.get(), numberOfLocationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedLocations = otherPageLocationSearchBundle.getEntry();

        // Map to DTO
        List<LocationDto> locationsList = retrievedLocations.stream().map(loc -> convertLocationBundleEntryToLocationDto(loc, organizationResourceId, assignedToPractitioner)).collect(Collectors.toList());
        return (PageDto<LocationDto>) PaginationUtil.applyPaginationForSearchBundle(locationsList, otherPageLocationSearchBundle.getTotal(), numberOfLocationsPerPage, pageNumber);
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
    public void createLocation(String organizationId, LocationDto locationDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();
        log.info("Creating location for Organization Id:" + organizationId);
        log.info("But first, checking if a duplicate location(active/inactive/suspended) exists based on the Identifiers provided.");

        checkForDuplicateLocationBasedOnOrganizationTaxId(organizationId, locationDto);
        checkForDuplicateLocationBasedOnIdentifiersDuringCreate(locationDto);

        getOrganizationIdentifier(organizationId).ifPresent(identifierDto -> locationDto.getIdentifiers().add(identifierDto));
        Location fhirLocation = modelMapper.map(locationDto, Location.class);
        fhirLocation.setStatus(getLocationStatusFromDto(locationDto));
        fhirLocation.setManagingOrganization(new Reference("Organization/" + organizationId.trim()));

        if (locationDto.getManagingLocationLogicalId() != null && !locationDto.getManagingLocationLogicalId().trim().isEmpty()) {
            fhirLocation.setPartOf(new Reference("Location/" + locationDto.getManagingLocationLogicalId().trim()));
        }
        //Set Profile Meta Data
        FhirProfileUtil.setLocationProfileMetaData(fhirClient, fhirLocation);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, fhirLocation, Optional.empty(), ResourceType.Location.name(), "Create Location");

        //Create
        MethodOutcome methodOutcome = FhirOperationUtil.createFhirResource(fhirClient, fhirLocation, ResourceType.Location.name());
        idList.add(ResourceType.Location.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

        if (fisProperties.isProvenanceEnabled()) {
            provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.CREATE, loggedInUser);
        }
    }

    @Override
    public void updateLocation(String organizationId, String locationId, LocationDto locationDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();

        log.info("Updating location Id: " + locationId + " for Organization Id:" + organizationId);
        log.info("But first, checking if a duplicate location(active/inactive/suspended) exists based on the Identifiers provided.");
        checkForDuplicateLocationBasedOnOrganizationTaxId(organizationId, locationDto);
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
        existingFhirLocation.setPhysicalType(updatedFhirLocation.hasPhysicalType() ? updatedFhirLocation.getPhysicalType() : null);
        existingFhirLocation.setManagingOrganization(new Reference("Organization/" + organizationId.trim()));

        if (locationDto.getManagingLocationLogicalId() != null && !locationDto.getManagingLocationLogicalId().trim().isEmpty()) {
            existingFhirLocation.setPartOf(new Reference("Location/" + locationDto.getManagingLocationLogicalId().trim()));
        } else {
            existingFhirLocation.setPartOf(null);
        }

        //Set Profile Meta Data
        FhirProfileUtil.setLocationProfileMetaData(fhirClient, existingFhirLocation);
        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, existingFhirLocation, Optional.of(locationId), ResourceType.Location.name(), "Update Location");

        //Update
        MethodOutcome methodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, existingFhirLocation, "Update Location");
        idList.add(ResourceType.Location.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

        if (fisProperties.isProvenanceEnabled()) {
            provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.UPDATE, loggedInUser);
        }
    }

    @Override
    public void inactivateLocation(String locationId) {
        log.info("Inactivating the location Id: " + locationId);
        Location existingFhirLocation = readLocationFromServer(locationId);
        existingFhirLocation.setStatus(Location.LocationStatus.INACTIVE);

        //Set Profile Meta Data
        FhirProfileUtil.setLocationProfileMetaData(fhirClient, existingFhirLocation);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, existingFhirLocation, Optional.of(locationId), ResourceType.Location.name(), "Inactivate Location");

        //Update the resource
        FhirOperationUtil.updateFhirResource(fhirClient, existingFhirLocation, "Inactivate Location");
    }

    private IQuery addAdditionalLocationSearchConditions(IQuery locationsSearchQuery, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue) {
        // Check for location status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            log.info("Searching for location with the following specific status(es)");
            statusList.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for locations with ALL statuses");
        }

        // Check for bad requests
        if (searchKey.isPresent() && !SearchKeyEnum.LocationSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check searchKey and searchValue
        if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.NAME.name())) {
            log.info("Searching for location " + SearchKeyEnum.LocationSearchKey.NAME.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new RichStringClientParam("name").contains().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.LOGICALID.name())) {
            log.info("Searching for location " + SearchKeyEnum.LocationSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for location " + SearchKeyEnum.LocationSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            locationsSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("Searching location : No additional search criteria entered.");
        }
        return locationsSearchQuery;
    }

    private Location readLocationFromServer(String locationId) {
        Location existingFhirLocation;

        try {
            existingFhirLocation = fhirClient.read().resource(Location.class).withId(locationId.trim()).execute();
        } catch (BaseServerResponseException e) {
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
        final String ORGANIZATION_TAX_ID_DISPLAY = KnownIdentifierSystemEnum.TAX_ID_ORGANIZATION.getDisplay();
        List<Bundle.BundleEntryComponent> bundle = getLocationBundleBasedOnIdentifierSystemAndIdentifierValue(identifierSystem, identifierValue);

        if (bundle != null && !bundle.isEmpty()) {
            List<Identifier> identifierList = bundle.stream().flatMap(loc -> {
                Location location = (Location) loc.getResource();
                return location.getIdentifier().stream();
            }).collect(Collectors.toList());
            identifierList.stream().filter(identifier -> !identifier.getSystem().equalsIgnoreCase(ORGANIZATION_TAX_ID_DISPLAY)).findAny().ifPresent(ids -> {
                        throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
                    }
            );
        }
    }

    private void checkForDuplicateLocationBasedOnOrganizationTaxId(String organizationId, LocationDto locationDto) {
        final String ORGANIZATION_TAX_ID_DISPLAY = KnownIdentifierSystemEnum.TAX_ID_ORGANIZATION.getDisplay();
        final String ORGANIZATION_TAX_ID_URI = KnownIdentifierSystemEnum.TAX_ID_ORGANIZATION.getUri();

        fhirClient.read().resource(Organization.class).withId(organizationId).execute().getIdentifier().stream()
                .filter(identifier -> identifier.getSystem().equalsIgnoreCase(ORGANIZATION_TAX_ID_URI)).findAny().ifPresent(identifier -> {
            locationDto.getIdentifiers().stream().filter(identifierDto -> identifierDto.getSystem().equalsIgnoreCase(ORGANIZATION_TAX_ID_DISPLAY))
                    .findAny().ifPresent(identifierDto -> {
                if (!identifierDto.getValue().replaceAll(" ", "")
                        .replaceAll("-", "").trim().equalsIgnoreCase(identifier.getValue().replaceAll(" ", "")
                                .replaceAll("-", "").trim())) {
                    throw new DuplicateResourceFoundException("The organization id is different from the original organization.");
                }
            });
        });
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
        final String ORGANIZATION_TAX_ID_DISPLAY = KnownIdentifierSystemEnum.TAX_ID_ORGANIZATION.getDisplay();
        List<Bundle.BundleEntryComponent> bundle = getLocationBundleBasedOnIdentifierSystemAndIdentifierValue(identifierSystem, identifierValue);

        if (bundle != null && bundle.size() > 1) {
            String iS = bundle.stream().map(loc -> {
                Location location = (Location) loc.getResource();
                return location.getIdentifier().stream().findFirst().get().getSystem();
            }).findFirst().get();

            if (!iS.equalsIgnoreCase(ORGANIZATION_TAX_ID_DISPLAY)) {
                throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
            }
        } else if (bundle != null && bundle.size() == 1) {
            String logicalId = bundle.get(0).getResource().getIdElement().getIdPart();
            if (!logicalId.trim().equalsIgnoreCase(locationId.trim())) {
                throw new DuplicateResourceFoundException("A Location already exists has the identifier system:" + identifierSystem + " and value: " + identifierValue);
            }
        }
    }

    private List<Bundle.BundleEntryComponent> getLocationBundleBasedOnIdentifierSystemAndIdentifierValue(String identifierSystem, String identifierValue) {
        List<Bundle.BundleEntryComponent> bundleEntry;
        if (identifierSystem != null && !identifierSystem.trim().isEmpty()
                && identifierValue != null && !identifierValue.trim().isEmpty()) {
            Bundle bundle = fhirClient.search().forResource(Location.class).returnBundle(Bundle.class).execute();
            bundleEntry = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties).stream().filter(location -> {
                Location l = (Location) location.getResource();
                return l.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(identifierSystem) && identifier.getValue().replaceAll(" ", "")
                        .replaceAll("-", "").trim()
                        .equalsIgnoreCase(identifierValue.replaceAll(" ", "").replaceAll("-", "").trim()));
            }).collect(Collectors.toList());
        } else if (identifierValue != null && !identifierValue.trim().isEmpty()) {
            Bundle bundle = fhirClient.search().forResource(Location.class)
                    .where(new TokenClientParam("identifier").exactly().code(identifierValue.trim()))
                    .returnBundle(Bundle.class)
                    .execute();
            bundleEntry = bundle.getEntry();
        } else {
            throw new BadRequestException("Found no valid identifierSystem and/or identifierValue");
        }
        return bundleEntry;
    }

    private LocationDto convertLocationBundleEntryToLocationDto(Bundle.BundleEntryComponent fhirLocationModel) {
        LocationDto tempLocationDto = modelMapper.map(fhirLocationModel.getResource(), LocationDto.class);
        tempLocationDto.setLogicalId(fhirLocationModel.getResource().getIdElement().getIdPart());
        return tempLocationDto;
    }

    private LocationDto convertLocationBundleEntryToLocationDto(Bundle.BundleEntryComponent fhirLocationModel,
                                                                String organizationId,
                                                                Optional<String> assignedToPractitioner) {
        LocationDto locationDto = convertLocationBundleEntryToLocationDto(fhirLocationModel);
        assignedToPractitioner.ifPresent(prac -> {
            locationDto.setAssignToCurrentPractitioner(Optional.ofNullable(isAssignedToPractitioner(prac, organizationId, locationDto.getLogicalId())));
        });

        return locationDto;
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
                    } catch (FHIRException fe) {
                        log.error("Could not convert Location Status");
                    }
                }
            }
        }
        return Location.LocationStatus.ACTIVE;
    }

    private Optional<IdentifierDto> getOrganizationIdentifier(String organizationId) {
        Organization organization = fhirClient.read().resource(Organization.class).withId(organizationId).execute();

        OrganizationDto organizationDto = modelMapper.map(organization, OrganizationDto.class);
        return organizationDto.getIdentifiers().stream()
                .filter(identifier -> identifier.getSystem().equalsIgnoreCase(KnownIdentifierSystemEnum.TAX_ID_ORGANIZATION.getUri()))
                .findFirst();
    }

    private Boolean isAssignedToPractitioner(String practitionerId, String organizationId, String logicalId) {
        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("practitioner").hasId(practitionerId))
                .where(new ReferenceClientParam("organization").hasId(organizationId))
                .where(new ReferenceClientParam("location").hasId(logicalId))
                .returnBundle(Bundle.class).execute();

        if (bundle.getEntry().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
}

