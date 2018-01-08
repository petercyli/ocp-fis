package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.OrganizationService;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;



@RestController
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * Gets all available locations in the configured FHIR server
     *
     * @return
     */
    @GetMapping("/organizations")
    public List<OrganizationDto> getAllOrganization() {
        return organizationService.getAllOrganizations();
    }

}