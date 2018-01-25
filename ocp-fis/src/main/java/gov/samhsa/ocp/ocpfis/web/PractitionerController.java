package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {
    public enum SearchType {
        identifier, name
    }

    @Autowired
    private PractitionerService practitionerService;

    @GetMapping
    public PageDto<PractitionerDto> getPractitioners(@RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return practitionerService.getAllPractitioners(showInactive, page, size);
    }

    @GetMapping("/search")
    public PageDto<PractitionerDto> searchPractitioners(@RequestParam SearchType searchType, @RequestParam String searchValue, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return practitionerService.searchPractitioners(searchType, searchValue, showInactive, page, size);
    }

    @PostMapping
    public void createPractitioner(@Valid @RequestBody PractitionerDto practitionerDto) {
        practitionerService.createPractitioner(practitionerDto);
    }

    @GetMapping("/{practitionerId}")
    public PractitionerDto getPractitioner(@PathVariable String practitionerId) {
        return practitionerService.getPractitioner(practitionerId);
    }

    @PutMapping("/{practitionerId}")
    @ResponseStatus(HttpStatus.OK)
    public void updatePractitioner(@PathVariable String practitionerId, @Valid @RequestBody PractitionerDto practitionerDto) {
        practitionerService.updatePractitioner(practitionerId, practitionerDto);
    }
}