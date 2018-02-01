package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.HealthCareServiceService;
import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController

public class HealthCareServiceController {

    @Autowired
    private HealthCareServiceService healthCareServiceService;

    @GetMapping("/health-care-services")
    public PageDto<HealthCareServiceDto> getAllHealthCareServices(@RequestParam(value = "statusList") Optional<List<String>> statusList,
                                                                  @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                  @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                  @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                  @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return healthCareServiceService.getAllHealthCareServices(statusList, searchKey, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/organizations/{organizationId}/health-care-services")
    public PageDto<HealthCareServiceDto> getAllHealthCareServicesByOrganization(@PathVariable String organizationId,
                                                                                @RequestParam(value = "locationId") Optional<String> locationId,
                                                                                @RequestParam(value = "statusList") Optional<List<String>> statusList,
                                                                                @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                                @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                                @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                                @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return healthCareServiceService.getAllHealthCareServicesByOrganization(organizationId, locationId, statusList, searchKey, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/health-care-services/{healthCareServiceId}")
    public HealthCareServiceDto getHealthCareService(@PathVariable String healthCareServiceId) {
        return healthCareServiceService.getHealthCareService(healthCareServiceId);
    }


    @PutMapping("/health-care-services/{healthCareServiceId}/assign")
    public void assignLocationToHealthCareService(@PathVariable String healthCareServiceId,
                                                  @RequestParam(value = "organizationId") String organizationId,
                                                  @RequestParam(value = "locationIdList") List<String> locationIdList) {
        healthCareServiceService.assignLocationToHealthCareService(healthCareServiceId, organizationId, locationIdList);
    }


}
