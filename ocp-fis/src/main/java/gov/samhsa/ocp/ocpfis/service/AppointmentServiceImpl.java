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
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.AppointmentToAppointmentDtoConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.CareTeamToCareTeamDtoConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AppointmentDtoToAppointmentConverter;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        final Appointment appointment = AppointmentDtoToAppointmentConverter.map(appointmentDto, true, Optional.empty());
        //Set created Date
        appointment.setCreated(new Date());
        //Validate
        FhirUtil.validateFhirResource(fhirValidator, appointment, Optional.empty(), ResourceType.Appointment.name(), "Create Appointment");
        //Create
        FhirUtil.createFhirResource(fhirClient, appointment, ResourceType.Appointment.name());

    }

    @Override
    public void updateAppointment(String appointmentId, AppointmentDto appointmentDto) {
        log.info("Updating appointmentId: " + appointmentId);
        //Validate if the request body has all the mandatory fields
        validateAppointDtoFromRequest(appointmentDto);
        //Map
        final Appointment appointment = AppointmentDtoToAppointmentConverter.map(appointmentDto, false, Optional.of(appointmentId));
        //Validate
        FhirUtil.validateFhirResource(fhirValidator, appointment, Optional.of(appointmentId), ResourceType.Appointment.name(), "Update Appointment");
        //Update
        FhirUtil.updateFhirResource(fhirClient, appointment, ResourceType.Appointment.name());
    }

    @Override
    public List<ParticipantReferenceDto> getAppointmentParticipants(String patientId, Optional<List<String>> roles, Optional<String> appointmentId) {
        List<ReferenceDto> participantsByRoles = new ArrayList<>();
        List<ParticipantReferenceDto> participantsSelected = new ArrayList<>();

        Bundle careTeamBundle = (Bundle) FhirUtil.searchNoCache(fhirClient, CareTeam.class, Optional.empty())
                .where(new ReferenceClientParam("patient").hasId(patientId.trim()))
                .include(CareTeam.INCLUDE_PARTICIPANT)
                .returnBundle(Bundle.class).execute();

        if (careTeamBundle != null) {
            List<Bundle.BundleEntryComponent> retrievedCareTeams = careTeamBundle.getEntry();

            if (retrievedCareTeams != null) {
                List<CareTeam> careTeams = retrievedCareTeams.stream()
                        .filter(bundle -> bundle.getResource().getResourceType().equals(ResourceType.CareTeam))
                        .map(careTeamMember -> (CareTeam) careTeamMember.getResource()).collect(toList());

                participantsByRoles = careTeams.stream()
                        .flatMap(it -> CareTeamToCareTeamDtoConverter.mapToParticipants(it, roles).stream()).collect(toList());
            }
        }

        //retrieve recipients by Id
        List<String> recipients = new ArrayList<>();

        if (appointmentId.isPresent()) {

            recipients = getParticipantsByPatientAndAppointmentId(patientId, appointmentId.get().trim());

        }

        for (ReferenceDto participant : participantsByRoles) {
            ParticipantReferenceDto participantReferenceDto = new ParticipantReferenceDto();
            participantReferenceDto.setReference(participant.getReference());
            participantReferenceDto.setDisplay(participant.getDisplay());

            if (recipients.contains(FhirDtoUtil.getIdFromParticipantReferenceDto(participant))) {
                participantReferenceDto.setSelected(true);
            }
            participantsSelected.add(participantReferenceDto);
        }

        return participantsSelected;
    }

    @Override
    public AppointmentDto getAppointmentById(String appointmentId) {
        log.info("Searching for appointmentId: " + appointmentId);

        Bundle appointmentBundle = (Bundle) FhirUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty())
                .where(new TokenClientParam("_id").exactly().code(appointmentId.trim()))
                .returnBundle(Bundle.class)
                .execute();

        if (appointmentBundle == null || appointmentBundle.getEntry().isEmpty()) {
            log.info("No appointment was found for the given appointmentId:" + appointmentId);
            throw new ResourceNotFoundException("No appointment was found for the given appointment ID:" + appointmentId);
        }

        log.info("FHIR appointment bundle retrieved from FHIR server successfully for appointment Id:" + appointmentId);

        Bundle.BundleEntryComponent retrievedAppointment = appointmentBundle.getEntry().get(0);
        return AppointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), Optional.empty());
    }

    @Override
    public PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList,
                                                   Optional<String> requesterReference,
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

        IQuery iQuery = FhirUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty());

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
                        (AppointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), requesterReference))).collect(toList());

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

    private List<String> getParticipantsByPatientAndAppointmentId(String patientId, String appointmentId) {
        List<String> participantIds = new ArrayList<>();

        Bundle bundle = (Bundle) FhirUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty())
                .where(new ReferenceClientParam("patient").hasId(patientId))
                .where(new TokenClientParam("_id").exactly().code(appointmentId))
                .include(Appointment.INCLUDE_PATIENT)
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> components = bundle.getEntry();
            participantIds = components.stream().map(Bundle.BundleEntryComponent::getResource).filter(resource -> resource instanceof Practitioner || resource instanceof Patient || resource instanceof Location || resource instanceof RelatedPerson || resource instanceof HealthcareService).map(resource -> resource.getIdElement().getIdPart()).collect(Collectors.toList());
        }

        return participantIds;
    }

    private Appointment setAppointmentStatusBasedOnParticipantActions(Appointment apt) {
        //Call this function only when Accept/Declining an appointment
        AppointmentDto aptDto = AppointmentToAppointmentDtoConverter.map(apt, Optional.empty());

        if (aptDto.getStatusCode().trim().equalsIgnoreCase("proposed")) {
            List<AppointmentParticipantDto> participantList = aptDto.getParticipant();
            if (participantList.stream().anyMatch(temp -> temp.getParticipationStatusCode().trim().equalsIgnoreCase("needs-action") &&
                    temp.getParticipantRequiredCode().trim().equalsIgnoreCase("required"))) {
                apt.setStatus(Appointment.AppointmentStatus.PENDING);
            }
        } else if (aptDto.getStatusCode().trim().equalsIgnoreCase("pending")) {
            List<AppointmentParticipantDto> participantList = aptDto.getParticipant();
            boolean allParticipantsHaveResponded = true;
            if (participantList.stream().anyMatch(temp -> temp.getParticipationStatusCode().trim().equalsIgnoreCase("needs-action") &&
                    temp.getParticipantRequiredCode().trim().equalsIgnoreCase("required"))) {
                allParticipantsHaveResponded = false;
            }
            if (allParticipantsHaveResponded) {
                apt.setStatus(Appointment.AppointmentStatus.BOOKED);
            }
        } //else do nothing
        return apt;
    }

}
