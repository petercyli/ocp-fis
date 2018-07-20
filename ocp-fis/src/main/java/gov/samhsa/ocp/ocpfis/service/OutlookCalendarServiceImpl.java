package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CredentialDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameAndEmailAddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.OutlookCalendarDto;
import gov.samhsa.ocp.ocpfis.service.exception.NotAuthorizedException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.autodiscover.exception.AutodiscoverLocalException;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.misc.IdFormat;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.exception.misc.FormatException;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceRequestException;
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceResponseException;
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder;
import microsoft.exchange.webservices.data.core.service.item.Appointment;
import microsoft.exchange.webservices.data.core.service.schema.AppointmentSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.misc.id.AlternateId;
import microsoft.exchange.webservices.data.search.CalendarView;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OutlookCalendarServiceImpl implements OutlookCalendarService {

    static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
        public boolean autodiscoverRedirectionUrlValidationCallback(
                String redirectionUrl) {
            return redirectionUrl.toLowerCase().startsWith("https://");
        }
    }

    @Override
    public List<OutlookCalendarDto> getOutlookCalendarAppointments(String emailAddress,
                                                                   String password,
                                                                   Optional<LocalDateTime> start,
                                                                   Optional<LocalDateTime> end) {
        ExchangeService service = initializeExchangeService(emailAddress, password);
        LocalDateTime dateNow = LocalDateTime.now();
        LocalDateTime startDate = dateNow.minusYears(1);
        LocalDateTime endDate = dateNow.plusYears(1);

        if (start.isPresent()) {
            startDate = start.get();
        }
        if (end.isPresent()) {
            endDate = end.get();
        }
        final int NUM_APPOINTMENTS = 1000;

        CalendarFolder calendar;
        try {
            // Initialize the calendar folder object with only the folder ID.
            calendar = CalendarFolder.bind(service, WellKnownFolderName.Calendar, new PropertySet());

            // Set the start and end time and number of appointments to retrieve.
            CalendarView cView = new CalendarView(DateUtil.convertLocalDateTimeToUTCDate(startDate), DateUtil.convertLocalDateTimeToUTCDate(endDate), NUM_APPOINTMENTS);

            // Limit the properties returned to the start time, and end time.
            cView.setPropertySet(new PropertySet(AppointmentSchema.Start, AppointmentSchema.End));

            // Retrieve a collection of appointments by using the calendar view.
            FindItemsResults<Appointment> appointments = calendar.findAppointments(cView);
            log.info("Found " + appointments.getTotalCount() + " Outlook appointments.");
            return appointments.getItems().stream().map(this::mapAppointmentToDto).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Exception occurred either when binding the service or when finding appointments from calendar view", e);
            throw new ResourceNotFoundException("Exception occurred either when binding the service or when finding appointments from calendar view", e);
        }
    }

    @Override
    public void loginToOutlook(CredentialDto credentialDto) {
        initializeExchangeService(credentialDto.getUsername(), credentialDto.getPassword());
    }

    private ExchangeService initializeExchangeService(String emailAddress, String password) {
        boolean authenticated = false;
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        ExchangeCredentials credentials = new WebCredentials(emailAddress, password);
        service.setCredentials(credentials);
        try {
            service.autodiscoverUrl(emailAddress, new RedirectionUrlCallback());
            log.info("Auto discover URL complete");
        } catch (AutodiscoverLocalException ale) {
            log.error("Failed to set URL using AutoDiscover", ale.getMessage());
            // throw new NotAuthorizedException("Failed to set URL using AutoDiscover");
        } catch (Exception e) {
            throw new NotAuthorizedException("Failed to set URL using AutoDiscover");
        }

        if (service.getUrl() == null || service.getUrl().getPath().isEmpty()) {
            throw new NotAuthorizedException("URL is not set.");
        }

        try {
            // Once we have the URL, try a ConvertId operation to check if we can access the service. We expect that
            // the user will be authenticated and that we will get an error code due to the invalid format. Expect a
            // ServiceResponseException.
            service.convertId(new AlternateId(IdFormat.EwsId, "Placeholder", emailAddress), IdFormat.EwsId);
        } catch (FormatException fe) {
            // The user principal name is in a bad format.
            log.error("Please enter your credentials in UPN format.", fe.getMessage());
        } catch (ServiceResponseException sre) {
            // The credentials were authenticated. We expect this exception since we are providing intentional bad data for ConvertId
            log.info("Successfully connected to EWS.");
            authenticated = true;
        } catch (ServiceRequestException sreq) {
            throw new NotAuthorizedException("ServiceRequestException: The credentials were not authenticated.", sreq);
        } catch (Exception e) {
            throw new NotAuthorizedException("Exception: The credentials were not authenticated.", e);
        }

        if (authenticated) {
            return service;
        } else {
            throw new NotAuthorizedException("The credentials were not authenticated.");
        }

    }

    private OutlookCalendarDto mapAppointmentToDto(Appointment apt) {
        OutlookCalendarDto eDto = new OutlookCalendarDto();
        try {
            apt.load();
            eDto.setSubject(apt.getSubject());
            eDto.setStart(DateUtil.convertDateToLocalDateTime(apt.getStart()));
            eDto.setEnd(DateUtil.convertDateToLocalDateTime(apt.getEnd()));
            eDto.setLocation(apt.getLocation());

            if (apt.getOrganizer() != null) {
                if (FhirOperationUtil.isStringNotNullAndNotEmpty(apt.getOrganizer().getAddress())) {
                    eDto.setOrganizerEmail(apt.getOrganizer().getAddress());
                } else {
                    eDto.setOrganizerEmail("");
                }

                if (FhirOperationUtil.isStringNotNullAndNotEmpty(apt.getOrganizer().getName())) {
                    eDto.setOrganizerName(apt.getOrganizer().getName());
                } else {
                    eDto.setOrganizerName("");
                }
            }

            eDto.setDurationInMinutes(apt.getDuration().getTotalMinutes());
            eDto.setTimeZone(apt.getTimeZone());

            eDto.setAllDayEvent(apt.getIsAllDayEvent());
            eDto.setCancelled(apt.getIsCancelled());
            eDto.setMeeting(apt.getIsMeeting());
            eDto.setRecurring(apt.getIsRecurring());
            eDto.setResponseRequested(apt.getIsResponseRequested());


            List<NameAndEmailAddressDto> attendeesList = apt.getRequiredAttendees().getItems().stream().map(attendee -> {
                        NameAndEmailAddressDto neDto = new NameAndEmailAddressDto();
                        neDto.setEmail(attendee.getAddress());
                        neDto.setName(attendee.getName());
                        return neDto;
                    }
            ).collect(Collectors.toList());
            eDto.setRequiredAttendees(attendeesList);

            List<NameAndEmailAddressDto> optionalAttendeesList = apt.getOptionalAttendees().getItems().stream().map(attendee -> {
                        NameAndEmailAddressDto neDto = new NameAndEmailAddressDto();
                        neDto.setEmail(attendee.getAddress());
                        neDto.setName(attendee.getName());
                        return neDto;
                    }
            ).collect(Collectors.toList());
            eDto.setOptionalAttendees(optionalAttendeesList);

            List<String> requiredAttendeesName = attendeesList.stream().map(NameAndEmailAddressDto::getName).collect(Collectors.toList());
            eDto.setRequiredAttendeeName(requiredAttendeesName);
            List<String> optionalAttendeesName = optionalAttendeesList.stream().map(NameAndEmailAddressDto::getName).collect(Collectors.toList());
            eDto.setOptionalAttendeeName(optionalAttendeesName);

            //Merge the 2 lists
            requiredAttendeesName.addAll(optionalAttendeesName);
            eDto.setAllAttendeeName(requiredAttendeesName);

            eDto.setMyResponse(apt.getMyResponseType().name());
            eDto.setCalUid(apt.getICalUid());
        } catch (ServiceLocalException e) {
            log.error("ServiceLocalException when converting EWS Appointment to DTO", e);
        } catch (Exception e) {
            log.error("Exception when converting EWS Appointment to DTO", e);
        }
        return eDto;
    }

}
