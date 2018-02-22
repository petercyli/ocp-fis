package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.TaskService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/tasks/search")
    public PageDto<TaskDto> getTasks(@RequestParam Optional<List<String>> statusList,
                                     @RequestParam(value = "searchType") String searchKey,
                                     @RequestParam(value = "searchValue") String searchValue,
                                     @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                     @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return taskService.getTasks(statusList, searchKey, searchValue, pageNumber, pageSize);
    }
}
