package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PatientService;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchPatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchType;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/patients")
public class PatientController {

    final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }


    @GetMapping
    public Set<PatientDto> getPatients() {
        return patientService.getPatients();
    }

    @PostMapping("/search")
    public Set<PatientDto> searchPatient(@Valid @RequestBody SearchPatientDto searchPatientDto) {
        return patientService.searchPatient(searchPatientDto);
    }

    @GetMapping("/search")
    public Set<PatientDto> getPatientsByValue( @RequestParam(value = "value") String value,@RequestParam(value = "type") String type,@RequestParam(value = "showInactive", defaultValue = "false") boolean showInactive) {
        if( type == null || !Arrays.stream(SearchType.values()).anyMatch(searchType -> searchType.name().equalsIgnoreCase(type.trim()))){
            throw new BadRequestException("Invalid Type Value. It should be either Name or Identifier");
        }
        return patientService.getPatientsByValue(value,type,showInactive);
    }
}