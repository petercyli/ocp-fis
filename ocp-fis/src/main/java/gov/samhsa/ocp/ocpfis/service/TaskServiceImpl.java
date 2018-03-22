package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.TaskToTaskDtoMap;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.TaskDtoToTaskMap;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.codesystems.EpisodeofcareType;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static gov.samhsa.ocp.ocpfis.util.FhirDtoUtil.mapReferenceDtoToReference;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final IGenericClient fhirClient;
    private final LookUpService lookUpService;
    private final FisProperties fisProperties;
    private final EpisodeOfCareService episodeOfCareService;
    private final ActivityDefinitionService activityDefinitionService;
    private final PatientService patientService;

    @Autowired
    public TaskServiceImpl(IGenericClient fhirClient,
                           LookUpService lookUpService,
                           FisProperties fisProperties,
                           ActivityDefinitionService activityDefinitionService,
                           EpisodeOfCareService episodeOfCareService,
                           PatientService patientService) {
        this.fhirClient = fhirClient;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.activityDefinitionService = activityDefinitionService;
        this.episodeOfCareService = episodeOfCareService;
        this.patientService = patientService;
    }

    private static String createDisplayForEpisodeOfCare(TaskDto dto) {
        String status = dto.getDefinition() != null ? dto.getDefinition().getDisplay() : "NA";
        String date = dto.getExecutionPeriod() != null ? DateUtil.convertLocalDateToString(dto.getExecutionPeriod().getStart()) : "NA";
        String agent = dto.getAgent() != null ? dto.getAgent().getDisplay() : "NA";

        return new StringJoiner("-").add(status).add(date).add(agent).toString();
    }

    @Override
    public PageDto<TaskDto> getTasks(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfTasksPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Task.name());
        IQuery iQuery = fhirClient.search().forResource(Task.class);

        //Check for Patient
        if (searchKey.equalsIgnoreCase("patientId"))
            iQuery.where(new ReferenceClientParam("patient").hasId("Patient/" + searchValue));

        //Check for Organization
        if (searchKey.equalsIgnoreCase("organizationId"))
            iQuery.where(new ReferenceClientParam("organization").hasId("Organization/" + searchValue));

        //Check for Task
        if (searchKey.equalsIgnoreCase("taskId"))
            iQuery.where(new TokenClientParam("_id").exactly().code(searchValue));

        //Check for Status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            iQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        }

        Bundle firstPageTaskBundle;
        Bundle otherPageTaskBundle;
        boolean firstPage = true;

        firstPageTaskBundle = (Bundle) iQuery
                .count(numberOfTasksPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageTaskBundle == null || firstPageTaskBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Tasks were found in the FHIR server.");
        }

        otherPageTaskBundle = firstPageTaskBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageTaskBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageTaskBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageTaskBundle, pageNumber.get(), numberOfTasksPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedTasks = otherPageTaskBundle.getEntry();

        List<TaskDto> taskDtos = retrievedTasks.stream()
                .filter(retrivedBundle -> retrivedBundle.getResource().getResourceType().equals(ResourceType.Task))
                .map(retrievedTask -> {
                    Task task = (Task) retrievedTask.getResource();
                    return TaskToTaskDtoMap.map(task, lookUpService.getTaskPerformerType());
                }).collect(toList());

        double totalPages = Math.ceil((double) otherPageTaskBundle.getTotal() / numberOfTasksPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(taskDtos, numberOfTasksPerPage, totalPages, currentPage, taskDtos.size(), otherPageTaskBundle.getTotal());
    }

    @Override
    public void createTask(TaskDto taskDto) {
        if (!isDuplicate(taskDto)) {
            retrieveActivityDefinitionDuration(taskDto);
            Task task = TaskDtoToTaskMap.map(taskDto);

            //Checking activity definition for enrollment
            task.setContext(createOrRetrieveEpisodeOfCare(taskDto));

            //authoredOn
            task.setAuthoredOn(java.sql.Date.valueOf(LocalDate.now()));

            fhirClient.create().resource(task).execute();
        } else {
            throw new DuplicateResourceFoundException("Duplicate task is already present.");
        }
    }

    @Override
    public void updateTask(String taskId, TaskDto taskDto) {
        Task task = TaskDtoToTaskMap.map(taskDto);
        task.setId(taskId);

        Bundle taskBundle = fhirClient.search().forResource(Task.class)
                .where(new TokenClientParam("_id").exactly().code(taskId))
                .returnBundle(Bundle.class)
                .execute();

        Task existingTask = (Task) taskBundle.getEntry().stream().findFirst().get().getResource();

        //Check activity definition for enrollment
        task.setContext(createOrRetrieveEpisodeOfCare(taskDto));

        //authoredOn
        task.setAuthoredOn(existingTask.getAuthoredOn());

        fhirClient.update().resource(task).execute();
    }

    @Override
    public PageDto<TaskDto> getUpcomingTasksByPractitioner(String practitioner, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfTasksPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Task.name());
        List<TaskDto> upcomingTasks = getUpcomingTasksByPractitioner(practitioner, searchKey, searchValue);

        return (PageDto<TaskDto>) PaginationUtil.applyPaginationForCustomArrayList(upcomingTasks, numberOfTasksPerPage, pageNumber, false);
    }

    private List<TaskDto> getUpcomingTasksByPractitioner(String practitioner, Optional<String> searchKey, Optional<String> searchValue) {
        List<PatientDto> patients = patientService.getPatientsByPractitioner(practitioner, Optional.empty(), Optional.empty());

        List<TaskDto> allTasks = patients.stream()
                .flatMap(it -> getTasksByPatient(it.getId()).stream())
                .distinct()
                .collect(toList());

        Map<String, List<TaskDto>> tasksGroupedByPatient = allTasks.stream().collect(groupingBy(x -> x.getBeneficiary().getReference()));

        List<TaskDto> finalList = new ArrayList<>();

        for (Map.Entry<String, List<TaskDto>> entry : tasksGroupedByPatient.entrySet()) {
            List<TaskDto> filtered = entry.getValue();
            Collections.sort(filtered);

            if (!filtered.isEmpty()) {
                TaskDto upcomingTask = filtered.get(0);
                finalList.add(upcomingTask);

                filtered.stream().skip(1).filter(task -> endDateAvailable(upcomingTask) && upcomingTask.getExecutionPeriod().getEnd().equals(task.getExecutionPeriod().getEnd())).forEach(finalList::add);
            }
        }

        //TODO: filter tasks by searchKey/value if present

        Collections.sort(finalList);
        return finalList;
    }

    private boolean endDateAvailable(TaskDto dto) {
        if (dto.getExecutionPeriod() != null && dto.getExecutionPeriod().getEnd() != null) {
            return true;
        }
        return false;
    }

    private void retrieveActivityDefinitionDuration(TaskDto taskDto) {
        LocalDate startDate = taskDto.getExecutionPeriod().getStart();
        LocalDate endDate = taskDto.getExecutionPeriod().getEnd();

        if (startDate == null) {
            startDate = LocalDate.now();
        }

        //if start date is available but endDate (due date) is not sent by UI
        if (endDate == null) {

            Reference reference = FhirDtoUtil.mapReferenceDtoToReference(taskDto.getDefinition());
            String activityDefinitionId = FhirDtoUtil.getIdFromReferenceDto(taskDto.getDefinition(), ResourceType.ActivityDefinition);

            ActivityDefinitionDto activityDefinition = activityDefinitionService.getActivityDefinitionById(activityDefinitionId);

            float duration = activityDefinition.getTiming().getDurationMax();
            taskDto.getExecutionPeriod().setEnd(startDate.plusDays((long) duration));
        }
    }

    private Reference createOrRetrieveEpisodeOfCare(TaskDto taskDto) {
        Reference contextReference = new Reference();

        if (taskDto.getDefinition().getDisplay().equalsIgnoreCase("Enrollment")) {

            Optional<EpisodeOfCareDto> episodeOfCare = retrieveEpisodeOfCare(taskDto);

            if (episodeOfCare.isPresent()) {
                EpisodeOfCareDto dto = episodeOfCare.get();
                contextReference.setReference(ResourceType.EpisodeOfCare + "/" + dto.getId());
            } else {
                EpisodeOfCare newEpisodeOfCare = createEpisodeOfCare(taskDto);
                MethodOutcome methodOutcome = fhirClient.create().resource(newEpisodeOfCare).execute();
                contextReference.setReference(ResourceType.EpisodeOfCare + "/" + methodOutcome.getId().getIdPart());
            }

            contextReference.setDisplay(createDisplayForEpisodeOfCare(taskDto));
        }

        return contextReference;
    }

    @Override
    public void deactivateTask(String taskId) {
        Task task = fhirClient.read().resource(Task.class).withId(taskId.trim()).execute();
        task.setStatus(Task.TaskStatus.CANCELLED);
        fhirClient.update().resource(task).execute();
    }

    @Override
    public TaskDto getTaskById(String taskId) {
        Bundle taskBundle = fhirClient.search().forResource(Task.class)
                .where(new TokenClientParam("_id").exactly().code(taskId))
                .include(Task.INCLUDE_CONTEXT)
                .returnBundle(Bundle.class)
                .execute();

        TaskDto taskDto = new TaskDto();

        taskBundle.getEntry().stream()
                .filter(taskResource -> taskResource.getResource().getResourceType().equals(ResourceType.Task))
                .findFirst().ifPresent(taskPresent -> {
            Task task = (Task) taskPresent.getResource();
            //Setting definition
            taskDto.setLogicalId(task.getIdElement().getIdPart());
            try {
                taskDto.setDefinition(FhirDtoUtil.convertReferenceToReferenceDto(task.getDefinitionReference()));
            } catch (FHIRException e) {
                e.printStackTrace();
            }

            if (task.hasPartOf()) {
                taskDto.setPartOf(FhirDtoUtil.convertReferenceToReferenceDto(task.getPartOf().stream().findFirst().get()));
            }

            //Setting Status, Intent, Priority
            taskDto.setStatus(FhirDtoUtil.convertDisplayCodeToValueSetDto(task.getStatus().toCode(), lookUpService.getTaskStatus()));
            taskDto.setIntent(FhirDtoUtil.convertDisplayCodeToValueSetDto(task.getIntent().toCode(), lookUpService.getRequestIntent()));
            taskDto.setPriority(FhirDtoUtil.convertDisplayCodeToValueSetDto(task.getPriority().toCode(), lookUpService.getRequestPriority()));

            if (task.hasDescription()) {
                taskDto.setDescription(task.getDescription());
            }

            taskDto.setBeneficiary(FhirDtoUtil.convertReferenceToReferenceDto(task.getFor()));

            taskDto.setAgent(FhirDtoUtil.convertReferenceToReferenceDto(task.getRequester().getAgent()));

            taskDto.setOrganization(FhirDtoUtil.convertReferenceToReferenceDto(task.getRequester().getOnBehalfOf()));

            taskDto.setOrganization(FhirDtoUtil.convertReferenceToReferenceDto(task.getRequester().getOnBehalfOf()));

            //Set Performer Type
            if (task.hasPerformerType()) {
                task.getPerformerType().stream().findFirst().ifPresent(performerType -> performerType.getCoding().stream().findFirst().ifPresent(coding -> {
                    taskDto.setPerformerType(FhirDtoUtil.convertCodeToValueSetDto(coding.getCode(), lookUpService.getTaskPerformerType()));
                }));
            }

            taskDto.setOwner(FhirDtoUtil.convertReferenceToReferenceDto(task.getOwner()));

            //Set Note
            task.getNote().stream().findFirst().ifPresent(note -> taskDto.setNote(note.getText()));

            if (task.hasLastModified()) {
                taskDto.setLastModified(DateUtil.convertDateToLocalDate(task.getLastModified()));
            }

            if (task.hasAuthoredOn()) {
                taskDto.setAuthoredOn(DateUtil.convertDateToLocalDate(task.getAuthoredOn()));
            }

            if (task.getExecutionPeriod() != null && !task.getExecutionPeriod().isEmpty()) {
                PeriodDto periodDto = new PeriodDto();
                periodDto.setStart((task.getExecutionPeriod().hasStart()) ? DateUtil.convertDateToLocalDate(task.getExecutionPeriod().getStart()) : null);
                periodDto.setEnd((task.getExecutionPeriod().hasEnd()) ? DateUtil.convertDateToLocalDate(task.getExecutionPeriod().getEnd()) : null);
                taskDto.setExecutionPeriod(periodDto);
            }

            taskDto.setContext(FhirDtoUtil.convertReferenceToReferenceDto(task.getContext()));
        });

        return taskDto;

    }

    private List<TaskDto> getTasksByPatient(String patient) {
        List<TaskDto> tasks = new ArrayList<>();

        Bundle bundle = getBundleForPatient(patient);

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> taskComponents = bundle.getEntry();

            if (taskComponents != null) {
                tasks = taskComponents.stream()
                        .map(it -> (Task) it.getResource())
                        .map(it -> TaskToTaskDtoMap.map(it, lookUpService.getTaskPerformerType()))
                        .collect(toList());
            }
        }

        return tasks;
    }

    private Bundle getBundleForPatient(String patient) {
        return fhirClient.search().forResource(Task.class)
                .where(new ReferenceClientParam("patient").hasId(ResourceType.Patient + "/" + patient))
                .returnBundle(Bundle.class).execute();
    }

    public List<ReferenceDto> getRelatedTasks(String patient, Optional<String> definition) {
        List<ReferenceDto> tasks = getBundleForPatient(patient).getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .map(resource -> FhirDtoUtil.mapTaskToReferenceDto((Task) resource))
                .collect(toList());
        if (definition.isPresent()) {
            return tasks.stream()
                    .filter(referenceDto -> referenceDto.getDisplay().equalsIgnoreCase(definition.get()))
                    .collect(toList());
        }
        return tasks;
    }

    private boolean isDuplicate(TaskDto taskDto) {
        Bundle taskForPatientbundle = fhirClient.search().forResource(Task.class)
                .where(new ReferenceClientParam("patient").hasId(taskDto.getBeneficiary().getReference()))
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> duplicateCheckList = new ArrayList<>();
        if (!taskForPatientbundle.isEmpty()) {
            duplicateCheckList = taskForPatientbundle.getEntry().stream().filter(taskResource -> {
                Task task = (Task) taskResource.getResource();
                try {
                    boolean defCheck = task.getDefinitionReference().getReference().equalsIgnoreCase(taskDto.getDefinition().getReference());
                    boolean isMainTask = Boolean.TRUE;
                    if( taskDto.getPartOf() != null  )
                        isMainTask = Boolean.FALSE;
                    return isMainTask ? defCheck: Boolean.FALSE;
                } catch (FHIRException e) {
                    throw new ResourceNotFoundException("No definition reference found in the Server");
                }
            }).collect(Collectors.toList());
        }
        return !duplicateCheckList.isEmpty();
    }

    private EpisodeOfCare createEpisodeOfCare(TaskDto taskDto) {
        EpisodeOfCare episodeOfCare = new EpisodeOfCare();
        episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);

        //Setting Episode of care type tp HACC
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(EpisodeofcareType.HACC.getSystem())
                .setDisplay(EpisodeofcareType.HACC.getDisplay())
                .setCode(EpisodeofcareType.HACC.toCode());
        List<CodeableConcept> codeableConcepts = new ArrayList<>();
        codeableConcepts.add(codeableConcept);

        episodeOfCare.setType(codeableConcepts);

        //patient
        episodeOfCare.setPatient(mapReferenceDtoToReference(taskDto.getBeneficiary()));

        //managing organization
        episodeOfCare.setManagingOrganization(mapReferenceDtoToReference(taskDto.getOrganization()));

        //start date
        episodeOfCare.getPeriod().setStart(java.sql.Date.valueOf(taskDto.getExecutionPeriod().getStart()));

        //careManager
        episodeOfCare.setCareManager(mapReferenceDtoToReference(taskDto.getAgent()));

        return episodeOfCare;
    }

    private Optional<EpisodeOfCareDto> retrieveEpisodeOfCare(TaskDto taskDto) {
        String patient = mapReferenceDtoToReference(taskDto.getBeneficiary()).getReference();

        List<EpisodeOfCareDto> episodeOfCareDtos = episodeOfCareService.getEpisodeOfCares(patient, Optional.of(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE.toCode()));

        return episodeOfCareDtos.stream().findFirst();
    }
}
