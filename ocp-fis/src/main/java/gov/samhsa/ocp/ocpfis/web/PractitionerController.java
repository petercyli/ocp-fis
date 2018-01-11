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
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {
    public enum SearchType {
        identifier,name
    }

    @Autowired
    private PractitionerService practitionerService;

    @GetMapping
    public List<PractitionerDto> getPractitioners(@RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return practitionerService.getAllPractitioners(showInactive, page, size);
    }

    @GetMapping("/search")
    public List<PractitionerDto> searchPractitioners(@RequestParam SearchType searchType, @RequestParam String searchValue, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return practitionerService.searchPractitioners(searchType, searchValue, showInactive, page, size);
    }
}