package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {

    @Autowired
    private PractitionerService practitionerService;

    @GetMapping
    public List<PractitionerDto> getPractitioners(){
        return practitionerService.getAllPractitioners();
    }

    @GetMapping("/search")
    public List<PractitionerDto> searchPractitioners(@RequestParam String searchValue){return practitionerService.searchPractitioners(searchValue);}
}