package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    /**
     *
     * @param status
     * @param page
     * @param size
     * @return
     */
    List<LocationDto> getAllLocations(Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size);

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     * @param organizationResourceId
     * @param status
     * @param page
     * @param size
     * @return
     */
    List<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size);

    /**
     *
     * @param locationId
     * @return
     */
    LocationDto getLocation(String locationId);

    /**
     * Gets level 1 child location for a given Location Id
     * @param locationId
     * @return
     */
    LocationDto getChildLocation(String locationId);
}
