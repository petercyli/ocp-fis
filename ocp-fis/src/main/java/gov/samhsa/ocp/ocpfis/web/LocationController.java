package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.LocationService;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     *
     * @param status
     * @param searchKey
     * @param searchValue
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/locations")
    public PageDto<LocationDto> getAllLocations(@RequestParam(value = "status") Optional<List<String>> status,
                                                @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                @RequestParam Optional<Integer> page,
                                                @RequestParam Optional<Integer> size) {
        return locationService.getAllLocations(status, searchKey, searchValue, page, size);
    }

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     * @param organizationId
     * @param status
     * @param searchKey
     * @param searchValue
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/organizations/{organizationId}/locations")
    public PageDto<LocationDto> getLocationsByOrganization(@PathVariable String organizationId,
                                                           @RequestParam(value = "status") Optional<List<String>> status,
                                                           @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                           @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                           @RequestParam Optional<Integer> page,
                                                           @RequestParam Optional<Integer> size) {
        return locationService.getLocationsByOrganization(organizationId, status, searchKey, searchValue, page, size);
    }

    /**
     *
     * @param locationId
     * @return
     */
    @GetMapping("/locations/{locationId}")
    public LocationDto getLocation(@PathVariable String locationId) {
        return locationService.getLocation(locationId);
    }

    /**
     * Gets level 1 child location for a given Location Id
     * @param locationId
     * @return
     */
    @GetMapping("/locations/{locationId}/childLocation")
    public LocationDto getChildLocation(@PathVariable String locationId) {
        return locationService.getChildLocation(locationId);
    }

}
