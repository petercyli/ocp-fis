package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.AppointmentService;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createAppointment(@Valid @RequestBody AppointmentDto appointmentDto) {
        appointmentService.createAppointment(appointmentDto);
    }
}
