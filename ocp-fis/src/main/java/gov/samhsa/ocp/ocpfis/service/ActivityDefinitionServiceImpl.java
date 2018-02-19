package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirUtils;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.Timing;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class ActivityDefinitionServiceImpl implements ActivityDefinitionService {
    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public ActivityDefinitionServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }


    @Override
    public void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto, String organizationId) {
        if (!isDuplicate(activityDefinitionDto, organizationId)) {
            ActivityDefinition activityDefinition = new ActivityDefinition();
            activityDefinition.setName(activityDefinitionDto.getName());
            activityDefinition.setTitle(activityDefinitionDto.getTitle());
            activityDefinition.setDescription(activityDefinitionDto.getDescription());

            activityDefinition.setVersion(fisProperties.getActivityDefinition().getVersion());
            activityDefinition.setStatus(Enumerations.PublicationStatus.valueOf(activityDefinitionDto.getStatus().getCode().toUpperCase()));
            try {
                activityDefinition.setDate(FhirUtils.convertToDate(activityDefinitionDto.getDate()));
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
                    relatedArtifact.setType(RelatedArtifactType.valueOf(relatedArtifactDto.getCode().toUpperCase()));
                    relatedArtifacts.add(relatedArtifact);
                });
                activityDefinition.setRelatedArtifact(relatedArtifacts);
            }

            //Participant
            CodeableConcept actionParticipantRole = new CodeableConcept();
            actionParticipantRole.addCoding().setCode(activityDefinitionDto.getParticipant().getActionRoleCode())
                    .setDisplay(activityDefinitionDto.getParticipant().getActionRoleDisplay())
                    .setSystem(activityDefinitionDto.getParticipant().getActionRoleSystem());

            activityDefinition.addParticipant().setRole(actionParticipantRole).setType(ActivityDefinition.ActivityParticipantType.valueOf(activityDefinitionDto.getParticipant().getActionTypeCode().toUpperCase()));

            //Topic
            CodeableConcept topic = new CodeableConcept();
            topic.addCoding().setCode(activityDefinitionDto.getTopic().getCode()).setSystem(activityDefinitionDto.getTopic().getSystem())
                    .setDisplay(activityDefinitionDto.getTopic().getDisplay());
            activityDefinition.addTopic(topic);

            //Period
            if (activityDefinitionDto.getStatus().getCode().equalsIgnoreCase("active")) {
                if (activityDefinitionDto.getEffectivePeriod().getStart() != null) {
                    activityDefinition.getEffectivePeriod().setStart(java.sql.Date.valueOf(activityDefinitionDto.getEffectivePeriod().getStart()));
                } else {
                    activityDefinition.getEffectivePeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));
                }
            }

            if (activityDefinitionDto.getStatus().getCode().equalsIgnoreCase("expired")) {
                activityDefinition.getEffectivePeriod().setEnd(java.sql.Date.valueOf(LocalDate.now()));
            } else {
                activityDefinition.getEffectivePeriod().setEnd(java.sql.Date.valueOf(activityDefinitionDto.getEffectivePeriod().getEnd()));
            }

            //Timing
            Timing timing = new Timing();
            timing.getRepeat().setDurationMax(activityDefinitionDto.getTiming().getDurationMax());
            timing.getRepeat().setFrequency(activityDefinitionDto.getTiming().getFrequency());
            activityDefinition.setTiming(timing);

            fhirClient.create().resource(activityDefinition).execute();
        } else {
            throw new DuplicateResourceFoundException("Duplicate Activity Definition is already present.");
        }
    }

    private boolean isDuplicate(ActivityDefinitionDto activityDefinitionDto, String organizationid) {
        if (activityDefinitionDto.getStatus().getCode().equalsIgnoreCase(Enumerations.PublicationStatus.ACTIVE.toString())) {

            if (isDuplicateWithNamePublisherKindAndStatus(activityDefinitionDto, organizationid) || isDuplicateWithTitlePublisherKindAndStatus(activityDefinitionDto, organizationid)) {
                return true;
            } else {
                return false;
            }

        }
        return false;
    }

    private boolean isDuplicateWithNamePublisherKindAndStatus(ActivityDefinitionDto activityDefinitionDto, String organizationid) {
        Bundle duplicateCheckWithNamePublisherAndStatusBundle = (Bundle) fhirClient.search().forResource(ActivityDefinition.class)
                .where(new StringClientParam("publisher").matches().value("Organization/" + organizationid))
                .where(new TokenClientParam("status").exactly().code("active"))
                .where(new StringClientParam("name").matches().value(activityDefinitionDto.getName()))
                .returnBundle(Bundle.class)
                .execute();

        return hasSameKind(duplicateCheckWithNamePublisherAndStatusBundle, activityDefinitionDto);

    }

    private boolean isDuplicateWithTitlePublisherKindAndStatus(ActivityDefinitionDto activityDefinitionDto, String organizationid) {

        Bundle duplicateCheckWithTitlePublisherAndStatusBundle = (Bundle) fhirClient.search().forResource(ActivityDefinition.class)
                .where(new StringClientParam("publisher").matches().value("Organization/" + organizationid))
                .where(new TokenClientParam("status").exactly().code("active"))
                .where(new StringClientParam("title").matches().value(activityDefinitionDto.getTitle()))
                .returnBundle(Bundle.class)
                .execute();

        return hasSameKind(duplicateCheckWithTitlePublisherAndStatusBundle, activityDefinitionDto);
    }

    private boolean hasSameKind(Bundle bundle, ActivityDefinitionDto activityDefinitionDto) {
        List<Bundle.BundleEntryComponent> duplicateCheckList = new ArrayList<>();
        if (!bundle.isEmpty()) {
            duplicateCheckList = bundle.getEntry().stream().filter(activityDefinitionResource -> {
                ActivityDefinition activityDefinition = (ActivityDefinition) activityDefinitionResource.getResource();
                return activityDefinition.getKind().toCode().equalsIgnoreCase(activityDefinitionDto.getKind().getCode());
            }).collect(toList());
        }
        if (duplicateCheckList.isEmpty()) {
            return false;
        } else {
            return true;
        }

    }


}
