package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.EwsCalendarService;
import gov.samhsa.ocp.ocpfis.service.dto.EwsCalendarDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/EWS/calendar")
public class EwsCalendarController {

    private final EwsCalendarService ewsCalendarService;

    public EwsCalendarController(EwsCalendarService ewsCalendarService) {
        this.ewsCalendarService = ewsCalendarService;
    }

    @GetMapping
    public List<EwsCalendarDto> getEwsCalendarAppointments(@RequestParam String emailAddress,
                                                           @RequestParam String password,
                                                           @RequestParam Optional<LocalDateTime> start,
                                                           @RequestParam Optional<LocalDateTime> end) {
        return ewsCalendarService.getEwsCalendarAppointments(emailAddress, password, start, end);
    }
}
