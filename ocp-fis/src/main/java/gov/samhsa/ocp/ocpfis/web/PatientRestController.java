package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/patients")
public class PatientRestController {

    @Autowired
    private PatientService patientService;

    @PostMapping
    void publishFhirPatient(@Valid @RequestBody PatientDto patientDto) {
        patientService.publishFhirPatient(patientDto);
    }

    @PutMapping
    void updateFhirPatient(@Valid @RequestBody PatientDto patientDto) {

        patientService.updateFhirPatient(patientDto);
    }

}
