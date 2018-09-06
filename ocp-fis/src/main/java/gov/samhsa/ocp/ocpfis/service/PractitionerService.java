package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;

import java.util.List;
import java.util.Optional;

public interface PractitionerService {
    PageDto<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    PageDto<PractitionerDto> searchPractitioners(Optional<PractitionerController.SearchType> searchType, Optional<String> searchValue, Optional<String> organization, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll);

    PractitionerDto findPractitioner(Optional<String> organization, String firstName, Optional<String> middleName, String lastName, String identifierType, String identifier);

    void createPractitioner(PractitionerDto practitionerDto, Optional<String> loggedInUser);

    void updatePractitioner(String practitionerId, PractitionerDto practitionerDto, Optional<String> loggedInUser);

    PractitionerDto getPractitioner(String practitionerId);

    PractitionerDto getPractitionerDemographicsOnly(String practitionerId);

    List<ReferenceDto> getPractitionersInOrganizationByPractitionerId(Optional<String> practitioner,Optional<String> organization, Optional<String> location, Optional<String> role);

    PageDto<PractitionerDto> getPractitionersByOrganizationAndRole(String organization, Optional<String> role, Optional<Integer> pageNumber,Optional<Integer> pageSize);

    String getPractitionerByName(String name);

    void assignLocationToPractitioner(String practitionerId, String organizationId, String locationId);

    void unassignLocationToPractitioner(String practitionerId, String organizationId, String locationId);

    List<PractitionerDto> getAllPractitionersInSystem(Optional<Integer> size, String name);

    List<PractitionerDto> getAllPractitionersInOrganization(String organization);
}
