package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.List;
import java.util.Optional;

@Slf4j
public class TaskToTaskDtoMap {

    public static TaskDto map(Task task, List<ValueSetDto> taskPerformerTypes) {
        TaskDto taskDto = new TaskDto();
        ValueSetDto performerTypeDto = new ValueSetDto();

        taskDto.setLogicalId(task.getIdElement().getIdPart());
        taskDto.setDescription(task.getDescription());
        if (task.getNote() != null && (task.getNote().size() > 0))
            taskDto.setNote(task.getNote().get(0).getText());

        if (task.getStatus() != null) {

            taskDto.setStatus(ValueSetDto.builder()
                    .code((task.getStatus().toCode() != null && !task.getStatus().toCode().isEmpty()) ? task.getStatus().toCode() : null)
                    .display((task.getStatus().getDisplay() != null && !task.getStatus().getDisplay().isEmpty()) ? task.getStatus().getDisplay() : null)
                    .build());
        }

        if (task.getIntent() != null) {

            taskDto.setIntent(ValueSetDto.builder()
                    .code((task.getIntent().toCode() != null && !task.getIntent().toCode().isEmpty()) ? task.getIntent().toCode() : null)
                    .display((task.getIntent().getDisplay() != null && !task.getIntent().getDisplay().isEmpty()) ? task.getIntent().getDisplay() : null)
                    .build());
        }

        if (task.getPriority() != null) {
            taskDto.setPriority(ValueSetDto.builder()
                    .code((task.getPriority().toCode() != null && !task.getPriority().toCode().isEmpty()) ? task.getPriority().toCode() : null)
                    .display((task.getPriority().getDisplay() != null && !task.getPriority().getDisplay().isEmpty()) ? task.getPriority().getDisplay() : null)
                    .build());
        }

        if (task.getPerformerType() != null) {
            task.getPerformerType().stream().findFirst().ifPresent(performerType -> performerType.getCoding().stream().findFirst().ifPresent(coding -> {
                performerTypeDto.setCode((coding.getCode() != null && !coding.getCode().isEmpty()) ? coding.getCode() : null);
                performerTypeDto.setDisplay((FhirDtoUtil.getDisplayForCode(coding.getCode(), Optional.ofNullable(taskPerformerTypes))).orElse(null));
            }));

            taskDto.setPerformerType(performerTypeDto);
        }

        if (task.hasPartOf()) {
            taskDto.setPartOf(ReferenceDto.builder()
                    .reference((task.getPartOf().get(0).getReference() != null && !task.getPartOf().get(0).getReference().isEmpty()) ? task.getPartOf().get(0).getReference() : null)
                    .display((task.getPartOf().get(0).getDisplay() != null && !task.getPartOf().get(0).getDisplay().isEmpty()) ? task.getPartOf().get(0).getDisplay() : null)
                    .build());
        }

        if (task.hasFor()) {
            taskDto.setBeneficiary(ReferenceDto.builder()
                    .reference((task.getFor().getReference() != null && !task.getFor().getReference().isEmpty()) ? task.getFor().getReference() : null)
                    .display((task.getFor().getDisplay() != null && !task.getFor().getDisplay().isEmpty()) ? task.getFor().getDisplay() : null)
                    .build());
        }

        if (task.hasRequester()) {
            if (task.getRequester().hasOnBehalfOf())
                taskDto.setOnBehalfOf(ReferenceDto.builder()
                        .reference((task.getRequester().getOnBehalfOf().getReference() != null && !task.getRequester().getOnBehalfOf().getReference().isEmpty()) ? task.getRequester().getOnBehalfOf().getReference() : null)
                        .display((task.getRequester().getOnBehalfOf().getDisplay() != null && !task.getRequester().getOnBehalfOf().getDisplay().isEmpty()) ? task.getRequester().getOnBehalfOf().getDisplay() : null)
                        .build());
        }

        if (task.hasRequester()) {
            if (task.getRequester().hasAgent())
                taskDto.setAgent(ReferenceDto.builder()
                        .reference((task.getRequester().getAgent().getReference() != null && !task.getRequester().getAgent().getReference().isEmpty()) ? task.getRequester().getOnBehalfOf().getReference() : null)
                        .display((task.getRequester().getAgent().getDisplay() != null && !task.getRequester().getAgent().getDisplay().isEmpty()) ? task.getRequester().getOnBehalfOf().getDisplay() : null)
                        .build());
        }

        if (task.hasOwner()) {
            taskDto.setOwner(ReferenceDto.builder()
                    .reference((task.getOwner().getReference() != null && !task.getOwner().getReference().isEmpty()) ? task.getOwner().getReference() : null)
                    .display((task.getOwner().getDisplay() != null && !task.getOwner().getDisplay().isEmpty()) ? task.getOwner().getDisplay() : null)
                    .build());
        }

        if (task.hasDefinition()) {
            try {
                taskDto.setDefinition(ReferenceDto.builder()
                        .reference((task.hasDefinitionReference()) ? task.getDefinitionReference().getReference() : null)
                        .display((task.hasDefinitionReference()) ? task.getDefinitionReference().getDisplay() : null)
                        .build());
            } catch (FHIRException e) {
                log.error("FHIR Exception when setting task definition", e);
            }
        }

        //TODO: redo context field

        if (task.hasLastModified()) {
            taskDto.setLastModified(DateUtil.convertDateToLocalDate(task.getLastModified()));
        }

        if (task.hasAuthoredOn()) {
            taskDto.setAuthoredOn(DateUtil.convertDateToLocalDate(task.getAuthoredOn()));
        }

        if (task.getExecutionPeriod() != null && !task.getExecutionPeriod().isEmpty()) {
            PeriodDto periodDto = new PeriodDto();
            taskDto.setExecutionPeriod(periodDto);
            taskDto.getExecutionPeriod().setStart((task.getExecutionPeriod().hasStart()) ? DateUtil.convertDateToLocalDate(task.getExecutionPeriod().getStart()) : null);
            taskDto.getExecutionPeriod().setEnd((task.getExecutionPeriod().hasEnd()) ? DateUtil.convertDateToLocalDate(task.getExecutionPeriod().getEnd()) : null);
        }

        return taskDto;
    }
}
