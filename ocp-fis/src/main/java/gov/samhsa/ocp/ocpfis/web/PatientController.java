package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PatientService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchType;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }


    @GetMapping
    public List<PatientDto> getPatients() {
        return patientService.getPatients();
    }


    @GetMapping("/search")
    public PageDto<PatientDto> getPatientsByValue(@RequestParam(value = "value") String value, @RequestParam(value = "type", defaultValue = "name") String type, @RequestParam(value = "showInactive", defaultValue = "false") boolean showInactive,
                                                  @RequestParam Optional<Integer> page,
                                                  @RequestParam Optional<Integer> size) {
        if (type == null || !Arrays.stream(SearchType.values()).anyMatch(searchType -> searchType.name().equalsIgnoreCase(type.trim()))) {
            throw new BadRequestException("Invalid Type Value. It should be either Name or Identifier");
        }
        return patientService.getPatientsByValue(value, type, showInactive, page, size);
    }
}