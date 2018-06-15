package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.OutlookCalendarService;
import gov.samhsa.ocp.ocpfis.service.dto.CredentialDto;
import gov.samhsa.ocp.ocpfis.service.dto.OutlookCalendarDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/outlook")
public class OutlookCalendarController {

    private final OutlookCalendarService outlookCalendarService;

    public OutlookCalendarController(OutlookCalendarService outlookCalendarService) {
        this.outlookCalendarService = outlookCalendarService;
    }

    @GetMapping("/calendar")
    public List<OutlookCalendarDto> getOutlookCalendarAppointments(@RequestParam String emailAddress,
                                                                   @RequestParam String password,
                                                                   @RequestParam Optional<LocalDateTime> start,
                                                                   @RequestParam Optional<LocalDateTime> end) {
        return outlookCalendarService.getOutlookCalendarAppointments(emailAddress, password, start, end);
    }

    @PostMapping("/login")
    public void login(@Valid @RequestBody CredentialDto credentialDto) {
        outlookCalendarService.loginToOutlook(credentialDto);
    }
}
