package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.codesystems.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static gov.samhsa.ocp.ocpfis.util.FhirDtoUtil.mapReferenceDtoToReference;

public class TaskDtoToTaskMap {

    public static Task map(TaskDto taskDto) {
        Task task = new Task();
        task.setDefinition(FhirDtoUtil.mapReferenceDtoToReference(taskDto.getDefinition()));

        if (taskDto.getPartOf() != null) {
            List<Reference> partOfReferences = new ArrayList<>();
            partOfReferences.add(mapReferenceDtoToReference(taskDto.getPartOf()));
            task.setPartOf(partOfReferences);
        }

        task.setStatus(Task.TaskStatus.valueOf(taskDto.getStatus().getCode().toUpperCase()));
        task.setIntent(Task.TaskIntent.valueOf(taskDto.getIntent().getCode().toUpperCase()));
        task.setPriority(Task.TaskPriority.valueOf(taskDto.getPriority().getCode().toUpperCase()));

        if (taskDto.getDescription() != null && !taskDto.getDescription().isEmpty()) {
            task.setDescription(taskDto.getDescription());
        }

        task.setFor(mapReferenceDtoToReference(taskDto.getBeneficiary()));

        //Set execution Period
        if (taskDto.getExecutionPeriod() != null) {
            if (taskDto.getExecutionPeriod().getStart() != null)
                task.getExecutionPeriod().setStart(java.sql.Date.valueOf(taskDto.getExecutionPeriod().getStart()));
        } else if (taskDto.getStatus().getCode().equalsIgnoreCase(TaskStatus.INPROGRESS.toCode()))
            task.getExecutionPeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));

        if (taskDto.getExecutionPeriod() != null) {
            if (taskDto.getExecutionPeriod().getEnd() != null)
                task.getExecutionPeriod().setEnd(java.sql.Date.valueOf(taskDto.getExecutionPeriod().getEnd()));
        } else if (taskDto.getStatus().getCode().equalsIgnoreCase(TaskStatus.COMPLETED.toCode()))
            task.getExecutionPeriod().setEnd(java.sql.Date.valueOf(LocalDate.now()));

        //Set agent
        task.getRequester().setAgent(mapReferenceDtoToReference(taskDto.getAgent()));

        //Set on Behalf of
        if (taskDto.getOnBehalfOf() != null) {
            task.getRequester().setOnBehalfOf(mapReferenceDtoToReference(taskDto.getOnBehalfOf()));
        }

        //Set PerformerType
        if (taskDto.getPerformerType() != null) {
            List<CodeableConcept> codeableConcepts = new ArrayList<>();
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.addCoding().setCode(taskDto.getPerformerType().getCode())
                    .setDisplay(taskDto.getPerformerType().getDisplay())
                    .setSystem(taskDto.getPerformerType().getSystem());
            codeableConcepts.add(codeableConcept);
            task.setPerformerType(codeableConcepts);
        }

        //Set last Modified
        task.setLastModified(java.sql.Date.valueOf(LocalDate.now()));

        task.setOwner(mapReferenceDtoToReference(taskDto.getOwner()));

        Annotation annotation = new Annotation();
        annotation.setText(taskDto.getNote());
        List<Annotation> annotations = new ArrayList<>();
        annotations.add(annotation);
        task.setNote(annotations);

        return task;
    }
}
