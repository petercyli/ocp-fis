package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.constants.AppointmentConstants;
import gov.samhsa.ocp.ocpfis.domain.CodeSystemEnum;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.OutsideParticipant;
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
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.ProvenanceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.AppointmentResponse;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
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

import static ca.uhn.fhir.rest.api.Constants.PARAM_LASTUPDATED;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentToAppointmentDtoConverter appointmentToAppointmentDtoConverter;
    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    private final PatientService patientService;

    private final CareTeamServiceImpl careTeamService;

    private final ProvenanceUtil provenanceUtil;

    private final ParticipantService participantService;

    @Autowired
    public AppointmentServiceImpl(AppointmentToAppointmentDtoConverter appointmentToAppointmentDtoConverter, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, PatientService patientService, ProvenanceUtil provenanceUtil, CareTeamServiceImpl careTeamService, ParticipantService participantService) {
        this.appointmentToAppointmentDtoConverter = appointmentToAppointmentDtoConverter;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.patientService = patientService;
        this.provenanceUtil = provenanceUtil;
        this.careTeamService = careTeamService;
        this.participantService = participantService;
    }

    @Override
    public void createAppointment(AppointmentDto appointmentDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();

        String creatorName = appointmentDto.getCreatorName() != null ? appointmentDto.getCreatorName().trim() : "";
        log.info("Creating an appointment initiated by " + creatorName);

        //Validate if the request body has all the mandatory fields
        validateAppointDtoFromRequest(appointmentDto);
        //Map
        final Appointment appointment = AppointmentDtoToAppointmentConverter.map(appointmentDto, true, Optional.empty());
        //Set created Date
        appointment.setCreated(new Date());
        //Set Profile Meta Data
        FhirProfileUtil.setAppointmentProfileMetaData(fhirClient, appointment);
        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, appointment, Optional.empty(), ResourceType.Appointment.name(), "Create Appointment");
        //Create
        MethodOutcome appointmentMethodOutcome = FhirOperationUtil.createFhirResource(fhirClient, appointment, ResourceType.Appointment.name());
        idList.add(ResourceType.Appointment.name() + "/" + FhirOperationUtil.getFhirId(appointmentMethodOutcome));

        if (fisProperties.isProvenanceEnabled()) {
            provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.CREATE, loggedInUser);
        }
    }

    @Override
    public void updateAppointment(String appointmentId, AppointmentDto appointmentDto, Optional<String> loggedInUser) {
        log.info("Updating appointmentId: " + appointmentId);
        List<String> idList = new ArrayList<>();

        //Validate if the request body has all the mandatory fields
        validateAppointDtoFromRequest(appointmentDto);

        //Map
        Appointment appointment = AppointmentDtoToAppointmentConverter.map(appointmentDto, false, Optional.of(appointmentId));

        //Set Appointment Status only in specific case, else set the value from the dto
        if (appointmentDto.getStatusCode().equalsIgnoreCase(AppointmentConstants.PROPOSED_APPOINTMENT_STATUS) || appointmentDto.getStatusCode().equalsIgnoreCase(AppointmentConstants.PENDING_APPOINTMENT_STATUS)) {
            appointment = setAppointmentStatusBasedOnParticipantActions(appointment);
        }

        //Set Profile Meta Data
        FhirProfileUtil.setAppointmentProfileMetaData(fhirClient, appointment);

        // Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, appointment, Optional.of(appointmentId), ResourceType.Appointment.name(), "Update Appointment");

        //Update
        MethodOutcome methodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, appointment, "Update Appointment");
        idList.add(ResourceType.Appointment.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

        if (fisProperties.isProvenanceEnabled()) {
            provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.UPDATE, loggedInUser);
        }
    }

    @Override
    public List<ParticipantReferenceDto> getAppointmentParticipants(String patientId, Optional<List<String>> roles, Optional<String> appointmentId) {
        List<ReferenceDto> participantsByRoles = new ArrayList<>();
        List<ParticipantReferenceDto> participantsSelected = new ArrayList<>();

        Bundle careTeamBundle = (Bundle) FhirOperationUtil.searchNoCache(fhirClient, CareTeam.class, Optional.empty())
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
                        .flatMap(it -> CareTeamToCareTeamDtoConverter.mapToParticipants(it, roles, fhirClient).stream()).collect(toList());
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

        Bundle appointmentBundle = (Bundle) FhirOperationUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty())
                .where(new TokenClientParam("_id").exactly().code(appointmentId.trim()))
                .returnBundle(Bundle.class)
                .execute();

        if (appointmentBundle == null || appointmentBundle.getEntry().isEmpty()) {
            log.info("No appointment was found for the given appointmentId:" + appointmentId);
            throw new ResourceNotFoundException("No appointment was found for the given appointment ID:" + appointmentId);
        }

        log.info("FHIR appointment bundle retrieved from FHIR server successfully for appointment Id:" + appointmentId);

        Bundle.BundleEntryComponent retrievedAppointment = appointmentBundle.getEntry().get(0);
        return appointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), Optional.empty(), Optional.of(true));
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

        IQuery iQuery = FhirOperationUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty());

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
                        (appointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), requesterReference, Optional.of(true)))).collect(toList());

        //Remove cancelled appointments
        appointmentDtos.removeIf(a -> a.getStatusCode().equalsIgnoreCase(AppointmentConstants.CANCELLED_APPOINTMENT_STATUS));

        double totalPages = Math.ceil((double) otherPageAppointmentBundle.getTotal() / numberOfAppointmentsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();
        log.info("Found " + otherPageAppointmentBundle.getTotal() + " appointments during search(getAppointments).");
        return new PageDto<>(appointmentDtos, numberOfAppointmentsPerPage, totalPages, currentPage, appointmentDtos.size(), otherPageAppointmentBundle.getTotal());
    }

    @Override
    public List<AppointmentDto> getNonDeclinedAppointmentsWithNoPagination(Optional<List<String>> statusList, Optional<String> patientId, Optional<String> practitionerId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<Boolean> sortByStartTimeAsc) {
        IQuery iQuery = FhirOperationUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty());

        String actorReference = null;

        if (patientId.isPresent()) {
            log.info("Searching Appointments for patientId = " + patientId.get().trim());
            iQuery.where(new ReferenceClientParam("patient").hasId(patientId.get().trim()));
            actorReference = "Patient/" + patientId.get().trim();
        }
        if (practitionerId.isPresent()) {
            log.info("Searching Appointments for practitionerId = " + practitionerId.get().trim());
            iQuery.where(new ReferenceClientParam("practitioner").hasId(practitionerId.get().trim()));
            actorReference = "Practitioner/" + practitionerId.get().trim();
        }
        final String actorReferenceFinal = actorReference;
        // Check if there are any additional search criteria
        iQuery = addStatusSearchConditions(iQuery, statusList);

        // Additional Search Key and Value
        iQuery = addSearchKeyValueConditions(iQuery, searchKey, searchValue);

        // Past appointments
        iQuery = addShowPastAppointmentConditions(iQuery, showPastAppointments, Optional.empty());

        //Check sort order
        iQuery = addSortConditions(iQuery, sortByStartTimeAsc);

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        List<Bundle.BundleEntryComponent> retrievedAppointments = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);

        List<AppointmentDto> allCalendarAppointments = retrievedAppointments.stream()
                .filter(retrievedBundle -> retrievedBundle.getResource().getResourceType().equals(ResourceType.Appointment)).map(retrievedAppointment ->
                        (appointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), Optional.of(actorReferenceFinal), Optional.empty()))).collect(toList());

        //Remove cancelled appointments
        allCalendarAppointments.removeIf(a -> a.getStatusCode().equalsIgnoreCase(AppointmentConstants.CANCELLED_APPOINTMENT_STATUS));

        if (actorReference != null && !actorReference.trim().isEmpty()) {
            //Remove the appointments which has been declined by the actorReference and not required to participate
            allCalendarAppointments.removeIf(app -> hasActorDeclined(app, actorReferenceFinal));
            allCalendarAppointments.removeIf(app -> isActorNotRequired(app, actorReferenceFinal));
        }
        log.info("Found " + allCalendarAppointments.size() + " appointments during search(getNonDeclinedAppointmentsWithNoPagination).");

        return allCalendarAppointments;
    }

    private boolean hasActorDeclined(AppointmentDto appointmentDto, String actorReference) {
        return appointmentDto.getParticipant().stream().anyMatch(p -> p.getActorReference().equalsIgnoreCase(actorReference) && p.getParticipationStatusCode().equalsIgnoreCase(AppointmentConstants.DECLINED_PARTICIPATION_STATUS));
    }

    private boolean isActorNotRequired(AppointmentDto appointmentDto, String actorReference) {
        return appointmentDto.getParticipant().stream().anyMatch(p -> p.getActorReference().equalsIgnoreCase(actorReference) && !p.getParticipantRequiredCode().equalsIgnoreCase(AppointmentConstants.REQUIRED) && !p.getParticipationStatusCode().equalsIgnoreCase(AppointmentConstants.ACCEPTED_PARTICIPATION_STATUS));
    }

    @Override
    public PageDto<AppointmentDto> getAppointmentsByPractitionerAndAssignedCareTeamPatients(String practitionerId, Optional<List<String>> statusList, Optional<String> requesterReference, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showPastAppointments, Optional<String> filterDateOption, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfAppointmentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Appointment.name());
        Bundle firstPageAppointmentBundle;
        Bundle otherPageAppointmentBundle;
        boolean firstPage = true;

        IQuery iQuery = FhirOperationUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty());

        // Find all patients who have made the given Practitioner Id as their Care Team member
        List<PatientDto> assignedPatients = patientService.getPatientsByPractitioner(Optional.of(practitionerId.trim()), Optional.empty(), Optional.empty());
        Set<String> patientIds = getPatientIdSet(assignedPatients);

        if (patientIds != null && !patientIds.isEmpty()) {
            log.info("Searching for Patients assigned/belonging to Practitioner Id :" + practitionerId);
            log.info("Number of Patients assigned/belonging to Practitioner Id (" + practitionerId + ") = " + patientIds.size());
            patientIds.add(practitionerId);
            iQuery.where(new ReferenceClientParam("actor").hasAnyOfIds(patientIds));
        } else {
            log.info("No Patient found assigned/belonging to Practitioner Id :" + practitionerId);
            log.info("Hence, searching Appointments belonging to only Practitioner with Id = " + practitionerId.trim());
            iQuery.where(new ReferenceClientParam("practitioner").hasId(practitionerId.trim()));
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
                        (appointmentToAppointmentDtoConverter.map((Appointment) retrievedAppointment.getResource(), requesterReference, Optional.of(true)))).collect(toList());

        //Remove cancelled appointments
        appointmentDtos.removeIf(a -> a.getStatusCode().equalsIgnoreCase(AppointmentConstants.CANCELLED_APPOINTMENT_STATUS));

        double totalPages = Math.ceil((double) otherPageAppointmentBundle.getTotal() / numberOfAppointmentsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        log.info("Found " + otherPageAppointmentBundle.getTotal() + " appointments during search(getAppointmentsByPractitionerAndAssignedCareTeamPatients).");
        return new PageDto<>(appointmentDtos, numberOfAppointmentsPerPage, totalPages, currentPage, appointmentDtos.size(), otherPageAppointmentBundle.getTotal());

    }

    @Override
    public void cancelAppointment(String appointmentId) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Cancel
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

        //Set Profile Meta Data
        FhirProfileUtil.setAppointmentProfileMetaData(fhirClient, appointment);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, appointment, Optional.of(appointmentId), ResourceType.Appointment.name(), "Cancel Appointment");

        //Update the resource
        FhirOperationUtil.updateFhirResource(fhirClient, appointment, "Cancel Appointment");
    }

    @Override
    public void acceptAppointment(String appointmentId, String actorReference) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Accept
        appointment = setParticipantAction(appointment, actorReference, "ACCEPT");
        appointment = setAppointmentStatusBasedOnParticipantActions(appointment);

        //Set Profile Meta Data
        FhirProfileUtil.setAppointmentProfileMetaData(fhirClient, appointment);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, appointment, Optional.of(appointmentId), ResourceType.Appointment.name(), "Accept Appointment");

        //Update the resource
        FhirOperationUtil.updateFhirResource(fhirClient, appointment, "Accept Appointment");
    }

    @Override
    public void declineAppointment(String appointmentId, String actorReference) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Decline
        appointment = setParticipantAction(appointment, actorReference, "DECLINE");
        appointment = setAppointmentStatusBasedOnParticipantActions(appointment);

        //Set Profile Meta Data
        FhirProfileUtil.setAppointmentProfileMetaData(fhirClient, appointment);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, appointment, Optional.of(appointmentId), ResourceType.Appointment.name(), "Decline Appointment");

        //Update the resource
        FhirOperationUtil.updateFhirResource(fhirClient, appointment, "Decline Appointment");
    }

    @Override
    public void tentativelyAcceptAppointment(String appointmentId, String actorReference) {
        Appointment appointment = fhirClient.read().resource(Appointment.class).withId(appointmentId.trim()).execute();
        //Tentatively Accept
        appointment = setParticipantAction(appointment, actorReference, "TENTATIVE");
        appointment = setAppointmentStatusBasedOnParticipantActions(appointment);

        //Set Profile Meta Data
        FhirProfileUtil.setAppointmentProfileMetaData(fhirClient, appointment);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, appointment, Optional.of(appointmentId), ResourceType.Appointment.name(), "Tentatively Accept Appointment");

        //Update the resource
        FhirOperationUtil.updateFhirResource(fhirClient, appointment, "TentativelyAccept Appointment");
    }

    @Override
    public List<AppointmentParticipantReferenceDto> getAllHealthcareServicesReferences(String resourceType, String resourceValue) {
        IQuery iQuery = fhirClient.search().forResource(HealthcareService.class);

        if (SearchKeyEnum.HealthcareServiceParticipantSearchKey.ORGANIZATION.name().equalsIgnoreCase(resourceType)) {
            iQuery.where(new ReferenceClientParam("organization").hasId(resourceValue));
        } else if (SearchKeyEnum.HealthcareServiceParticipantSearchKey.LOCATION.name().equalsIgnoreCase(resourceType)) {
            iQuery.where(new ReferenceClientParam("location").hasId(resourceValue));
        }
        Bundle bundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(iQuery).returnBundle(Bundle.class).execute();

        return FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties).stream().map(entry -> {
            HealthcareService hs = (HealthcareService) entry.getResource();
            AppointmentParticipantReferenceDto referenceDto = new AppointmentParticipantReferenceDto();
            referenceDto.setDisplay(hs.getName());
            referenceDto.setReference(ResourceType.HealthcareService.toString() + "/" + hs.getIdElement().getIdPart());
            setParticipantTypeAsAttender(referenceDto);
            setParticipantRequiredAsInformationOnly(referenceDto);
            setParticipantStatusAsAccepted(referenceDto);
            return referenceDto;
        }).collect(toList());
    }

    @Override
    public List<AppointmentParticipantReferenceDto> getAllLocationReferences(String resourceType, String resourceValue) {
        List<AppointmentParticipantReferenceDto> locationsRef = new ArrayList<>();
        if (SearchKeyEnum.LocationAppointmentParticipantSearchKey.HEALTHCARESERVICE.name().equalsIgnoreCase(resourceType)) {
            HealthcareService hcs = fhirClient.read().resource(HealthcareService.class).withId(resourceValue).execute();
            locationsRef = hcs.getLocation().stream().map(l -> convertLocationRefToAppointmentParticipantReferenceDto(l))
                    .collect(toList());
        } else if (SearchKeyEnum.LocationAppointmentParticipantSearchKey.PRACTITIONER.name().equalsIgnoreCase(resourceType)) {
            Bundle prRoleBundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(PractitionerRole.class).where(new ReferenceClientParam("practitioner").hasId(resourceValue)))
                    .returnBundle(Bundle.class).execute();
            locationsRef = FhirOperationUtil.getAllBundleComponentsAsList(prRoleBundle, Optional.empty(), fhirClient, fisProperties).stream()
                    .flatMap(pr -> {
                        PractitionerRole p = (PractitionerRole) pr.getResource();
                        return p.getLocation().stream().map(l -> convertLocationRefToAppointmentParticipantReferenceDto(l));
                    }).collect(toList());
        } else if (SearchKeyEnum.LocationAppointmentParticipantSearchKey.ORGANIZATION.name().equalsIgnoreCase(resourceType)) {
            IQuery iQuery = fhirClient.search().forResource(Location.class)
                    .where(new ReferenceClientParam("organization").hasId(resourceValue));
            Bundle bundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(iQuery).returnBundle(Bundle.class).execute();
            locationsRef = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties).stream().map(entry -> {
                Location location = (Location) entry.getResource();
                AppointmentParticipantReferenceDto referenceDto = new AppointmentParticipantReferenceDto();
                referenceDto.setDisplay(location.getName());
                referenceDto.setReference(ResourceType.Location.toString() + "/" + location.getIdElement().getIdPart());
                setParticipantTypeAsAttender(referenceDto);
                setParticipantRequiredAsInformationOnly(referenceDto);
                setParticipantStatusAsAccepted(referenceDto);
                return referenceDto;
            }).collect(toList());
        }
        return locationsRef;
    }

    @Override
    public List<AppointmentParticipantReferenceDto> getPractitionersReferences(String resourceType, String resourceValue) {
        List<AppointmentParticipantReferenceDto> practitionerReferences = new ArrayList<>();
        if (SearchKeyEnum.PractitionerParticipantSearchKey.PATIENT.name().equalsIgnoreCase(resourceType)) {
            practitionerReferences = careTeamService.getParticipantMemberFromCareTeam(resourceValue).stream()
                    .filter(ct -> ct.getReference().split("/")[0].equalsIgnoreCase(ResourceType.Practitioner.toString()))
                    .map(ct -> convertPractitionerReferenceToAppointmentParticipantReferenceDto(ct))
                    .collect(toList());
        } else if (SearchKeyEnum.PractitionerParticipantSearchKey.LOCATION.name().equalsIgnoreCase(resourceType)) {
            Bundle bundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(PractitionerRole.class)
                    .where(new ReferenceClientParam("location").hasId(resourceValue)))
                    .include(PractitionerRole.INCLUDE_PRACTITIONER)
                    .sort().descending(PARAM_LASTUPDATED).returnBundle(Bundle.class).execute();

            if (bundle != null && !bundle.getEntry().isEmpty()) {
                practitionerReferences = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties)
                        .stream().filter(it -> it.getResource().getResourceType().equals(ResourceType.Practitioner))
                        .map(it -> {
                            Practitioner pr = (Practitioner) it.getResource();
                            ReferenceDto referenceDto = new ReferenceDto();
                            referenceDto.setReference("Practitioner/" + pr.getIdElement().getIdPart());
                            pr.getName().stream().findAny().ifPresent(name -> {
                                String ln = name.getFamily();
                                StringType fnStringType = name.getGiven().stream().findAny().orElse(null);
                                String fn = fnStringType.getValueNotNull();
                                referenceDto.setDisplay(fn + " " + ln);
                            });
                            AppointmentParticipantReferenceDto aRefDto = convertPractitionerReferenceToAppointmentParticipantReferenceDto(referenceDto);
                            return aRefDto;
                        }).distinct().collect(toList());

            }
        } else if (SearchKeyEnum.PractitionerParticipantSearchKey.ORGANIZATION.name().equalsIgnoreCase(resourceType)) {
            practitionerReferences = getAllPractitionersInOrganization(resourceValue);
        }
        return practitionerReferences;
    }

    @Override
    public List<OutsideParticipant> searchOutsideParticipants(String patient, String participantType, String name, String organization, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {
        List<OutsideParticipant> outsideParticipants = participantService.retrieveOutsideParticipants(patient, participantType, name, organization, page, size, showAll);
        outsideParticipants.forEach(outsideParticipant -> outsideParticipant.setAppointmentParticipantReferenceDto(mapToAppointmentParticipantReference(outsideParticipant, participantType)));
        return outsideParticipants;
    }

    private List<AppointmentParticipantReferenceDto> getAllPractitionersInOrganization(String organization) {
        int numberOfPractitionersPerPage = PaginationUtil.getValidPageSize(fisProperties, Optional.empty(), ResourceType.Practitioner.name());

        //Get the practitioners
        Bundle practitionerBundle = (Bundle) fhirClient.search().forResource(PractitionerRole.class)
                .sort().descending(PARAM_LASTUPDATED)
                .where(new ReferenceClientParam("organization").hasId(organization))
                .include(new Include("PractitionerRole:practitioner"))
                .returnBundle(Bundle.class)
                .execute();
        List<Bundle.BundleEntryComponent> practitionerEntry = FhirOperationUtil.getAllBundleComponentsAsList(practitionerBundle, Optional.empty(), fhirClient, fisProperties);
        //Get the practitioners belonging to the organization
        List<String> practitionerIds = practitionerEntry.stream()
                .filter(retrievedPractitionerAndPractitionerRoles -> retrievedPractitionerAndPractitionerRoles.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(practitioner -> (practitioner.getResource()).getIdElement().getIdPart())
                .collect(toList());

        //Get the practitioners along with the practitioner Roles and organizations in dto
        Bundle bundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().codes(practitionerIds))
                .revInclude(PractitionerRole.INCLUDE_PRACTITIONER)
                .sort().descending(PARAM_LASTUPDATED)
                .returnBundle(Bundle.class)
                .execute();

        return FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.of(numberOfPractitionersPerPage), fhirClient, fisProperties).stream()
                .filter(retrievedPractitionerAndPractitionerRoles -> retrievedPractitionerAndPractitionerRoles.getResource().getResourceType().equals(ResourceType.Practitioner))
                .map(entry -> {
                    Practitioner practitioner = (Practitioner) entry.getResource();
                    AppointmentParticipantReferenceDto referenceDto = new AppointmentParticipantReferenceDto();
                    practitioner.getName().stream().findAny().ifPresent(name -> {
                        String ln = name.getFamily();
                        StringType fnStringType = name.getGiven().stream().findAny().orElse(null);
                        String fn = fnStringType != null ? fnStringType.getValueNotNull() : null;
                        referenceDto.setDisplay(fn + " " + ln);
                    });
                    referenceDto.setReference(ResourceType.Practitioner.toString() + "/" + practitioner.getIdElement().getIdPart());
                    setParticipantTypeAsAttender(referenceDto);
                    setParticipantRequiredAsInformationOnly(referenceDto);
                    setParticipantStatusAsAccepted(referenceDto);
                    return referenceDto;
                })
                .collect(toList());
    }

    private AppointmentParticipantReferenceDto mapToAppointmentParticipantReference(OutsideParticipant outsideParticipant, String participantType) {
        AppointmentParticipantReferenceDto referenceDto = new AppointmentParticipantReferenceDto();
        referenceDto.setDisplay(outsideParticipant.getName());
        referenceDto.setReference(StringUtils.capitalize(participantType) + "/" + outsideParticipant.getParticipantId());
        setParticipantTypeAsAttender(referenceDto);
        setParticipantRequiredAsInformationOnly(referenceDto);
        setParticipantStatusAsAccepted(referenceDto);
        return referenceDto;
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

        if (FhirOperationUtil.isStringNullOrEmpty(appointmentDto.getDescription())) {
            missingFields = missingFields + "description, ";
        }

        if (FhirOperationUtil.isStringNullOrEmpty(appointmentDto.getTypeCode())) {
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

        Bundle bundle = (Bundle) FhirOperationUtil.searchNoCache(fhirClient, Appointment.class, Optional.empty())
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

        AppointmentDto aptDto = appointmentToAppointmentDtoConverter.map(apt, Optional.empty(), Optional.of(false));
        List<AppointmentParticipantDto> participantList = aptDto.getParticipant();

        if (aptDto.getStatusCode().trim().equalsIgnoreCase(AppointmentConstants.PENDING_APPOINTMENT_STATUS)) {
            boolean allParticipantsHaveResponded = true;
            if (participantList.stream().anyMatch(temp -> temp.getParticipationStatusCode().trim().equalsIgnoreCase(AppointmentConstants.NEEDS_ACTION_PARTICIPATION_STATUS) &&
                    temp.getParticipantRequiredCode().trim().equalsIgnoreCase(AppointmentConstants.REQUIRED))) {
                allParticipantsHaveResponded = false;
            }
            if (allParticipantsHaveResponded) {
                apt.setStatus(Appointment.AppointmentStatus.BOOKED);
            }
        } else {
            if (participantList.stream().anyMatch(temp -> temp.getParticipationStatusCode().trim().equalsIgnoreCase(AppointmentConstants.NEEDS_ACTION_PARTICIPATION_STATUS) &&
                    temp.getParticipantRequiredCode().trim().equalsIgnoreCase(AppointmentConstants.REQUIRED))) {
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
                            temp.setStatus(Appointment.ParticipationStatus.fromCode(AppointmentConstants.ACCEPTED_PARTICIPATION_STATUS));
                            break;
                        case "DECLINE":
                            temp.setStatus(Appointment.ParticipationStatus.fromCode(AppointmentConstants.DECLINED_PARTICIPATION_STATUS));
                            break;
                        case "TENTATIVE":
                            temp.setStatus(Appointment.ParticipationStatus.fromCode(AppointmentConstants.TENTATIVE_PARTICIPATION_STATUS));
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

    private Set<String> getPatientIdSet(List<PatientDto> patientDtoList) {
        if (patientDtoList != null && !patientDtoList.isEmpty()) {
            return patientDtoList.stream().map(PatientDto::getId).collect(Collectors.toSet());
        }
        return null;
    }

    private void setParticipantTypeAsAttender(AppointmentParticipantReferenceDto referenceDto) {
        referenceDto.setParticipationTypeCode(Optional.of(AppointmentConstants.ATTENDER_PARTICIPANT_TYPE_CODE));
        referenceDto.setParticipationTypeDisplay(Optional.of(AppointmentConstants.ATTENDER_PARTICIPANT_TYPE_DISPLAY));
        referenceDto.setParticipantStatusSystem(Optional.of(CodeSystemEnum.APPOINTMENT_PARTICIPATION_TYPE.getUrl()));
    }

    private void setParticipantRequiredAsInformationOnly(AppointmentParticipantReferenceDto referenceDto) {
        referenceDto.setParticipantRequiredCode(Optional.of(Appointment.ParticipantRequired.INFORMATIONONLY.toCode()));
        referenceDto.setParticipantRequiredDisplay(Optional.of(Appointment.ParticipantRequired.INFORMATIONONLY.getDisplay()));
        referenceDto.setParticipantRequiredSystem(Optional.of(Appointment.ParticipantRequired.INFORMATIONONLY.getSystem()));
    }

    private void setParticipantStatusAsAccepted(AppointmentParticipantReferenceDto referenceDto) {
        referenceDto.setParticipantStatusCode(Optional.of(AppointmentResponse.ParticipantStatus.ACCEPTED.toCode()));
        referenceDto.setParticipantStatusDisplay(Optional.of(AppointmentResponse.ParticipantStatus.ACCEPTED.getDisplay()));
        referenceDto.setParticipantStatusSystem(Optional.of(AppointmentResponse.ParticipantStatus.ACCEPTED.getSystem()));
    }

    private void setParticipantStatusAsNeedsAction(AppointmentParticipantReferenceDto referenceDto) {
        referenceDto.setParticipantStatusCode(Optional.of(AppointmentResponse.ParticipantStatus.NEEDSACTION.toCode()));
        referenceDto.setParticipantStatusDisplay(Optional.of(AppointmentResponse.ParticipantStatus.NEEDSACTION.getDisplay()));
        referenceDto.setParticipantStatusSystem(Optional.of(AppointmentResponse.ParticipantStatus.NEEDSACTION.getSystem()));
    }

    private AppointmentParticipantReferenceDto convertLocationRefToAppointmentParticipantReferenceDto(Reference location) {
        AppointmentParticipantReferenceDto referenceDto = new AppointmentParticipantReferenceDto();
        referenceDto.setReference(location.getReference());
        Location l = fhirClient.read().resource(Location.class).withId(location.getReference().split("/")[1]).execute();
        referenceDto.setDisplay(l.getName());
        setParticipantTypeAsAttender(referenceDto);
        setParticipantRequiredAsInformationOnly(referenceDto);
        setParticipantStatusAsAccepted(referenceDto);
        return referenceDto;
    }

    private AppointmentParticipantReferenceDto convertPractitionerReferenceToAppointmentParticipantReferenceDto(ReferenceDto ref) {
        AppointmentParticipantReferenceDto appointmentParticipantReferenceDto = new AppointmentParticipantReferenceDto();
        appointmentParticipantReferenceDto.setReference(ref.getReference());
        appointmentParticipantReferenceDto.setDisplay(ref.getDisplay());
        setParticipantTypeAsAttender(appointmentParticipantReferenceDto);
        setParticipantStatusAsNeedsAction(appointmentParticipantReferenceDto);
        return appointmentParticipantReferenceDto;
    }
}

