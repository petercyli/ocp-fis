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

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/healthcareservice")

public class HealthCareServiceController {

    @Autowired
    private HealthCareServiceService healthCareServiceService;

    @GetMapping
    public PageDto<HealthCareServiceDto> getAllHealthCareServices(@RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return null;
    }
}
