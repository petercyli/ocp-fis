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
    private String participantRequiredCode;
    private String participationStatusCode;

    private String actorReference;
    private String actorName;
}
