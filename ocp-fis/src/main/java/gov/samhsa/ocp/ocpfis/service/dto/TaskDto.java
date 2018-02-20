package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {
    private String logicalId;

    private String definitionReference;

    private String definitionName;

    private String partOf;

    private ValueSetDto status;

    private ValueSetDto intent;

    private ValueSetDto priority;

    private String description;

    private String beneficiaryReference;

    private String context;

    private PeriodDto executionPeriod;

    private LocalDate authoredOn;

    private LocalDate lastModified;

    private String agent;

    private ValueSetDto performerType;

    private String owner;

    private String note;
}
