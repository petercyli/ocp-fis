package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.exception.LocationNotFound;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Location;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    @Autowired
    public LocationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
    }

    /**
     * Gets all available locations in the configured FHIR server
     *
     * @return
     */
    @Override
    public List<LocationDto> getAllLocations() {
        Bundle allLocationsSearchBundle = fhirClient.search().forResource(Location.class)
                .returnBundle(Bundle.class)
                .execute();

        if (allLocationsSearchBundle == null || allLocationsSearchBundle.getEntry().size() < 1) {
            throw new LocationNotFound("No locations were found in the FHIR server");
        }
        log.info("FHIR Location(s) bundle retrieved from FHIR server successfully");
        List<Bundle.BundleEntryComponent> retrievedLocations = allLocationsSearchBundle.getEntry();

        List<LocationDto> temp = retrievedLocations.stream().map(location -> modelMapper.map(location.getResource(), LocationDto.class)).collect(Collectors.toList());

        log.info(retrievedLocations.toString());
        return temp;
        // return retrievedLocations.stream().map(location -> modelMapper.map(location, LocationDto.class)).collect(Collectors.toList());
    }

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     *
     * @param organizationResourceId
     * @return
     */
    @Override
    public List<LocationDto> getLocationsByOrganization(String organizationResourceId) {
        Bundle locationSearchBundle = fhirClient.search().forResource(Location.class)
                .where(new ReferenceClientParam("organization").hasId(organizationResourceId))
                .returnBundle(Bundle.class)
                .execute();

        if (locationSearchBundle == null || locationSearchBundle.getEntry().size() < 1) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            throw new LocationNotFound("No location found for the given OrganizationID:" + organizationResourceId);
        }

        log.info("FHIR Location(s) bundle retrieved from FHIR server successfully");

        List<Bundle.BundleEntryComponent> retrievedLocations = locationSearchBundle.getEntry();

        return retrievedLocations.stream().map(location -> modelMapper.map(location.getResource(), LocationDto.class)).collect(Collectors.toList());
    }

    /**
     * Get Location By Id
     *
     * @param locationId
     * @return
     */
    @Override
    public LocationDto getLocation(String locationId) {
        Bundle locationBundle = fhirClient.search().forResource(Location.class)
                .where(new TokenClientParam("_id").exactly().code(locationId))
                .returnBundle(Bundle.class)
                .execute();

        if (locationBundle == null || locationBundle.getEntry().size() < 1) {
            log.info("No location was found for the given LocationID:" + locationId);
            throw new LocationNotFound("No location was found for the given LocationID:" + locationId);
        }

        log.info("FHIR Location bundle retrieved from FHIR server successfully");

        Bundle.BundleEntryComponent retrievedLocation = locationBundle.getEntry().get(0);

        return modelMapper.map(retrievedLocation.getResource(), LocationDto.class);
    }

    /**
     * Gets level 1 child location for a given Location Id
     *
     * @param locationId
     * @return
     */
    @Override
    public LocationDto getChildLocation(String locationId) {
        Bundle childLocationBundle = fhirClient.search().forResource(Location.class)
                .where(new ReferenceClientParam("partof").hasId(locationId))
                .returnBundle(Bundle.class)
                .execute();

        if (childLocationBundle == null || childLocationBundle.getEntry().size() < 1) {
            log.info("No child location found for the given LocationID:" + locationId);
            throw new LocationNotFound("No child location found for the given LocationID:" + locationId);
        }

        log.info("FHIR Location bundle retrieved from FHIR server successfully");

        Bundle.BundleEntryComponent retrievedLocation = childLocationBundle.getEntry().get(0);

        return modelMapper.map(retrievedLocation.getResource(), LocationDto.class);
    }


}
