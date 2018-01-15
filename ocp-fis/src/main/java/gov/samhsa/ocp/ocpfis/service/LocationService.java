package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

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
    PageDto<LocationDto> getAllLocations(Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size);

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     * @param organizationResourceId
     * @param status
     * @param page
     * @param size
     * @return
     */
    PageDto<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size);

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

    /**
     *
     * @param organizationId
     * @param locationId
     * @param locationDto
     */
    void createLocation(String organizationId, Optional<String> locationId, LocationDto locationDto);
}
