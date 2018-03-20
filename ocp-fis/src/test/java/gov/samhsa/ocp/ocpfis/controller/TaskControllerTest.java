package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.TaskService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import gov.samhsa.ocp.ocpfis.web.TaskController;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@WebMvcTest(value = TaskController.class, secure = false)
@Ignore
public class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private LookUpService lookUpService;

    private final String TASK_JSON = "[{\"logicalId\":\"1\",\"definition\":null,\"partOf\":null,\"status\":null,\"intent\":null,\"priority\":null,\"description\":\"This is description\",\"beneficiary\":null,\"context\":null,\"executionPeriod\":null,\"authoredOn\":null,\"lastModified\":null,\"agent\":null,\"performerType\":null,\"owner\":null,\"note\":\"note\",\"organization\":null}]";

    private final String REFERENCE_TASK_JSON = "[{\"reference\":\"1\",\"display\":\"display\"}]";

    @Test
    public void testGetRelatedTasks() throws Exception {
        //Arrange
        ReferenceDto referenceDto = createReference();
        List<ReferenceDto> referenceDtoList = new ArrayList<>();
        referenceDtoList.add(referenceDto);
        Mockito.when(taskService.getRelatedTasks("12", Optional.empty())).thenReturn(referenceDtoList);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/tasks/task-references?patient=12");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(REFERENCE_TASK_JSON));
    }

    @Test
    public void testGetUpcomingTasks() throws Exception {
        //Arrange
        TaskDto taskDto = createTask();
        PageDto<TaskDto> taskDtoList = new PageDto<>(Arrays.asList(taskDto), 1, 1, 1, 1, 1);
        Mockito.when(taskService.getUpcomingTasksByPractitioner("234", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())).thenReturn(taskDtoList);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/tasks?practitioner=234");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(TASK_JSON));
    }

    private TaskDto createTask() {
        TaskDto taskDto = new TaskDto();
        taskDto.setLogicalId("1");
        taskDto.setDescription("This is description");
        taskDto.setNote("note");
        return taskDto;
    }

    private ReferenceDto createReference() {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference("1");
        referenceDto.setDisplay("display");
        return referenceDto;
    }
}
