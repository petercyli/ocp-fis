package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {
    private String definitionReference;

    private String definitionName;

    private String partOf;

    private ValueSetDto status;

    private ValueSetDto intent;

    private ValueSetDto priority;

    private String description;

    private String forReference;

    private String context;

    private PeriodDto executionPeriod;

    private LocalDateTime authoredOn;

    private LocalDateTime lastModified;

    //confused
    private String requester;

    //confused
    private String agent;

    private ValueSetDto performerType;

    private String owner;

    private String note;
}
