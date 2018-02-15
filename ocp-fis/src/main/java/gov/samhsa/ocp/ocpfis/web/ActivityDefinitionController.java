package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ActivityDefinitionService;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActivityDefinitionController {

    @Autowired
    private ActivityDefinitionService activityDefinitionService;

    @RequestMapping("/activity-definitions")
    public void createActivityDefinition(@RequestBody ActivityDefinitionDto activityDefinitionDto){
       activityDefinitionService.createActivityDefinition(activityDefinitionDto);
    }
}
