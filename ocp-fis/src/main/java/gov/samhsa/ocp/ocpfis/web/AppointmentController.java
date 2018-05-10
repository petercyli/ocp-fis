package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.AppointmentService;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public void createAppointment(@Valid @RequestBody AppointmentDto appointmentDto) {
        appointmentService.createAppointment(appointmentDto);
    }

    @PutMapping("/appointments/{appointmentId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateAppointment(@PathVariable String appointmentId, @Valid @RequestBody AppointmentDto appointmentDto) {
        appointmentService.updateAppointment(appointmentId, appointmentDto);
    }

    @GetMapping("/patients/{patientId}/appointmentParticipants")
    public List<ParticipantReferenceDto> getAppointmentParticipants(@PathVariable String patientId,
                                                                    @RequestParam(value = "roles", required = false) Optional<List<String>> roles,
                                                                    @RequestParam(value = "appointmentId", required = false) Optional<String> appointmentId) {
        return appointmentService.getAppointmentParticipants(patientId, roles, appointmentId);
    }

    @GetMapping("/appointments/{appointmentId}")
    public AppointmentDto getAppointmentById(@PathVariable String appointmentId) {
        return appointmentService.getAppointmentById(appointmentId);
    }

    @GetMapping("/appointments/search")
    public PageDto<AppointmentDto> getAppointments(@RequestParam Optional<List<String>> statusList,
                                                   @RequestParam(value = "requesterReference") Optional<String> requesterReference,
                                                   @RequestParam(value = "patientId") Optional<String> patientId,
                                                   @RequestParam(value = "practitionerId") Optional<String> practitionerId,
                                                   @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                   @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                   @RequestParam(value = "showPastAppointments") Optional<Boolean> showPastAppointments,
                                                   @RequestParam(value = "sortByStartTimeAsc", defaultValue = "true") Optional<Boolean> sortByStartTimeAsc,
                                                   @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                   @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return appointmentService.getAppointments(statusList, requesterReference, patientId, practitionerId, searchKey, searchValue, showPastAppointments, sortByStartTimeAsc, pageNumber, pageSize);
    }

    @GetMapping("/appointments/search-with-no-pagination")
    public List<AppointmentDto> getAppointmentsWithNoPagination(@RequestParam Optional<List<String>> statusList,
                                                   @RequestParam(value = "patientId") Optional<String> patientId,
                                                   @RequestParam(value = "practitionerId") Optional<String> practitionerId,
                                                   @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                   @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                   @RequestParam(value = "showPastAppointments") Optional<Boolean> showPastAppointments,
                                                   @RequestParam(value = "sortByStartTimeAsc", defaultValue = "true") Optional<Boolean> sortByStartTimeAsc) {
        return appointmentService.getAppointmentsWithNoPagination(statusList, patientId, practitionerId, searchKey, searchValue, showPastAppointments, sortByStartTimeAsc);
    }

    @PutMapping("/appointments/{appointmentId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public void cancelAppointment(@PathVariable String appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
    }

    @PutMapping("/appointments/{appointmentId}/accept")
    @ResponseStatus(HttpStatus.OK)
    public void acceptAppointment(@PathVariable String appointmentId,
                                  @RequestParam(value = "actorReference") String actorReference) {
        appointmentService.acceptAppointment(appointmentId, actorReference);
    }

    @PutMapping("/appointments/{appointmentId}/decline")
    @ResponseStatus(HttpStatus.OK)
    public void declineAppointment(@PathVariable String appointmentId,
                                   @RequestParam(value = "actorReference") String actorReference) {
        appointmentService.declineAppointment(appointmentId, actorReference);
    }

    @PutMapping("/appointments/{appointmentId}/tentative")
    @ResponseStatus(HttpStatus.OK)
    public void tentativelyAcceptAppointment(@PathVariable String appointmentId,
                                             @RequestParam(value = "actorReference") String actorReference) {
        appointmentService.tentativelyAcceptAppointment(appointmentId, actorReference);
    }

}
