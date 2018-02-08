package gov.samhsa.ocp.ocpfis.service.dto;

import gov.samhsa.ocp.ocpfis.service.validation.ParticipantRoleCodeConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDto {

    @ParticipantRoleCodeConstraint
    private String roleCode;

    private String roleDisplay;

    private String startDate;

    private String endDate;

    private Optional<String> memberFirstName;

    private Optional<String> memberLastName;

    private Optional<String> memberName;

    private String memberId;

    private String memberType;

    private String onBehalfOfId;

    private String onBehalfOfName;
}
