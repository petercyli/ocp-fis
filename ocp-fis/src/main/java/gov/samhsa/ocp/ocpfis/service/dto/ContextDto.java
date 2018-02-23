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

    //Patient who is the focus of this context.
    private ReferenceDto patient;

    private ReferenceDto managingOrganization;

    //Start date as active when enrollment task is active.
    private PeriodDto period;

    private ReferenceDto referralRequest;

    //Practitioner who create the task
    private ReferenceDto careManager;
}
