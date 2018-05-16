package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.EwsCalendarDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameAndEmailAddressDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder;
import microsoft.exchange.webservices.data.core.service.item.Appointment;
import microsoft.exchange.webservices.data.core.service.schema.AppointmentSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.search.CalendarView;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EwsCalendarServiceImpl implements EwsCalendarService {

    static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
        public boolean autodiscoverRedirectionUrlValidationCallback(
                String redirectionUrl) {
            return redirectionUrl.toLowerCase().startsWith("https://");
        }
    }

    @Override
    public List<EwsCalendarDto> getEwsCalendarAppointments(String emailAddress,
                                                           String password,
                                                           Optional<LocalDateTime> start,
                                                           Optional<LocalDateTime> end) {
        ExchangeService service = initializeExchangeService(emailAddress, password);
        LocalDateTime dateNow = LocalDateTime.now();
        LocalDateTime startDate = dateNow.minusMonths(1);
        LocalDateTime endDate = dateNow.plusMonths(1);

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

            return appointments.getItems().stream().map(this::mapAppointmentToDto).collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("Exception occurred either when binding the service or when finding appointments from calendar view", e);
        }
        return null;
    }

    private ExchangeService initializeExchangeService(String emailAddress, String password) {
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        ExchangeCredentials credentials = new WebCredentials(emailAddress, password);
        service.setCredentials(credentials);
        try {
            service.autodiscoverUrl(emailAddress);
        }
        catch (Exception e) {
            log.error("Exception trying to set URL by AutoDiscover", e);
            log.info("Accepting Redirection");
            try {
                service.autodiscoverUrl(emailAddress, new RedirectionUrlCallback());
            }
            catch (Exception e1) {
                log.error("Redirection failed", e1);
            }
        }
        return service;
    }

    private EwsCalendarDto mapAppointmentToDto(Appointment apt) {
        EwsCalendarDto eDto = new EwsCalendarDto();
        try {
            apt.load();
            eDto.setSubject(apt.getSubject());
            eDto.setStart(apt.getStart());
            eDto.setEnd(apt.getEnd());
            eDto.setLocation(apt.getLocation());

            if (apt.getOrganizer() != null) {
                if (FhirUtil.isStringNotNullAndNotEmpty(apt.getOrganizer().getAddress())) {
                    eDto.setOrganizerEmail(apt.getOrganizer().getAddress());
                } else {
                    eDto.setOrganizerEmail("");
                }

                if (FhirUtil.isStringNotNullAndNotEmpty(apt.getOrganizer().getName())) {
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

            eDto.setMyResponse(apt.getMyResponseType().name());
            eDto.setCalUid(apt.getICalUid());
        }
        catch (ServiceLocalException e) {
            log.error("ServiceLocalException when converting EWS Appointment to DTO", e);
        }
        catch (Exception e) {
            log.error("Exception when converting EWS Appointment to DTO", e);
        }
        return eDto;
    }

}
