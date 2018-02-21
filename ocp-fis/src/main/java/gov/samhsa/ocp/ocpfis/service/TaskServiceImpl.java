package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ContextDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.codesystems.EpisodeofcareType;
import org.hl7.fhir.dstu3.model.codesystems.TaskStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService{

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public TaskServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }


    @Override
    public void createTask(TaskDto taskDto) {
        if(!isDuplicate(taskDto)) {
            Task task = setTaskDtoToTask(taskDto);

            //Checking activity definition for enrollment and creating context with Episode of care
            if (taskDto.getDefinition().getDisplay().equalsIgnoreCase("Enrollment")) {
                EpisodeOfCare episodeOfCare=createEpisodeOfCare(taskDto);
                MethodOutcome methodOutcome = fhirClient.create().resource(episodeOfCare).execute();
                Reference contextReference = new Reference();
                task.setContext(contextReference.setReference("EpisodeOfCare/" + methodOutcome.getId().getIdPart()));
            }

            //Set authoredOn
            task.setAuthoredOn(java.sql.Date.valueOf(LocalDate.now()));

            fhirClient.create().resource(task).execute();
        }else{
            throw new DuplicateResourceFoundException("Duplicate task is already present.");
        }
    }

    @Override
    public void updateTask(String taskId, TaskDto taskDto) {
        Task task=setTaskDtoToTask(taskDto);
        task.setId(taskId);

        if (taskDto.getDefinition().getDisplay().equalsIgnoreCase("Enrollment")) {
            Bundle taskBundle = (Bundle) fhirClient.search().forResource(Task.class)
                    .where(new TokenClientParam("_id").exactly().code(taskId))
                    .returnBundle(Bundle.class)
                    .execute();

            Task existingTask = (Task) taskBundle.getEntry().get(0).getResource();
            if(!existingTask.hasContext()){
                EpisodeOfCare episodeOfCare=createEpisodeOfCare(taskDto);
                MethodOutcome methodOutcome = fhirClient.create().resource(episodeOfCare).execute();
                Reference contextReference = new Reference();
                task.setContext(contextReference.setReference("EpisodeOfCare/" + methodOutcome.getId().getIdPart()));
                }
            else {
                EpisodeOfCare episodeOfCare = createEpisodeOfCare(taskDto);
                episodeOfCare.setId(existingTask.getContext().getReference().split("/")[1]);
                MethodOutcome methodOutcome=fhirClient.update().resource(episodeOfCare).execute();
                Reference contextReference = new Reference();
                task.setContext(contextReference.setReference("EpisodeOfCare/" + methodOutcome.getId().getIdPart()));
            }
        }
        fhirClient.update().resource(task).execute();
    }


    private boolean isDuplicate(TaskDto taskDto){
        Bundle taskForPatientbundle= (Bundle) fhirClient.search().forResource(Task.class)
                .where(new ReferenceClientParam("patient").hasId(taskDto.getBeneficiary().getReference()))
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> duplicateCheckList=new ArrayList<>();
        if(!taskForPatientbundle.isEmpty()){
            duplicateCheckList= taskForPatientbundle.getEntry().stream().filter(taskResource->{
                Task task= (Task) taskResource.getResource();
                try {
                    return task.getDefinitionReference().getReference().equalsIgnoreCase(taskDto.getDefinition().getReference());

                } catch (FHIRException e) {
                   throw new ResourceNotFoundException("No definition reference found in the Server");
                }
            }).collect(Collectors.toList());
        }
        if(duplicateCheckList.isEmpty()){
            return false;
        }else{
            return true;
        }
    }


    private Reference getReferenceValue(ReferenceDto referenceDto){
        Reference reference=new Reference();
        reference.setDisplay(referenceDto.getDisplay());
        reference.setReference(referenceDto.getReference());
        return reference;
    }

    private Task setTaskDtoToTask(TaskDto taskDto){
        Task task=new Task();
        task.setDefinition(getReferenceValue(taskDto.getDefinition()));
        List<Reference> partOfReferences = new ArrayList<>();
        partOfReferences.add(getReferenceValue(taskDto.getPartOf()));
        task.setPartOf(partOfReferences);

        task.setStatus(Task.TaskStatus.valueOf(taskDto.getStatus().getCode().toUpperCase()));
        task.setIntent(Task.TaskIntent.valueOf(taskDto.getIntent().getCode().toUpperCase()));
        task.setPriority(Task.TaskPriority.valueOf(taskDto.getPriority().getCode().toUpperCase()));

        task.setDescription(taskDto.getDescription());

        task.setFor(getReferenceValue(taskDto.getBeneficiary()));


        //Set execution Period
        if (taskDto.getStatus().getCode().equalsIgnoreCase(TaskStatus.INPROGRESS.toCode()))
            task.getExecutionPeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));

        if (taskDto.getStatus().getCode().equalsIgnoreCase(TaskStatus.COMPLETED.toCode()))
            task.getExecutionPeriod().setEnd(java.sql.Date.valueOf(LocalDate.now()));

        //Set agent
        task.getRequester().setAgent(getReferenceValue(taskDto.getAgent()));

        //Set PerformerType
        if (taskDto.getPerformerType() != null) {
            List<CodeableConcept> codeableConcepts = new ArrayList<>();
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.addCoding().setCode(taskDto.getPerformerType().getCode());
            codeableConcept.addCoding().setSystem(taskDto.getPerformerType().getSystem());
            codeableConcept.addCoding().setDisplay(taskDto.getPerformerType().getDisplay());
            codeableConcepts.add(codeableConcept);
            task.setPerformerType(codeableConcepts);
        }

        //Set last Modified
        task.setLastModified(java.sql.Date.valueOf(LocalDate.now()));

        task.setOwner(getReferenceValue(taskDto.getOwner()));

        Annotation annotation = new Annotation();
        annotation.setText(taskDto.getNote());
        List<Annotation> annotations = new ArrayList<>();
        annotations.add(annotation);
        task.setNote(annotations);

        return task;
    }

    private EpisodeOfCare createEpisodeOfCare(TaskDto taskDto){
        EpisodeOfCare episodeOfCare = new EpisodeOfCare();
        episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);

        //Setting Episode of care type tp HACC
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding().setSystem(EpisodeofcareType.HACC.getSystem());
        codeableConcept.addCoding().setDisplay(EpisodeofcareType.HACC.getDisplay());
        codeableConcept.addCoding().setCode(EpisodeofcareType.HACC.toCode());
        List<CodeableConcept> codeableConcepts = new ArrayList<>();
        codeableConcepts.add(codeableConcept);

        episodeOfCare.setType(codeableConcepts);

        ContextDto contextDto = taskDto.getContext();
        if (contextDto != null) {
            //Setting patient
            episodeOfCare.setPatient(getReferenceValue(contextDto.getPatient()));

            //Setting managing Organizaition
            episodeOfCare.setManagingOrganization(getReferenceValue(contextDto.getManagingOrganization()));

            //Setting Start Period
            episodeOfCare.getPeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));

            //Setting CareManager
            episodeOfCare.setCareManager(getReferenceValue(contextDto.getCareManager()));

        }
        return episodeOfCare;

    }
}
