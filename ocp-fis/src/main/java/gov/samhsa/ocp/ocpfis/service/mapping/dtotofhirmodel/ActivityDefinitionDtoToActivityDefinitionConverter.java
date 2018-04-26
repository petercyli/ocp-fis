package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.Timing;
import org.hl7.fhir.exceptions.FHIRException;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ActivityDefinitionDtoToActivityDefinitionConverter {

    //moved the existing code from ActivityDefinitionServiceImpl.createActivityDefinition()
    //so that it can be resued for update.
    public static ActivityDefinition map(ActivityDefinitionDto activityDefinitionDto, String organizationId, String version) {

        ActivityDefinition activityDefinition = new ActivityDefinition();
        activityDefinition.setName(activityDefinitionDto.getName());
        activityDefinition.setTitle(activityDefinitionDto.getTitle());
        activityDefinition.setDescription(activityDefinitionDto.getDescription());

        activityDefinition.setVersion(version);
        activityDefinition.setStatus(Enumerations.PublicationStatus.valueOf(activityDefinitionDto.getStatus().getCode().toUpperCase()));
        try {
            activityDefinition.setDate(DateUtil.convertStringToDate(activityDefinitionDto.getDate()));
        } catch (ParseException e) {
            throw new BadRequestException("Invalid date was given.");
        }
        activityDefinition.setKind(ActivityDefinition.ActivityDefinitionKind.valueOf(activityDefinitionDto.getKind().getCode().toUpperCase()));
        activityDefinition.setPublisher("Organization/" + organizationId);

        //Relative Artifact
        List<RelatedArtifact> relatedArtifacts = new ArrayList<>();
        if (activityDefinitionDto.getRelatedArtifact() != null && !activityDefinitionDto.getRelatedArtifact().isEmpty()) {
            activityDefinitionDto.getRelatedArtifact().forEach(relatedArtifactDto -> {
                RelatedArtifact relatedArtifact = new RelatedArtifact();
                try {
                    relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.fromCode(relatedArtifactDto.getType()));
                } catch (FHIRException e) {
                    throw new BadRequestException("Invalid related artifact type was given.");
                }

                relatedArtifact.setDisplay(relatedArtifactDto.getDisplay());
                relatedArtifact.setCitation(relatedArtifactDto.getCitation());
                relatedArtifact.setUrl(relatedArtifactDto.getUrl());
                relatedArtifacts.add(relatedArtifact);
            });
            activityDefinition.setRelatedArtifact(relatedArtifacts);
        }

        //Participant
        CodeableConcept actionParticipantRole = new CodeableConcept();
        actionParticipantRole.addCoding().setCode(activityDefinitionDto.getActionParticipantRole().getCode())
                .setDisplay(activityDefinitionDto.getActionParticipantRole().getDisplay())
                .setSystem(activityDefinitionDto.getActionParticipantRole().getSystem());

        String participantType = activityDefinitionDto.getActionParticipantType().getCode();

        if(participantType != null) {
            participantType = participantType.replace("-", "").toUpperCase();
        }

        activityDefinition.addParticipant().setRole(actionParticipantRole).setType(ActivityDefinition.ActivityParticipantType.valueOf(participantType));

        //Topic
        CodeableConcept topic = new CodeableConcept();
        if (activityDefinitionDto.getTopic() != null) {
            topic.addCoding().setCode((activityDefinitionDto.getTopic().getCode() != null) ? activityDefinitionDto.getTopic().getCode() : null)
                    .setSystem((activityDefinitionDto.getTopic().getSystem() != null) ? activityDefinitionDto.getTopic().getSystem() : null)
                    .setDisplay((activityDefinitionDto.getTopic().getDisplay() != null) ? activityDefinitionDto.getTopic().getDisplay() : null);
            activityDefinition.addTopic(topic);
        }


        //Period
        if (activityDefinitionDto.getStatus().getCode().equalsIgnoreCase("active")) {
            if (activityDefinitionDto.getEffectivePeriod() != null) {
                if (activityDefinitionDto.getEffectivePeriod().getStart() != null)
                    activityDefinition.getEffectivePeriod().setStart((java.sql.Date.valueOf(activityDefinitionDto.getEffectivePeriod().getStart())));
            } else {
                activityDefinition.getEffectivePeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));
            }
        }
        if (activityDefinitionDto.getStatus().getCode().equalsIgnoreCase("retired")) {
            activityDefinition.getEffectivePeriod().setEnd(java.sql.Date.valueOf(LocalDate.now()));
        }


        //Timing
        Timing timing = new Timing();
        timing.getRepeat().setDurationMax(activityDefinitionDto.getTiming().getDurationMax());
        timing.getRepeat().setFrequency(activityDefinitionDto.getTiming().getFrequency());
        activityDefinition.setTiming(timing);

        return activityDefinition;
    }
}
