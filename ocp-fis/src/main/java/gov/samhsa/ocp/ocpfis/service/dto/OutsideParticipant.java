package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutsideParticipant {
    String name;
    String participantId;
    String identifierType;
    String identifierValue;
    List<ReferenceDto> associatedOrganizations;
    AppointmentParticipantReferenceDto appointmentParticipantReferenceDto;
}
