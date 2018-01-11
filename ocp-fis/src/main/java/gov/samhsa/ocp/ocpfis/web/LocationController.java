package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.LocationService;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
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
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/locations")
    public List<LocationDto> getAllLocations(@RequestParam Optional<List<String>> status,
                @RequestParam Optional<Integer> page,
                @RequestParam Optional<Integer> size) {
        return locationService.getAllLocations(status,page, size);
    }

    /**
     *
     * @param organizationId
     * @param status
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/organizations/{organizationId}/locations")
    public List<LocationDto> getLocationsByOrganization(@PathVariable String organizationId,
                                                        @RequestParam Optional<List<String>> status,
                                                        @RequestParam Optional<Integer> page,
                                                        @RequestParam Optional<Integer> size) {
        return locationService.getLocationsByOrganization(organizationId, status, page, size);
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
