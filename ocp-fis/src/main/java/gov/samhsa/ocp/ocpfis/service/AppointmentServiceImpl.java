package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AppointmentDtoToAppointmentConverter;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.ResourceType;
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
        //Map
        final Appointment appointment = AppointmentDtoToAppointmentConverter.map(appointmentDto, true);
        //Set created Date
        appointment.setCreated(new Date());
        //Validate
        FhirUtil.validateFhirResource(fhirValidator, appointment, Optional.empty(), ResourceType.Appointment.name(), "Create Appointment");
        //Create
        FhirUtil.createFhirResource(fhirClient, appointment, ResourceType.Appointment.name());

    }
}

