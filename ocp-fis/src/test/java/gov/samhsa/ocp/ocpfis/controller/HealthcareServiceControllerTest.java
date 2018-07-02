package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.HealthcareServiceService;
import gov.samhsa.ocp.ocpfis.service.dto.HealthcareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.web.CareTeamController;
import gov.samhsa.ocp.ocpfis.web.HealthcareServiceController;
import org.hamcrest.CoreMatchers;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = HealthcareServiceController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")



public class HealthcareServiceControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private static final String HEALTHCARESERVICE = "{\"organizationId\":\"orgId\",\"organizationName\":\"orgName\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthcareServiceService healthcareService;

    @Test
    public void testGetAllHealthcareServices() throws Exception {
        //Arrange
        HealthcareServiceDto dto = createHealthcareServiceDto();
        List<HealthcareServiceDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<> (dtos, 10, 1, 1, dtos.size(), 1);

        List<String> statusList = Arrays.asList("active");
        String searchKey = "123";
        String searchValue = "456";
        Integer pageNumber = 23;
        Integer pageSize = 5;
        Mockito.when(healthcareService.getAllHealthcareServices(Optional.of(statusList), Optional.of(searchKey), Optional.of(searchValue), Optional.of(pageNumber), Optional.of(pageSize))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/healthcare-services?statusList=active&searchKey=123&searchValue=456&pageNumber=23&pageSize=5");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        //mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("123"));

    }

    @Test
    public void testGetAllHealthcareServicesByOrganization() throws Exception {
        //Arrange
        HealthcareServiceDto dto = createHealthcareServiceDto();
        List<HealthcareServiceDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<> (dtos, 10, 1, 1, dtos.size(), 1);

        String organizationId = "123";
        String assignedToLocationId = "4567";
        List<String> statusList = Arrays.asList("active");
        String searchKey = "5";
        String searchValue = "6";
        Integer pageNumber = 24;
        Integer pageSize = 1;
        Mockito.when(healthcareService.getAllHealthcareServicesByOrganization(organizationId, Optional.of(assignedToLocationId), Optional.of(statusList), Optional.of(searchKey), Optional.of(searchValue), Optional.of(pageNumber), Optional.of(pageSize))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/organizations/123/healthcare-services?organizationId=1234&assignedToLocationId=4567&statusList=active&searchKey=5&searchValue=6&pageNumber=24&pageSize=1");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("123"));
    }

    @Test
    public void testCreateHealthcareService() throws Exception {
        //Arrange
        doNothing().when(healthcareService).createHealthcareService(isA(String.class), isA(HealthcareServiceDto.class));
        String organizationId = "123";

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/organization/123/healthcare-services").content(HEALTHCARESERVICE).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Assert
        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void testUpdateHealthcareService() throws Exception {
        //Arrange
        doNothing().when(healthcareService).updateHealthcareService(isA(String.class), isA(String.class), isA(HealthcareServiceDto.class));
        String organizationId = "123";
        String healthcareServiceId = "456";

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/organization/123/healthcare-services/456").content(HEALTHCARESERVICE).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isOk());

    }

    private HealthcareServiceDto createHealthcareServiceDto() {
        HealthcareServiceDto dto = new HealthcareServiceDto();
        dto.setOrganizationId("123");
        dto.setOrganizationName("abc");
        return dto;
    }

}