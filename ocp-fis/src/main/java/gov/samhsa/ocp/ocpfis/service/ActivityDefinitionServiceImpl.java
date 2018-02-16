package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.BackboneElement;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.Timing;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ActivityDefinitionServiceImpl implements ActivityDefinitionService{
    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public ActivityDefinitionServiceImpl(ModelMapper modelMapper,IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper=modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }


    @Override
    public void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto) {
       ActivityDefinition activityDefinition=modelMapper.map(activityDefinitionDto,ActivityDefinition.class);
       activityDefinition.setStatus(Enumerations.PublicationStatus.valueOf(activityDefinitionDto.getStatus().getCode().toUpperCase()));
       activityDefinition.setDate(java.sql.Date.valueOf(activityDefinitionDto.getDate()));
       activityDefinition.setKind(ActivityDefinition.ActivityDefinitionKind.valueOf(activityDefinitionDto.getKind().getCode().toUpperCase()));

       //Relative Artifact
        List<RelatedArtifact> relatedArtifacts=new ArrayList<>();
        if (activityDefinitionDto.getRelatedArtifact()!=null && !activityDefinitionDto.getRelatedArtifact().isEmpty()){
            activityDefinitionDto.getRelatedArtifact().forEach(relatedArtifactDto -> {
                RelatedArtifact relatedArtifact = new RelatedArtifact();
                relatedArtifact.setType(RelatedArtifactType.valueOf(relatedArtifactDto.getCode().toUpperCase()));
                relatedArtifacts.add(relatedArtifact);
            });
            activityDefinition.setRelatedArtifact(relatedArtifacts);
        }

       //Participant
        CodeableConcept actionParticipantRole=new CodeableConcept();
        actionParticipantRole.addCoding().setCode(activityDefinitionDto.getParticipant().getActionRoleCode())
                .setDisplay(activityDefinitionDto.getParticipant().getActionRoleDisplay())
                .setSystem(activityDefinitionDto.getParticipant().getActionRoleSystem());

        activityDefinition.addParticipant().setRole(actionParticipantRole).setType(ActivityDefinition.ActivityParticipantType.valueOf(activityDefinitionDto.getParticipant().getActionTypeCode().toUpperCase()));

        //Topic
        CodeableConcept topic=new CodeableConcept();
        topic.addCoding().setCode(activityDefinitionDto.getTopic().getCode()).setSystem(activityDefinitionDto.getTopic().getSystem())
                .setDisplay(activityDefinitionDto.getTopic().getDisplay());
        activityDefinition.addTopic(topic);

        //Period
        if(activityDefinitionDto.getStatus().getCode().equalsIgnoreCase("active")){
            if(activityDefinitionDto.getEffectivePeriod().getStart()!=null){
                activityDefinition.getEffectivePeriod().setStart(java.sql.Date.valueOf(activityDefinitionDto.getEffectivePeriod().getStart()));
            }else{
            activityDefinition.getEffectivePeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));
        }}

        if(activityDefinitionDto.getStatus().getCode().equalsIgnoreCase("expired")){
            activityDefinition.getEffectivePeriod().setEnd(java.sql.Date.valueOf(LocalDate.now()));
        }else {
            activityDefinition.getEffectivePeriod().setEnd(java.sql.Date.valueOf(activityDefinitionDto.getEffectivePeriod().getEnd()));
        }

        //Timing
        Timing timing=new Timing();
        timing.getRepeat().setDurationMax(activityDefinitionDto.getTiming().getDurationMax());
        timing.getRepeat().setFrequency(activityDefinitionDto.getTiming().getFrequency());
        activityDefinition.setTiming(timing);

        activityDefinition.setPublisher(activityDefinitionDto.getPublisherReference());

        fhirClient.create().resource(activityDefinition).execute();
    }
}
