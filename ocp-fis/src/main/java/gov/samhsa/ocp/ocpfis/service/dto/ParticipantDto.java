package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDto {
    private ParticipantRoleDto role;

    private ParticipantMemberDto member;

    private ParticipantOnBehalfOfDto onBehalfOfDto;
}
