package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.HealthcareServiceService;
import gov.samhsa.ocp.ocpfis.service.dto.HealthcareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController

public class HealthcareServiceController {

    @Autowired
    private HealthcareServiceService healthcareServiceService;

    @GetMapping("/healthcare-services")
    public PageDto<HealthcareServiceDto> getAllHealthcareServices(@RequestParam(value = "statusList") Optional<List<String>> statusList,
                                                                  @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                  @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                  @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                  @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return healthcareServiceService.getAllHealthcareServices(statusList, searchKey, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/organizations/{organizationId}/healthcare-services")
    public PageDto<HealthcareServiceDto> getAllHealthcareServicesByOrganization(@PathVariable String organizationId,
                                                                                @RequestParam(value = "assignedToLocationId") Optional<String> assignedToLocationId,
                                                                                @RequestParam(value = "statusList") Optional<List<String>> statusList,
                                                                                @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                                @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                                @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                                @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return healthcareServiceService.getAllHealthcareServicesByOrganization(organizationId, assignedToLocationId, statusList, searchKey, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/organizations/{organizationId}/locations/{locationId}/healthcare-services")
    public PageDto<HealthcareServiceDto> getAllHealthcareServiceByLocation(@PathVariable String organizationId,
                                                                           @PathVariable String locationId,
                                                                           @RequestParam(value = "statusList") Optional<List<String>> statusList,
                                                                           @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                           @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                           @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                           @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return healthcareServiceService.getAllHealthcareServicesByLocation(organizationId, locationId, statusList, searchKey, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/healthcare-services/{healthcareServiceId}")
    public HealthcareServiceDto getHealthcareService(@PathVariable String healthcareServiceId) {
        return healthcareServiceService.getHealthcareService(healthcareServiceId);
    }

    @PostMapping("/organization/{organizationId}/healthcare-services")
    @ResponseStatus(HttpStatus.CREATED)
    public void createHealthcareService(@PathVariable String organizationId,
                                        @Valid @RequestBody HealthcareServiceDto healthcareServiceDto, Optional<String> loggedInUser) {
        healthcareServiceService.createHealthcareService(organizationId, healthcareServiceDto, loggedInUser);

    }

    @PutMapping("/organization/{organizationId}/healthcare-services/{healthcareServiceId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateHealthcareService(@PathVariable String organizationId,
                                        @PathVariable String healthcareServiceId,
                                        @Valid @RequestBody HealthcareServiceDto healthcareServiceDto, Optional<String> loggedInUser) {
        healthcareServiceService.updateHealthcareService(organizationId, healthcareServiceId, healthcareServiceDto, loggedInUser);

    }

    @PutMapping("/healthcare-services/{healthcareServiceId}/assign")
    @ResponseStatus(HttpStatus.OK)
    public void assignLocationsToHealthcareService(@PathVariable String healthcareServiceId,
                                                  @RequestParam(value = "organizationId") String organizationId,
                                                  @RequestParam(value = "locationIdList") List<String> locationIdList) {
        healthcareServiceService.assignLocationsToHealthcareService(healthcareServiceId, organizationId, locationIdList);
    }

    @PutMapping("/healthcare-services/{healthcareServiceId}/unassign")
    @ResponseStatus(HttpStatus.OK)
    public void unassignsLocationFromHealthcareService(@PathVariable String healthcareServiceId,
                                                    @RequestParam(value = "organizationId") String organizationId,
                                                    @RequestParam(value = "locationIdList") List<String> locationIdList) {
        healthcareServiceService.unassignLocationsFromHealthcareService(healthcareServiceId, organizationId, locationIdList);
    }

    @PutMapping("/healthcare-services/{healthcareServiceId}/inactive")
    @ResponseStatus(HttpStatus.OK)
    public void inactivateHealthcareService(@PathVariable String healthcareServiceId) {
        healthcareServiceService.inactivateHealthcareService(healthcareServiceId);
    }
}
