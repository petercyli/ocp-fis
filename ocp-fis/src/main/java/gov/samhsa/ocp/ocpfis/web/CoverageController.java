package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CoverageService;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class CoverageController {

    @Autowired
    private CoverageService coverageService;

    @PostMapping("/coverage")
    public void createCoverage(@Valid @RequestBody CoverageDto coverageDto){
        coverageService.createCoverage(coverageDto);
    }
}
