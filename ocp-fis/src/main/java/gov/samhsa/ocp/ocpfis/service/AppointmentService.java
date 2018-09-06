package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.OutsideParticipant;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String> patientId, Optional<String> practitionerId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    List<AppointmentDto> getNonDeclinedAppointmentsWithNoPagination(Optional<List<String>> statusList, Optional<String> patientId, Optional<String> practitionerId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<Boolean> sortByStartTimeAsc);

    PageDto<AppointmentDto> getAppointmentsByPractitionerAndAssignedCareTeamPatients(String practitionerId, Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createAppointment(AppointmentDto appointmentDto, Optional<String> loggedInUser);

    void updateAppointment(String appointmentId, AppointmentDto appointmentDto, Optional<String> loggedInUser);

    List<ParticipantReferenceDto> getAppointmentParticipants(String patientId, Optional<List<String>> roles, Optional<String> appointmentId);

    AppointmentDto getAppointmentById(String appointmentId);

    void cancelAppointment(String appointmentId);

    void acceptAppointment(String appointmentId, String actorReference);

    void declineAppointment(String appointmentId, String actorReference);

    void tentativelyAcceptAppointment(String appointmentId, String actorReference);

    List<AppointmentParticipantReferenceDto> getAllHealthcareServicesReferences(String resourceType, String resourceValue);

    List<AppointmentParticipantReferenceDto> getAllLocationReferences(String resourceType, String resourceValue);

    List<AppointmentParticipantReferenceDto> getPractitionersReferences(String resourceType, String resourceValue);

    List<OutsideParticipant> searchOutsideParticipants(String patient, String participantType, String name, String organization, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll);
}
