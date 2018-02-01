package gov.samhsa.ocp.ocpfis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParticipantServiceImpl implements  ParticipantService {

    private final PractitionerService practitionerService;
    private final OrganizationService organizationService;

    public ParticipantServiceImpl(PractitionerService practitionerService, OrganizationService organizationService) {
        this.practitionerService = practitionerService;
        this.organizationService = organizationService;
    }

}
