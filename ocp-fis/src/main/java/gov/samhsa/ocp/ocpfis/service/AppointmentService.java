package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String>  patientId, Optional<String>  practitionerId, Optional<String>  searchKey, Optional<String>  searchValue, Optional<Boolean> showPastAppointments, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize);
    void createAppointment(AppointmentDto appointmentDto);
    void updateAppointment(String appointmentId, AppointmentDto appointmentDto);
    List<ParticipantReferenceDto> getAppointmentParticipants(String patientId, Optional<List<String>> roles, Optional<String> appointmentId);
    AppointmentDto getAppointmentById(String appointmentId);
    void cancelAppointment(String appointmentId);
}
