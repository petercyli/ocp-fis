package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionParticipantDto {
    private String actionRoleCode;
    private String actionRoleDisplay;
    private String actionRoleSystem;

    private String actionTypeCode;
    private String actionTypeDisplay;
    private String actionTypeSystem;
}
