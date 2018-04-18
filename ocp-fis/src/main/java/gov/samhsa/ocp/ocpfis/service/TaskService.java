package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.DateRangeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    PageDto<TaskDto> getTasks(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    List<TaskDto> getMainAndSubTasks(Optional<String> practitioner, Optional<String> patient, Optional<String> organization, Optional<String> definition, Optional<String> partOf, Optional<Boolean> isUpcomingTasks, Optional<DateRangeEnum> filterDate);

    void createTask(TaskDto taskDto);

    void updateTask(String taskId, TaskDto taskDto);

    void deactivateTask(String taskId);

    TaskDto getTaskById(String taskId);

    List<ReferenceDto> getRelatedTasks(String patient,Optional<String> definition, Optional<String> practitioner, Optional<String> organization);

    PageDto<TaskDto> getUpcomingTasksByPractitioner(@RequestParam(value = "practitioner") String practitioner,
                                                           @RequestParam(value = "searchKey") Optional<String> searchKey,
                                                           @RequestParam(value = "searchValue") Optional<String> searchValue,
                                                           @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                           @RequestParam(value = "pageSize") Optional<Integer> pageSize);
}
