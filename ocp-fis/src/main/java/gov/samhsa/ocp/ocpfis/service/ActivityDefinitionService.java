package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface ActivityDefinitionService {

    PageDto<ActivityDefinitionDto> getAllActivityDefinitionsByOrganization(String organizationResourceId, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto,String organizationId, Optional<String> loggedInUser);

    void updateActivityDefinition(ActivityDefinitionDto activityDefinitionDto, String organizationId, String activityDefinitionId, Optional<String> loggedInUser);

    List<ActivityReferenceDto> getActivityDefinitionsByPractitioner(String practitioner);

    ActivityDefinitionDto getActivityDefinitionById(String id);

    String getActivityDefinitionByName(String organizationId,String name);
}
