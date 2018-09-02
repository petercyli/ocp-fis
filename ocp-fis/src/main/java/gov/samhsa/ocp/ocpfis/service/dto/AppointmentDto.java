package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentDto {
    private String logicalId;

    private String statusCode;

    private String typeCode;
    private String typeDisplay;
    private String typeSystem;

    private String description;
    private String appointmentDate;
    private String appointmentDuration;
    private String patientName;
    private String patientId;

    private LocalDateTime start;
    private LocalDateTime end;
    private LocalDateTime created;

    private List<ReferenceDto> slot; //To be used later
    private List<ReferenceDto> incomingReferral;//To be used later

    //Used to identify person(Practitioner/Patient) who created the appointment from the UI
    private String creatorReference;
    private String creatorName;
    private String creatorRequired;
    private Optional<String> creatorRequiredDisplay;

    private List<AppointmentParticipantDto> participant;
    private List<String> participantName;

    //These help to show the required menu options on the UI
    private boolean canEdit;
    private boolean canCancel;
    private boolean canAccept;
    private boolean canDecline;
    private boolean canTentativelyAccept;
    private String requesterParticipationStatusCode;

    //Patient Demographics only
    PatientDto patient;
    LocationDto location;

}
