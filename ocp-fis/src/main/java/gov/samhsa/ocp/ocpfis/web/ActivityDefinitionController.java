package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ActivityDefinitionService;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class ActivityDefinitionController {

    @Autowired
    private ActivityDefinitionService activityDefinitionService;

    @PostMapping("/organizations/{organizationId}/activity-definitions")
    public void createActivityDefinition(@PathVariable String organizationId, @RequestBody ActivityDefinitionDto activityDefinitionDto, @RequestParam(value = "loggedInUser") Optional<String> loggedInUser) {
        try {
            activityDefinitionService.createActivityDefinition(activityDefinitionDto, organizationId, loggedInUser);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @PutMapping("/organizations/{organizationId}/activity-definitions/{activityDefinitionId}")
    public void updateActivityDefinition(@PathVariable String organizationId, @PathVariable String activityDefinitionId, @RequestBody ActivityDefinitionDto activityDefinitionDto, @RequestParam(value = "loggedInUser") Optional<String> loggedInUser) {
        activityDefinitionService.updateActivityDefinition(activityDefinitionDto, organizationId, activityDefinitionId, loggedInUser);
    }

    @GetMapping("/activity-definitions/{activityDefinitionId}")
    public ActivityDefinitionDto getActivityDefinitionById(@PathVariable String activityDefinitionId) {
        return activityDefinitionService.getActivityDefinitionById(activityDefinitionId);
    }

    @GetMapping("/organizations/{organizationId}/activity-definitions")
    public PageDto<ActivityDefinitionDto> getAllActivityDefinitionsByOrganization(@PathVariable String organizationId,
                                                                                 @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                                 @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                                 @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                                 @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return activityDefinitionService.getAllActivityDefinitionsByOrganization(organizationId, searchKey, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/activity-definitions")
    public List<ActivityReferenceDto> getActivityDefinitionsByPractitioner(@RequestParam(value = "practitioner") String practitioner) {
        return activityDefinitionService.getActivityDefinitionsByPractitioner(practitioner);
    }

    @GetMapping("/organizations/{organizationId}/activity-definitions/definition-reference")
    public String getSearchedActivityDefinition(@PathVariable String organizationId,
                                                                     @RequestParam(value="name") String name){
        return activityDefinitionService.getActivityDefinitionByName(organizationId,name);
    }
}
