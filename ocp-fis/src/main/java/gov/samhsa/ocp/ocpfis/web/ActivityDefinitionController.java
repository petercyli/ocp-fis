package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ActivityDefinitionService;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class ActivityDefinitionController {

    @Autowired
    private ActivityDefinitionService activityDefinitionService;

    @PostMapping("organization/{organizationId}/activity-definitions")
    public void createActivityDefinition(@PathVariable String organizationId, @RequestBody ActivityDefinitionDto activityDefinitionDto) {
        activityDefinitionService.createActivityDefinition(activityDefinitionDto, organizationId);

    @GetMapping("/organizations/{organizationId}/activity-definitions")
    public PageDto<ActivityDefinitionDto> getAllHealthcareServicesByOrganization(@PathVariable String organizationId,
                                                                                 @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                                                 @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                                                 @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                                                 @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return activityDefinitionService.getAllActivityDefinitionsByOrganization(organizationId, searchKey, searchValue, pageNumber, pageSize);
    }


    @RequestMapping("/activity-definitions")
    public void createActivityDefinition(@RequestBody ActivityDefinitionDto activityDefinitionDto){
       activityDefinitionService.createActivityDefinition(activityDefinitionDto);
    }
}
