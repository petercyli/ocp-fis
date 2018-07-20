package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;

import java.util.List;
import java.util.Optional;

public interface
PractitionerService {
    PageDto<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    PageDto<PractitionerDto> searchPractitioners(Optional<PractitionerController.SearchType> searchType, Optional<String> searchValue, Optional<String> organization, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll);

    void createPractitioner(PractitionerDto practitionerDto);

    void updatePractitioner(String practitionerId, PractitionerDto practitionerDto);

    PractitionerDto getPractitioner(String practitionerId);

    List<ReferenceDto> getPractitionersInOrganizationByPractitionerId(Optional<String> practitioner,Optional<String> organization,Optional<String> role);

    PageDto<PractitionerDto> getPractitionersByOrganizationAndRole(String organization, Optional<String> role, Optional<Integer> pageNumber,Optional<Integer> pageSize);

    String getPractitionerByName(String name);

    void assignLocationToPractitioner(String practitionerId, String organizationId, String locationId);

    void unassignLocationToPractitioner(String practitionerId, String organizationId, String locationId);
}
