package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {
    public enum SearchType {
        identifier, name
    }

    @Autowired
    private PractitionerService practitionerService;

    @GetMapping("/search")
    public PageDto<PractitionerDto> searchPractitioners(@RequestParam SearchType searchType, @RequestParam String searchValue, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return practitionerService.searchPractitioners(searchType, searchValue, showInactive, page, size);
    }

    @GetMapping
    public List<ReferenceDto> getPractitionersInOrganizationByPractitionerId(@RequestParam Optional<String> practitioner, @RequestParam Optional<String> role) {
        if (practitioner.isPresent()) {
            return practitionerService.getPractitionersInOrganizationByPractitionerId(practitioner.get());
        } else if (role.isPresent()) {
            return practitionerService.getPractitionersByRole(role.get());
        }
        return null;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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