package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;

import java.util.List;

public interface OrganizationService {
    /**
     * Gets all available organizations in the configured FHIR server
     *
     * @return
     */
    List<OrganizationDto> getAllOrganizations();
}
