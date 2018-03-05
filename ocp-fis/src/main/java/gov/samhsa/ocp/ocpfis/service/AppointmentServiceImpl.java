package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AppointmentToAppointmentDtoConverter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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
        Appointment appointment = AppointmentToAppointmentDtoConverter.map(appointmentDto, true);
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
}

