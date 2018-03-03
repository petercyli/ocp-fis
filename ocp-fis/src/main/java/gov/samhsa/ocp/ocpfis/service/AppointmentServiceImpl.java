package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    @Autowired
    public AppointmentServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
    }

    @Override
    public void createAppointment(AppointmentDto appointmentDto) {
        String creatorName = appointmentDto.getCreatorName() != null? appointmentDto.getCreatorName().trim() : "";
        log.info("Creating an appointment initiated by " + creatorName);
        Appointment appointment =  convertDtoToFhirModel(appointmentDto, true);
        //Set created Date
        appointment.setCreated(new Date());

        // Validate the resource
        validateAppointmentResource(appointment, Optional.empty(), "Create Appointment : ");

        try {
            MethodOutcome serverResponse = fhirClient.create().resource(appointment).execute();
            log.info("Created a new appointment :" + serverResponse.getId().getIdPart());
        }
        catch (BaseServerResponseException e) {
            log.error("Could NOT create appointment");
            throw new FHIRClientException("FHIR Client returned with an error while creating the appointment:" + e.getMessage());
        }
    }

    private void validateAppointmentResource(Appointment appointment, Optional<String> appointmentId, String createOrUpdateAppointment) {
        ValidationResult validationResult = fhirValidator.validateWithResult(appointment);

        if (appointmentId.isPresent()) {
            log.info(createOrUpdateAppointment + "Validation successful? " + validationResult.isSuccessful() + " for Appointment ID: " + appointmentId);
        } else {
            log.info(createOrUpdateAppointment + "Validation successful? " + validationResult.isSuccessful());
        }

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("Appointment Validation was not successful" + validationResult.getMessages());
        }
    }

    private boolean isStringNotNullAndNotEmpty(String givenString){
        return givenString != null && !givenString.trim().isEmpty();
    }

    private Appointment convertDtoToFhirModel(AppointmentDto appointmentDto, boolean isCreate){
        try{
            Appointment appointment = new Appointment();

            //id
            if(isStringNotNullAndNotEmpty(appointmentDto.getLogicalId())){
                appointment.setId(appointmentDto.getLogicalId().trim());
            }

            //Status
            if(isCreate){
                appointment.setStatus(Appointment.AppointmentStatus.fromCode("proposed"));
            } else if(isStringNotNullAndNotEmpty(appointmentDto.getStatusCode())){
                log.info("About to set Appointment Status-- Check if system, display is SET!!");
                Appointment.AppointmentStatus status = Appointment.AppointmentStatus.fromCode(appointmentDto.getStatusCode().trim());
                appointment.setStatus(status);
            }

            //Type
            if(isStringNotNullAndNotEmpty(appointmentDto.getTypeCode())){
                CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding(appointmentDto.getTypeCode(), appointmentDto.getTypeDisplay(), null));
                appointment.setAppointmentType(codeableConcept);
            }

            //Desc
            if(isStringNotNullAndNotEmpty(appointmentDto.getDescription())){
                appointment.setDescription(appointmentDto.getDescription().trim());
            }

            //Start and End Dates
            appointment.setStart(DateUtil.convertLocalDateTimeToDate(appointmentDto.getStart()));
            appointment.setEnd(DateUtil.convertLocalDateTimeToDate(appointmentDto.getEnd()));

            //Caution: DO NOT set created time here
            //TBD: Slot and incoming reference

            //Participants
            if(appointmentDto.getParticipant() == null || appointmentDto.getParticipant().isEmpty()){
                throw new BadRequestException("An appointment cannot be without its participant(s).");
            } else{
                List<Appointment.AppointmentParticipantComponent> participantList = new ArrayList<>();
                for(AppointmentParticipantDto participant : appointmentDto.getParticipant() ){
                    Appointment.AppointmentParticipantComponent participantModel = new Appointment.AppointmentParticipantComponent();

                    //Participation Type
                    if(isStringNotNullAndNotEmpty(participant.getParticipationTypeCode())){
                        CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding(participant.getParticipationTypeCode(), participant.getParticipationTypeDisplay(), null));
                        participantModel.setType(Collections.singletonList(codeableConcept));
                    } else if(isCreate){
                        //By default, add participants as "attender"
                        CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding("ATND", "attender", "http://hl7.org/fhir/v3/ParticipationType"));
                        participantModel.setType(Collections.singletonList(codeableConcept));
                    }

                    //Participation Status
                    if(isStringNotNullAndNotEmpty(participant.getParticipationStatusCode())){
                        Appointment.ParticipationStatus status = Appointment.ParticipationStatus.fromCode(participant.getParticipationStatusCode().trim());
                        participantModel.setStatus(status);
                    } else if(isCreate){
                        participantModel.setStatus(Appointment.ParticipationStatus.fromCode("tentative"));
                    }

                    //Actor
                    if(isStringNotNullAndNotEmpty(participant.getActorReference())){
                        Reference ref = new Reference(participant.getActorReference().trim());
                        if(isStringNotNullAndNotEmpty(participant.getActorName())){
                            ref.setDisplay(participant.getActorName().trim());
                        }
                        participantModel.setActor(ref);


                        //Participant Required
                        if(isStringNotNullAndNotEmpty(participant.getParticipantRequiredCode())){
                            Appointment.ParticipantRequired required = Appointment.ParticipantRequired.fromCode(participant.getParticipantRequiredCode().trim());
                            participantModel.setRequired(required);
                        } else {
                            participantModel.setRequired(getRequiredBasedOnParticipantReference(participant.getActorReference()));
                        }
                    }
                    participantList.add(participantModel);
                }

                //Add creator reference during appointment creation
                if(isCreate && isStringNotNullAndNotEmpty(appointmentDto.getCreatorReference())){
                    Appointment.AppointmentParticipantComponent creatorParticipantModel = new Appointment.AppointmentParticipantComponent();

                    //Participation Type
                    CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding("AUT", "author (originator)","http://hl7.org/fhir/v3/ParticipationType"));
                    creatorParticipantModel.setType(Collections.singletonList(codeableConcept));

                    //Participation Status
                    creatorParticipantModel.setStatus(Appointment.ParticipationStatus.fromCode("accepted"));

                    //Participant Required
                    creatorParticipantModel.setRequired(getRequiredBasedOnParticipantReference(appointmentDto.getCreatorReference()));

                    //Actor
                    Reference creatorRef = new Reference(appointmentDto.getCreatorReference().trim());
                    if(isStringNotNullAndNotEmpty(appointmentDto.getCreatorName())){
                        creatorRef.setDisplay(appointmentDto.getCreatorName().trim());
                    }
                    creatorParticipantModel.setActor(creatorRef);

                    participantList.add(creatorParticipantModel);
                }

                appointment.setParticipant(participantList);
            }
            return appointment;
        }
        catch (FHIRException e) {
           log.error("Unable to convert from the given code to valueSet");
           throw new BadRequestException("Invalid values in the request Dto ", e);
        }
    }

    private Appointment.ParticipantRequired getRequiredBasedOnParticipantReference(String reference) throws FHIRException {
        String resourceType = reference.trim().split("/")[0];
        List<String> requiredResources = Arrays.asList(ResourceType.Practitioner.name(),ResourceType.Patient.name(), ResourceType.RelatedPerson.name());
        List<String> infoOnlyResources = Arrays.asList(ResourceType.HealthcareService.name(),ResourceType.Location.name());

        if(requiredResources.contains(resourceType.toUpperCase())){
            return Appointment.ParticipantRequired.fromCode("required");
        } else if (infoOnlyResources.contains(resourceType.toUpperCase())){
            return Appointment.ParticipantRequired.fromCode("information-only");
        }
        return null;
    }
}

