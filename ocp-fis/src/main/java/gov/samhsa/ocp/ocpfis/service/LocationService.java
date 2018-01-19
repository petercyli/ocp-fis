package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    /**
     * @param statusList
     * @param searchKey
     * @param searchValue
     * @param page
     * @param size
     * @return
     */
    PageDto<LocationDto> getAllLocations(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     *
     * @param organizationResourceId
     * @param statusList
     * @param searchKey
     * @param searchValue
     * @param page
     * @param size
     * @return
     */
    PageDto<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    /**
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
