package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentParticipantDto {

    private String participationTypeCode;
    private String participationTypeDisplay;
    private String participationTypeSystem;

    private String participantRequiredCode;
    private String participantRequiredDisplay;
    private String participantRequiredSystem;

    private String participationStatusCode;
    private String participationStatusDisplay;
    private String participationStatusSystem;

    private String actorReference;
    private String actorName;

    private String phone;
    private String email;

    private boolean isPatient;
    private boolean isPractitioner;
    private boolean isRelatedPerson;
    private boolean isLocation;
    private boolean isHealthcareService;
}
