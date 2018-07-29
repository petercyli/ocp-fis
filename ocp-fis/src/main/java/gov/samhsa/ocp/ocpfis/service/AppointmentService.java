package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String> patientId, Optional<String> practitionerId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    List<AppointmentDto> getAppointmentsWithNoPagination(Optional<List<String>> statusList, Optional<String> patientId, Optional<String> practitionerId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<Boolean> sortByStartTimeAsc);

    PageDto<AppointmentDto> getAppointmentsByPractitionerAndAssignedCareTeamPatients(String practitionerId, Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createAppointment(AppointmentDto appointmentDto, Optional<String> loggedInUser);

    void updateAppointment(String appointmentId, AppointmentDto appointmentDto, Optional<String> loggedInUser);

    List<ParticipantReferenceDto> getAppointmentParticipants(String patientId, Optional<List<String>> roles, Optional<String> appointmentId);

    AppointmentDto getAppointmentById(String appointmentId);

    void cancelAppointment(String appointmentId);

    void acceptAppointment(String appointmentId, String actorReference);

    void declineAppointment(String appointmentId, String actorReference);

    void tentativelyAcceptAppointment(String appointmentId, String actorReference);
}
