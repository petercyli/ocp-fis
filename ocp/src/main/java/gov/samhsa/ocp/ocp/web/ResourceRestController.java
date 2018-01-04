package gov.samhsa.ocp.ocp.web;

import gov.samhsa.ocp.ocp.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResourceRestController {

    @Autowired
    private ResourceService resourceService;

    @GetMapping("/resources")
    public String getPatientFhirResource(@RequestParam String patientIdentifierSystem, @RequestParam String patientIdentifierValue) {
        return resourceService.getFhirResourceByPatientIdentifier(patientIdentifierSystem, patientIdentifierValue);
    }
}
