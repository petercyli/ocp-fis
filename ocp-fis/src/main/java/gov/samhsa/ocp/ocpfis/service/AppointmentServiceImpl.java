package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AppointmentDtoToAppointmentConverter;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private static final String PATIENT_ACTOR_REFERENCE = "Patient";
    private static final String DATE_TIME_FORMATTER_PATTERN_DATE = "MM/dd/yyyy";
    private static final String DATE_TIME_FORMATTER_PATTERN_TIME = "HH:mm";

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
        String creatorName = appointmentDto.getCreatorName() != null ? appointmentDto.getCreatorName().trim() : "";
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

    @Override
    public PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfAppointmentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Appointment.name());
        IQuery iQuery = fhirClient.search().forResource(Appointment.class);

        // Check if there are any additional search criteria
        iQuery = addAdditionalSearchConditions(iQuery, searchKey, searchValue);

        Bundle firstPageAppointmentBundle;
        Bundle otherPageAppointmentBundle;
        boolean firstPage = true;

        firstPageAppointmentBundle = (Bundle) iQuery
                .count(numberOfAppointmentsPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageAppointmentBundle == null || firstPageAppointmentBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Appointments were found in the FHIR server.");
        }

        otherPageAppointmentBundle = firstPageAppointmentBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageAppointmentBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageAppointmentBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageAppointmentBundle, pageNumber.get(), numberOfAppointmentsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedAppointments = otherPageAppointmentBundle.getEntry();


        List<AppointmentDto> appointmentDtos = retrievedAppointments.stream().filter(retrivedBundle -> retrivedBundle.getResource().getResourceType().equals(ResourceType.Appointment)).map(retrievedAppointment -> {

            Appointment appointment = (Appointment) retrievedAppointment.getResource();

            AppointmentDto appointmentDto = new AppointmentDto();

            appointmentDto.setLogicalId(appointment.getIdElement().getIdPart());

            if (appointment.hasStatus()) {
                appointmentDto.setStatusCode(appointment.getStatus().toCode());
            }

            if (appointment.hasAppointmentType()) {
                ValueSetDto type = FhirDtoUtil.convertCodeableConceptToValueSetDto(appointment.getAppointmentType());
                appointmentDto.setTypeCode(type.getCode());
            }

            if (appointment.hasDescription()) {
                appointmentDto.setDescription(appointment.getDescription());
            }

            if (appointment.hasParticipant()) {
                List<AppointmentParticipantDto> participantDtos = FhirDtoUtil.convertAppointmentParticipantListToAppointmentParticipantDtoList(appointment.getParticipant());
                appointmentDto.setParticipant(participantDtos);
            }

            if (!appointmentDto.getParticipant().isEmpty()) {
                List<String> actorNames = appointmentDto.getParticipant().stream()
                        .filter(participant -> participant.getActorReference().toUpperCase().contains(PATIENT_ACTOR_REFERENCE.toUpperCase()))
                        .map(AppointmentParticipantDto::getActorName)
                        .collect(toList());
                if (!actorNames.isEmpty())
                appointmentDto.setDisplayPatientName(actorNames.get(0));
            }

            String duration = "";

            if (appointment.hasStart()) {
                appointmentDto.setStart(DateUtil.convertDateToLocalDateTime(appointment.getStart()));
                DateTimeFormatter startFormatterDate = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN_DATE);
                String formattedDate = appointmentDto.getStart().format(startFormatterDate); // "MM/dd/yyyy HH:mm"
                appointmentDto.setDisplayDate(formattedDate);

                DateTimeFormatter startFormatterTime = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN_TIME);
                String formattedStartTime = appointmentDto.getStart().format(startFormatterTime); // "MM/dd/yyyy HH:mm"

                duration = duration + formattedStartTime;
            }

            if (appointment.hasEnd()) {
                appointmentDto.setEnd(DateUtil.convertDateToLocalDateTime(appointment.getEnd()));
                DateTimeFormatter endFormatterTime = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN_TIME);
                String formattedEndTime = appointmentDto.getEnd().format(endFormatterTime); // "HH:mm"
                duration = duration + " - " + formattedEndTime;
            }
            appointmentDto.setDisplayDuration(duration);

            return appointmentDto;

        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageAppointmentBundle.getTotal() / numberOfAppointmentsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(appointmentDtos, numberOfAppointmentsPerPage, totalPages, currentPage, appointmentDtos.size(), otherPageAppointmentBundle.getTotal());
    }

    private IQuery addAdditionalSearchConditions(IQuery searchQuery, Optional<String> searchKey, Optional<String> searchValue) {
        if (searchKey.isPresent() && !SearchKeyEnum.AppointmentSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check if there are any additional search criteria
        if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.AppointmentSearchKey.PATIENTID.name())) {
            log.info("Searching for " + SearchKeyEnum.AppointmentSearchKey.PATIENTID.name() + " = " + searchValue.get().trim());
            searchQuery.where(new ReferenceClientParam("patient").hasId(searchValue.get().trim()));

        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.AppointmentSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.AppointmentSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            searchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
            searchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }
        return searchQuery;
    }

}

