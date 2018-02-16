package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ActivityDefinitionService;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActivityDefinitionController {

    @Autowired
    private ActivityDefinitionService activityDefinitionService;

    @PostMapping("organization/{organizationId}/activity-definitions")
    public void createActivityDefinition(@RequestBody ActivityDefinitionDto activityDefinitionDto,@PathVariable String organizationId){
       activityDefinitionService.createActivityDefinition(activityDefinitionDto,organizationId);
    }
}
