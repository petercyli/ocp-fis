package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContextDto {
    private String logicalId;

    private String status;

    private ValueSetDto type;

    private String patient;

    private String managingOrganization;

    private PeriodDto period;

    private String referralRequest;

    private String careManager;
}
