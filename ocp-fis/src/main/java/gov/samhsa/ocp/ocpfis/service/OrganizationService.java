package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import java.util.Optional;

public interface OrganizationService {

    PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    PageDto<OrganizationDto> searchOrganizations(OrganizationController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    void createOrganization(OrganizationDto organizationDto);

    void updateOrganization(String organizationId, OrganizationDto organizationDto);

    void inactivateOrganization(String organizationId);
}
