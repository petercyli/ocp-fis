package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;

import java.util.List;
import java.util.Optional;

public interface OrganizationService {

    OrganizationDto getOrganization(String organizationId);

    PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    PageDto<OrganizationDto> searchOrganizations(Optional<OrganizationController.SearchType> searchType, Optional<String> searchValue, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll);

    void createOrganization(OrganizationDto organizationDto, Optional<String> loggedInUser);

    void updateOrganization(String organizationId, OrganizationDto organizationDto, Optional<String> loggedInUser);

    void inactivateOrganization(String organizationId);

    List<ReferenceDto> getOrganizationsByPractitioner(String practitioner);
}
