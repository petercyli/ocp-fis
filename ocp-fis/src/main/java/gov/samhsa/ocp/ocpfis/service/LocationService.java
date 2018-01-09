package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    /**
     * Gets all available locations in the configured FHIR server
     *
     * @return
     */
    List<LocationDto> getAllLocations(Optional<Integer> page, Optional<Integer> size);

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     *
     * @param organizationResourceId
     * @return
     */
    List<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<Integer> page, Optional<Integer> size);

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
