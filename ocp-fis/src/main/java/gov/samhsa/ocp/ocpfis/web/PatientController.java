package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PatientService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/patients")
public class PatientController {

    public enum SearchType {
        identifier, name, logicalId
    }

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }


    @GetMapping("/search")
    public PageDto<PatientDto> getPatientsByValue(@RequestParam(value = "type", defaultValue = "name") Optional<String> searchKey,
                                                  @RequestParam(value = "value") Optional<String> searchValue,
                                                  @RequestParam(value="organization") Optional<String> organization,
                                                  @RequestParam(value="assigned") Optional<Boolean> assigned,
                                                  @RequestParam(value = "showInactive", defaultValue = "false") Optional<Boolean> showInactive,
                                                  @RequestParam Optional<Integer> page,
                                                  @RequestParam Optional<Integer> size,
                                                  @RequestParam(value="showAll") Optional<Boolean> showAll) {
        return patientService.getPatientsByValue(searchKey, searchValue, organization, assigned, showInactive, page, size,showAll);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createPatient(@Valid @RequestBody PatientDto patientDto) {
        patientService.createPatient(patientDto);
        log.info("Patient successfully created");
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public void updatePatient(@Valid @RequestBody PatientDto patientDto) {
        patientService.updatePatient(patientDto);
        log.info("Patient successfully updated");
    }

    @GetMapping("/{patientId}")
    public PatientDto getPatientById(@PathVariable String patientId) {
        return patientService.getPatientById(patientId);
    }

    @GetMapping
    PageDto<PatientDto> getPatients(@RequestParam(value = "practitioner") Optional<String> practitioner,
                                                         @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                         @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                         @RequestParam(value = "showInActive") Optional<Boolean> showInactive,
                                                         @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                         @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return patientService.getPatientsByPractitioner(practitioner, searchKey, searchValue, showInactive, pageNumber, pageSize);
    }

}