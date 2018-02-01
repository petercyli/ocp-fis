package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.HealthCareServiceService;
import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController

public class HealthCareServiceController {

    @Autowired
    private HealthCareServiceService healthCareServiceService;

    @GetMapping("/healthcareservices")
    public PageDto<HealthCareServiceDto> getAllHealthCareServices(@RequestParam(value = "statusList") Optional<List<String>> statusList,
                                                @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return healthCareServiceService.getAllHealthCareServices(statusList, searchKey, searchValue, pageNumber, pageSize);
    }
}
