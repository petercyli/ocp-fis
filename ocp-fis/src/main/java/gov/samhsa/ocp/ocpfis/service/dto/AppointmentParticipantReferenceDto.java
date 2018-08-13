package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentParticipantReferenceDto {
    private String reference;

    private String display;

    private Optional<String> participationTypeCode;
    private Optional<String> participationTypeDisplay;
    private Optional<String> participationTypeSystem;

    private Optional<String> participantRequiredCode;
    private Optional<String> participantRequiredDisplay;
    private Optional<String> participantRequiredSystem;

    private Optional<String> participantStatusCode;
    private Optional<String> participantStatusDisplay;
    private Optional<String> participantStatusSystem;
}
