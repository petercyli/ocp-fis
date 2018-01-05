package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;

import java.util.List;

public interface LocationService {
    /**
     * Gets all available locations in the configured FHIR server
     *
     * @return
     */
    List<LocationDto> getAllLocations();

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     *
     * @param organizationResourceId
     * @return
     */
    List<LocationDto> getLocationsByOrganization(String organizationResourceId);

    /**
     * Get Location By Id
     *
     * @param locationId
     * @return
     */
    LocationDto getLocation(String locationId);

    /**
     * Gets level 1 child location for a given Location Id
     *
     * @param locationId
     * @return
     */
    LocationDto getChildLocation(String locationId);
}
