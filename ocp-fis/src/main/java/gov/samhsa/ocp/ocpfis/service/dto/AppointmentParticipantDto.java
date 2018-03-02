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
    //private String participationTypeSystem;
    private String participationTypeDisplay;

    private String actorReference;
    private String actorName;

    private String participantRequiredCode;
    //private String participantRequiredSystem;
    private String participantRequiredDisplay;

    private String participationStatusCode;
    //private String participationStatusSystem;
    private String participationStatusDisplay;
}
