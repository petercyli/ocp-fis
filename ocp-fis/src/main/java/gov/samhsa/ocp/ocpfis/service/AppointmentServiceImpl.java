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
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.PreconditionFailedException;
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
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    private final PatientService patientService;

    private final String ACCEPTED_PARTICIPATION_STATUS = "accepted";
    private final String DECLINED_PARTICIPATION_STATUS = "declined";
    private final String TENTATIVE_PARTICIPATION_STATUS = "tentative";
    private final String NEEDS_ACTION_PARTICIPATION_STATUS = "needs-action";
    private final String PENDING_APPOINTMENT_STATUS = "pending";
    private final String REQUIRED = "required";

    @Autowired
    public AppointmentServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, PatientService patientService) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.patientService = patientService;
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
                                                   Optional<String> filterDateOption,
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
        iQuery = addShowPastAppointmentConditions(iQuery, showPastAppointments, filterDateOption);

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
    public List<AppointmentDto> getAppointmentsWithNoPagination(Optional<List<String>> statusList, Optional<String> patientId, Optional<String> practitionerId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<Boolean> sortByStartTimeAsc) {
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
        iQuery = addShowPastAppointmentConditions(iQuery, showPastAppointments, Optional.empty());

        //Check sort order
        iQuery = addSortConditions(iQuery, sortByStartTimeAsc);

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        List<Bundle.BundleEntryComponent> retrievedAppointments = FhirUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);

        return retrievedAppointments.stream()
                .filter(retrievedBundle -> retrievedBundle.getResource().getResourceType().equals(ResourceType.Appointment)).map(retrievedAppointment ->
                        (AppointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), Optional.empty()))).collect(toList());
    }

    @Override
    public PageDto<AppointmentDto> getAppointmentsByPractitionerAndAssignedCareTeamPatients(String practitionerId, Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfAppointmentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Appointment.name());
        Bundle firstPageAppointmentBundle;
        Bundle otherPageAppointmentBundle;
        boolean firstPage = true;

        IQuery iQuery = FhirUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty());

        log.info("Searching Appointments for practitionerId = " + practitionerId.trim());
        iQuery.where(new ReferenceClientParam("practitioner").hasId(practitionerId.trim()));

        List<PatientDto> assignedPatients = patientService.getPatientsByPractitioner(Optional.of(practitionerId.trim()), Optional.empty(), Optional.empty());
        Set<String> patientIds = getPatientIdSet(assignedPatients);

        if(patientIds!= null && !patientIds.isEmpty()){
            log.info("Searching for Patients assigned/belonging to Practitioner Id :" + practitionerId);
            log.info("Number of Patients assigned/belonging to Practitioner Id (" + practitionerId + ") = " + patientIds.size());
            iQuery.where(new ReferenceClientParam("patient").hasAnyOfIds(patientIds));
        } else {
            log.info("No Patient found assigned/belonging to Practitioner Id :" + practitionerId);
        }

        // Check if there are any additional search criteria
        iQuery = addStatusSearchConditions(iQuery, statusList);

        // Additional Search Key and Value
        iQuery = addSearchKeyValueConditions(iQuery, searchKey, searchValue);

        // Past appointments
        iQuery = addShowPastAppointmentConditions(iQuery, showPastAppointments, filterDateOption);

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
        //Cancel
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        //Update the resource
        FhirUtil.updateFhirResource(fhirClient, appointment, "Cancel Appointment");
    }

    @Override
    public void acceptAppointment(String appointmentId, String actorReference) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Accept
        appointment = setParticipantAction(appointment, actorReference, "ACCEPT");
        appointment = setAppointmentStatusBasedOnParticipantActions(appointment);
        //Update the resource
        FhirUtil.updateFhirResource(fhirClient, appointment, "Accept Appointment");
    }

    @Override
    public void declineAppointment(String appointmentId, String actorReference) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Decline
        appointment = setParticipantAction(appointment, actorReference, "DECLINE");
        appointment = setAppointmentStatusBasedOnParticipantActions(appointment);
        //Update the resource
        FhirUtil.updateFhirResource(fhirClient, appointment, "Decline Appointment");
    }

    @Override
    public void tentativelyAcceptAppointment(String appointmentId, String actorReference) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Tentatively Accept
        appointment = setParticipantAction(appointment, actorReference, "TENTATIVE");
        appointment = setAppointmentStatusBasedOnParticipantActions(appointment);
        //Update the resource
        FhirUtil.updateFhirResource(fhirClient, appointment, "TentativelyAccept Appointment");
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

    private IQuery addShowPastAppointmentConditions(IQuery searchQuery, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption) {
        if (filterDateOption.isPresent()) {
            // Check for bad requests
            if (!SearchKeyEnum.AppointmentFilterKey.contains(filterDateOption.get())) {
                throw new BadRequestException("Unidentified filter option:" + filterDateOption.get());
            }
            Date today = new Date();
            Calendar c = Calendar.getInstance();
            c.setTime(today);
            if (filterDateOption.get().equalsIgnoreCase(SearchKeyEnum.AppointmentFilterKey.TODAY.toString())) {
                log.info("Searching TODAY's appointments");
                searchQuery.where(Appointment.DATE.afterOrEquals().day(today));
                c.add(Calendar.DATE, 1);
                Date tomorrow = c.getTime();
                searchQuery.where(Appointment.DATE.beforeOrEquals().day(tomorrow));
            } else if (filterDateOption.get().equalsIgnoreCase(SearchKeyEnum.AppointmentFilterKey.WEEK.toString())) {
                log.info("Searching this WEEK's appointments");
                Calendar first = (Calendar) c.clone();
                first.add(Calendar.DAY_OF_WEEK, first.getFirstDayOfWeek() - first.get(Calendar.DAY_OF_WEEK));
                Calendar last = (Calendar) first.clone();
                last.add(Calendar.DAY_OF_WEEK, 6);
                Date startDate = first.getTime();
                Date endDate = last.getTime();
                searchQuery.where(Appointment.DATE.afterOrEquals().day(startDate));
                searchQuery.where(Appointment.DATE.beforeOrEquals().day(endDate));
            } else if (filterDateOption.get().equalsIgnoreCase(SearchKeyEnum.AppointmentFilterKey.MONTH.toString())) {
                log.info("Searching this MONTH's appointments");
                c.set(Calendar.DAY_OF_MONTH, 1);
                Date startDate = c.getTime();
                c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                Date endDate = c.getTime();
                searchQuery.where(Appointment.DATE.afterOrEquals().day(startDate));
                searchQuery.where(Appointment.DATE.beforeOrEquals().day(endDate));
            }
        } else {
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
            throw new PreconditionFailedException("AppointmentDto is NULL!!");
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
            throw new PreconditionFailedException("The following required fields are missing in the appointmentDto: " + missingFields);
        }

        if (!DateUtil.isValidDateTimeRange(appointmentDto.getStart(), appointmentDto.getEnd(), false)) {
            throw new PreconditionFailedException("Appointment EndDateTime is before StartDateTime");
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
        //Call this function only when Accepting/TentativelyAccepting/Declining an appointment
        AppointmentDto aptDto = AppointmentToAppointmentDtoConverter.map(apt, Optional.empty());
        List<AppointmentParticipantDto> participantList = aptDto.getParticipant();

        if (aptDto.getStatusCode().trim().equalsIgnoreCase(PENDING_APPOINTMENT_STATUS)) {
            boolean allParticipantsHaveResponded = true;
            if (participantList.stream().anyMatch(temp -> temp.getParticipationStatusCode().trim().equalsIgnoreCase(NEEDS_ACTION_PARTICIPATION_STATUS) &&
                    temp.getParticipantRequiredCode().trim().equalsIgnoreCase(REQUIRED))) {
                allParticipantsHaveResponded = false;
            }
            if (allParticipantsHaveResponded) {
                apt.setStatus(Appointment.AppointmentStatus.BOOKED);
            }
        } else {
            if (participantList.stream().anyMatch(temp -> temp.getParticipationStatusCode().trim().equalsIgnoreCase(NEEDS_ACTION_PARTICIPATION_STATUS) &&
                    temp.getParticipantRequiredCode().trim().equalsIgnoreCase(REQUIRED))) {
                apt.setStatus(Appointment.AppointmentStatus.PENDING);
            }
        }

        return apt;
    }

    private Appointment setParticipantAction(Appointment apt, String actorReference, String actionInUpperCase) {
        List<org.hl7.fhir.dstu3.model.Appointment.AppointmentParticipantComponent> participantList = apt.getParticipant();
        try {
            for (org.hl7.fhir.dstu3.model.Appointment.AppointmentParticipantComponent temp : participantList) {
                if (temp.getActor().getReference().equalsIgnoreCase(actorReference)) {
                    switch (actionInUpperCase) {
                        case "ACCEPT":
                            temp.setStatus(Appointment.ParticipationStatus.fromCode(ACCEPTED_PARTICIPATION_STATUS));
                            break;
                        case "DECLINE":
                            temp.setStatus(Appointment.ParticipationStatus.fromCode(DECLINED_PARTICIPATION_STATUS));
                            break;
                        case "TENTATIVE":
                            temp.setStatus(Appointment.ParticipationStatus.fromCode(TENTATIVE_PARTICIPATION_STATUS));
                            break;
                        default:
                            log.error("Unidentified action by the participant.");
                            break;
                    }
                }
            }
        } catch (FHIRException e) {
            log.error("Unable to convert from the given Participation Status Code");
            throw new BadRequestException("Unable to convert from the given Participation Status Code ", e);
        }
        return apt;
    }

    private Set<String> getPatientIdSet(List<PatientDto> patientDtoList){
        if(patientDtoList != null && !patientDtoList.isEmpty()){
            return patientDtoList.stream().map(p -> p.getId()).collect(Collectors.toSet());
        }
        return null;
    }
}

