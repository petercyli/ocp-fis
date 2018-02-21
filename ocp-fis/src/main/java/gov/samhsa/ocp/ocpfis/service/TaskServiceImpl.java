package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ContextDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.codesystems.EpisodeofcareType;
import org.hl7.fhir.dstu3.model.codesystems.TaskStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        Task task=new Task();
        task.setDefinition(getReferenceValue(taskDto.getDefinition()));
        List<Reference> partOfReferences=new ArrayList<>();
        partOfReferences.add(getReferenceValue(taskDto.getPartOf()));
        task.setPartOf(partOfReferences);

        task.setStatus(Task.TaskStatus.valueOf(taskDto.getStatus().getCode().toUpperCase()));
        task.setIntent(Task.TaskIntent.valueOf(taskDto.getIntent().getCode().toUpperCase()));
         task.setPriority(Task.TaskPriority.valueOf(taskDto.getPriority().getCode().toUpperCase()));

        task.setDescription(taskDto.getDescription());

        task.setFor(getReferenceValue(taskDto.getBeneficiary()));

        //Checking activity definition for enrollment
        if(taskDto.getDefinition().getDisplay().equalsIgnoreCase("Enrollment")){
            EpisodeOfCare episodeOfCare=new EpisodeOfCare();
            episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);

            //Setting Episode of care type tp HACC
            CodeableConcept codeableConcept=new CodeableConcept();
            codeableConcept.addCoding().setSystem(EpisodeofcareType.HACC.getSystem());
            codeableConcept.addCoding().setDisplay(EpisodeofcareType.HACC.getDisplay());
            codeableConcept.addCoding().setCode(EpisodeofcareType.HACC.toCode());
            List<CodeableConcept> codeableConcepts=new ArrayList<>();
            codeableConcepts.add(codeableConcept);

            episodeOfCare.setType(codeableConcepts);

            ContextDto contextDto=taskDto.getContext();
            if(contextDto !=null){
                //Setting patient
                episodeOfCare.setPatient(getReferenceValue(contextDto.getPatient()));

                //Setting managing Organizaition
                episodeOfCare.setManagingOrganization(getReferenceValue(contextDto.getManagingOrganization()));

                //Setting Start Period
               episodeOfCare.getPeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));

               //Setting CareManager
                episodeOfCare.setCareManager(getReferenceValue(contextDto.getCareManager()));

            }
            MethodOutcome methodOutcome = fhirClient.create().resource(episodeOfCare).execute();
            Reference contextReference=new Reference();
            task.setContext(contextReference.setReference("EpisodeOfCare/"+methodOutcome.getId().getIdPart()));
        }

        //Set execution Period
        if(taskDto.getStatus().getCode().equalsIgnoreCase(TaskStatus.INPROGRESS.toCode()))
            task.getExecutionPeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));

        if(taskDto.getStatus().getCode().equalsIgnoreCase(TaskStatus.COMPLETED.toCode()))
            task.getExecutionPeriod().setEnd(java.sql.Date.valueOf(LocalDate.now()));

        //Set authoredOn
        task.setAuthoredOn(java.sql.Date.valueOf(LocalDate.now()));

        //Set last Modified
        task.setLastModified(java.sql.Date.valueOf(LocalDate.now()));

        //Set agent
        task.getRequester().setAgent(getReferenceValue(taskDto.getAgent()));

        //Set PerformerType
        if(taskDto.getPerformerType() !=null) {
            List<CodeableConcept> codeableConcepts=new ArrayList<>();
            CodeableConcept codeableConcept=new CodeableConcept();
            codeableConcept.addCoding().setCode(taskDto.getPerformerType().getCode());
            codeableConcept.addCoding().setSystem(taskDto.getPerformerType().getSystem());
            codeableConcept.addCoding().setDisplay(taskDto.getPerformerType().getDisplay());
            codeableConcepts.add(codeableConcept);
            task.setPerformerType(codeableConcepts);
        }

        task.setOwner(getReferenceValue(taskDto.getOwner()));

        Annotation annotation=new Annotation();
        annotation.setText(taskDto.getNote());
        List<Annotation> annotations=new ArrayList<>();
        annotations.add(annotation);
        task.setNote(annotations);

        fhirClient.create().resource(task).execute();
    }


    private Reference getReferenceValue(ReferenceDto referenceDto){
        Reference reference=new Reference();
        reference.setDisplay(referenceDto.getDisplay());
        reference.setReference(referenceDto.getReference());
        return reference;
    }
}
