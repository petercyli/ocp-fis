package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CoverageService;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
public class CoverageController {

    @Autowired
    private CoverageService coverageService;

    @PostMapping("/coverage")
    public void createCoverage(@Valid @RequestBody CoverageDto coverageDto){
        coverageService.createCoverage(coverageDto);
    }

    @GetMapping("/patients/{patientId}/subscriber-options")
    public List<ReferenceDto> getSubscriberOptions(@PathVariable String patientId){
       return coverageService.getSubscriberOptions(patientId);
    }
}
