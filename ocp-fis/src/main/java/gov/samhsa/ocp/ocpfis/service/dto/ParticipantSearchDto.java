package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantSearchDto {

    private ParticipantMemberDto member;

    private List<IdentifierDto> identifiers;

    private List<TelecomDto> telecoms;

    private List<AddressDto> addresses;

    private List<PractitionerRoleDto> practitionerRoles;
}
