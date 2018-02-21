package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;

public interface TaskService {
    void createTask(TaskDto taskDto);
}
