package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.LocationService;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Gets all available locations in the configured FHIR server
     *
     * @return
     */
    @GetMapping("/locations")
    public List<LocationDto> getAllLocations() {
        return locationService.getAllLocations();
    }

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     *
     * @param organizationId
     * @return
     */
    @GetMapping("/organizations/{organizationId}/locations")
    public List<LocationDto> getLocationsByOrganization(@PathVariable String organizationId) {
        return locationService.getLocationsByOrganization(organizationId);
    }

    /**
     * Get Location By Id
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
     *
     * @param locationId
     * @return
     */
    @GetMapping("/locations/{locationId}/childLocation")
    public LocationDto getChildLocation(@PathVariable String locationId) {
        return locationService.getChildLocation(locationId);
    }

}
