package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;

import java.util.List;
import java.util.Optional;

public interface OrganizationService {
    /**
     * Gets all available organizations in the configured FHIR server
     *
     * @return
     */
    List<OrganizationDto> getAllOrganizations(Optional<String> name);
}
