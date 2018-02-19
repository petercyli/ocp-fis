package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.Optional;

public interface ActivityDefinitionService {

    PageDto<ActivityDefinitionDto> getAllActivityDefinitionsByOrganization(String organizationResourceId, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto,String organizationId);
}
