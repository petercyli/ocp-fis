package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.AppointmentService;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

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

    @GetMapping("/search")
    public PageDto<AppointmentDto> getAppointments(@RequestParam Optional<List<String>> statusList,
                                                   @RequestParam(value = "searchKey") String searchKey,
                                                   @RequestParam(value = "searchValue") String searchValue,
                                                   @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                   @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return appointmentService.getAppointments(statusList, searchKey, searchValue, pageNumber, pageSize);
    }

}
