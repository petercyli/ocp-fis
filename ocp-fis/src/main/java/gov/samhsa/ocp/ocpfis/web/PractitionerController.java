package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController

public class PractitionerController {

    @Autowired
    private PractitionerService practitionerService;

    @RequestMapping("/practitioners")
    public List<PractitionerDto> getPractitioners(){
        return practitionerService.getAllPractitioners();
    }
}