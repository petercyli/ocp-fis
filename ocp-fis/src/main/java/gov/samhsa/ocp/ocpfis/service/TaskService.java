package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    PageDto<TaskDto> getTasks(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createTask(TaskDto taskDto);

    void updateTask(String taskId, TaskDto taskDto);

    void deactivateTask(String taskId);

    TaskDto getTaskById(String taskId);

    List<ReferenceDto> getRelatedTasks(String patient);

    PageDto<TaskDto> getUpcomingTasksByPractitionerAndRole(@RequestParam(value = "practitioner") String practitioner,
                                                           @RequestParam(value = "role") String role,
                                                           @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                           @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                           @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                           @RequestParam(value = "pageSize") Optional<Integer> pageSize);
}
