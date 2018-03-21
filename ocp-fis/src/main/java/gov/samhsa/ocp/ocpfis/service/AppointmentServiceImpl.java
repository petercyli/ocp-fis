package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.AppointmentToAppointmentDtoConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AppointmentDtoToAppointmentConverter;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

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
        String creatorName = appointmentDto.getCreatorName() != null ? appointmentDto.getCreatorName().trim() : "";
        log.info("Creating an appointment initiated by " + creatorName);

        //Validate if the request body has all the mandatory fields
        validateAppointDtoFromRequest(appointmentDto);
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
    public PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList,
                                                   Optional<String> patientId,
                                                   Optional<String> practitionerId,
                                                   Optional<String> searchKey,
                                                   Optional<String> searchValue,
                                                   Optional<Boolean> showPastAppointments,
                                                   Optional<Boolean> sortByStartTimeAsc,
                                                   Optional<Integer> pageNumber,
                                                   Optional<Integer> pageSize) {
        int numberOfAppointmentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Appointment.name());
        Bundle firstPageAppointmentBundle;
        Bundle otherPageAppointmentBundle;
        boolean firstPage = true;

        IQuery iQuery = fhirClient.search().forResource(Appointment.class);

        if (patientId.isPresent()) {
            log.info("Searching Appointments for patientId = " + patientId.get().trim());
            iQuery.where(new ReferenceClientParam("patient").hasId(patientId.get().trim()));
        }
        if (practitionerId.isPresent()) {
            log.info("Searching Appointments for practitionerId = " + practitionerId.get().trim());
            iQuery.where(new ReferenceClientParam("practitioner").hasId(practitionerId.get().trim()));
        }
        // Check if there are any additional search criteria
        iQuery = addStatusSearchConditions(iQuery, statusList);

        // Additional Search Key and Value
        iQuery = addSearchKeyValueConditions(iQuery, searchKey, searchValue);

        // Past appointments
        iQuery = addShowPastAppointmentConditions(iQuery, showPastAppointments);

        //Check sort order
        iQuery = addSortConditions(iQuery, sortByStartTimeAsc);

        firstPageAppointmentBundle = PaginationUtil.getSearchBundleFirstPage(iQuery, numberOfAppointmentsPerPage, Optional.empty());

        if (firstPageAppointmentBundle == null || firstPageAppointmentBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Appointments were found in the FHIR server.");
        }

        otherPageAppointmentBundle = firstPageAppointmentBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageAppointmentBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageAppointmentBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageAppointmentBundle, pageNumber.get(), numberOfAppointmentsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedAppointments = otherPageAppointmentBundle.getEntry();

        List<AppointmentDto> appointmentDtos = retrievedAppointments.stream()
                .filter(retrievedBundle -> retrievedBundle.getResource().getResourceType().equals(ResourceType.Appointment)).map(retrievedAppointment ->
                        (AppointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource()))).collect(toList());

        double totalPages = Math.ceil((double) otherPageAppointmentBundle.getTotal() / numberOfAppointmentsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(appointmentDtos, numberOfAppointmentsPerPage, totalPages, currentPage, appointmentDtos.size(), otherPageAppointmentBundle.getTotal());
    }

    @Override
    public void cancelAppointment(String appointmentId) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        //Update the resource
        FhirUtil.updateFhirResource(fhirClient, appointment, "Cancel Appointment");
    }


    private IQuery addStatusSearchConditions(IQuery searchQuery,
                                             Optional<List<String>> statusList) {
        // Check for appointment status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            log.info("Searching for appointments with the following specific status(es).");
            statusList.get().forEach(log::info);
            searchQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        } else {
            log.info("Searching for appointments with ALL statuses");
        }
        return searchQuery;
    }

    private IQuery addSearchKeyValueConditions(IQuery searchQuery,
                                               Optional<String> searchKey,
                                               Optional<String> searchValue) {
        // Check for bad requests
        if (searchKey.isPresent() && !SearchKeyEnum.AppointmentSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check if there are any additional search criteria
        if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.AppointmentSearchKey.LOGICALID.name())) {
            log.info("Searching Appointments for " + SearchKeyEnum.AppointmentSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            searchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else {
            log.info("Appointments - No additional search criteria entered.");
        }
        return searchQuery;
    }

    private IQuery addShowPastAppointmentConditions(IQuery searchQuery, Optional<Boolean> showPastAppointments) {
        // showPastAppointments?
        if (showPastAppointments.isPresent() && !showPastAppointments.get()) {
            log.info("Search results will NOT include past appointments.");
            searchQuery.where(Appointment.DATE.afterOrEquals().day(new Date()));
        } else if (showPastAppointments.isPresent() && showPastAppointments.get()) {
            log.info("Search results will include ONLY past appointments.");
            searchQuery.where(Appointment.DATE.before().day(new Date()));
        } else {
            log.info("Search results will include past AND upcoming appointments.");
        }
        return searchQuery;
    }

    private IQuery addSortConditions(IQuery searchQuery, Optional<Boolean> sortByStartTimeAsc) {
        // Currently, appointments can only be sorted by Appointment.DATE ("Appointment date/time.")
        if (!sortByStartTimeAsc.isPresent() || sortByStartTimeAsc.get()) {
            searchQuery.sort().ascending(Appointment.DATE);
        } else if (!sortByStartTimeAsc.get()) {
            searchQuery.sort().descending(Appointment.DATE);
        }
        return searchQuery;
    }

    private void validateAppointDtoFromRequest(AppointmentDto appointmentDto) {
        log.info("Validating appointment request DTO");
        String missingFields = "";

        if (appointmentDto == null) {
            throw new BadRequestException("AppointmentDto is NULL!!");
        }

        if (FhirUtil.isStringNullOrEmpty(appointmentDto.getTypeCode())) {
            missingFields = missingFields + "typeCode, ";
        }

        if (appointmentDto.getStart() == null) {
            missingFields = missingFields + "(appointment)start, ";
        }

        if (appointmentDto.getEnd() == null) {
            missingFields = missingFields + "(appointment)end, ";
        }

        if (appointmentDto.getParticipant().size() < 1) {
            missingFields = missingFields + "(appointment)participant(s), ";
        }

        if (missingFields.length() > 0) {
            missingFields = missingFields.substring(0, (missingFields.length() - 2)) + ".";
            throw new BadRequestException("The following required fields are missing in the appointmentDto: " + missingFields);
        }

        if (!DateUtil.isValidDateTimeRange(appointmentDto.getStart(), appointmentDto.getEnd(), false)) {
            throw new BadRequestException("Appointment EndDateTime is before StartDateTime");
        }
    }

}

