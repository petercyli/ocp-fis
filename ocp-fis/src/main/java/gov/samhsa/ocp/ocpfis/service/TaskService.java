package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    PageDto<TaskDto> getTasks(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createTask(TaskDto taskDto);

    void updateTask(String taskId, TaskDto taskDto);
}
