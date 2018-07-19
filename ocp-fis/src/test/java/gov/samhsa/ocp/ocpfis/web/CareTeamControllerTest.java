package gov.samhsa.ocp.ocpfis.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.validation.CareTeamCategoryCodeValidator;
import gov.samhsa.ocp.ocpfis.service.validation.CareTeamStatusCodeValidator;
import gov.samhsa.ocp.ocpfis.web.CareTeamController;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

import javax.validation.ConstraintValidatorContext;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebMvcTest(value = CareTeamController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")
public class CareTeamControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private static final String CARE_TEAM = "{\"name\":\"UR Team Thursday OneFifty\",\"statusCode\":\"active\",\"categoryCode\":\"longitudinal\",\"subjectId\":\"1913\",\"startDate\":\"01/01/2018\",\"endDate\":\"02/01/2018\",\"reasonCode\":\"109006\",\"participants\":[{\"memberId\":\"1913\",\"memberType\":\"patient\",\"roleCode\":\"101Y00000X\",\"startDate\":\"01/01/2017\",\"endDate\":\"02/01/2017\"},{\"memberId\":\"1382\",\"memberType\":\"RelatedPerson\",\"roleCode\":\"101Y00000X\",\"startDate\":\"03/01/2017\",\"endDate\":\"04/01/2017\"},{\"memberId\":\"1503\",\"memberType\":\"Organization\",\"roleCode\":\"101YA0400X\",\"startDate\":\"05/01/2017\",\"endDate\":\"06/01/2017\"},{\"memberId\":\"1522\",\"memberType\":\"Practitioner\",\"roleCode\":\"101YA0400X\",\"startDate\":\"07/01/2017\",\"endDate\":\"08/01/2017\"}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CareTeamService careTeamService;

    @MockBean
    private LookUpService lookUpService;

    @MockBean
    CareTeamCategoryCodeValidator careTeamCategoryCodeValidator;

    @MockBean
    CareTeamStatusCodeValidator careTeamStatusCodeValidator;


    @Test
    public void testMethod_Given_CareTeamsAvailable_When_RequestedWithTypeAndValue_Then_ReturnListOfCareTeams() throws Exception {
        //Arrange
        CareTeamDto dto = createCareTeamDto();
        List<CareTeamDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        List<String> statusList = Arrays.asList("active");
        Integer page = 1;
        Integer size = 10;
        Mockito.when(careTeamService.getCareTeams(Optional.of(statusList), "patientId", "1913", Optional.of(page), Optional.of(size))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/care-teams/search?searchValue=1913&searchType=patientId&pageNumber=1&pageSize=10&statusList=active");

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("CareTeam 1"));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("101Y00000X"));
    }

    @Test
    public void testMethod_Given_CareTeamsAvailable_When_RequestedWithCareTeamId_Then_ReturnListOfCareTeams() throws Exception {
        //Arrange
        CareTeamDto dto = createCareTeamDto();
        Mockito.when(careTeamService.getCareTeamById("101")).thenReturn(dto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/care-teams/101").accept(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("101"));
    }

    @Test
    public void testMethod_Given_ACareTeamDto_When_PostedWithValidJson_Then_CreateCareTeam() throws Exception {
        //Arrange
        doNothing().when(careTeamService).createCareTeam(isA(CareTeamDto.class));
        setUpLookups();

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/care-teams").content(CARE_TEAM).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isCreated());
    }

    @Test
    public void testMethod_Given_ACareTeamDtoWithId_When_PutWithValidJson_Then_UpdateCareTeam() throws Exception {
        //Arrange
        doNothing().when(careTeamService).updateCareTeam(isA(String.class), isA(CareTeamDto.class));
        setUpLookups();

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/care-teams/101").content(CARE_TEAM).contentType(MediaType.APPLICATION_JSON);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isOk());
    }

    private CareTeamDto createCareTeamDto() {
        CareTeamDto dto = new CareTeamDto();
        dto.setName("CareTeam 1");
        dto.setStatusCode("active");
        dto.setCategoryCode("longitudinal");
        dto.setSubjectId("1913");
        dto.setReasonCode("109006");
        dto.setStartDate("12/12/2017");
        dto.setEndDate("01/01/2018");

        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setRoleCode("101Y00000X");
        participantDto.setStartDate("01/01/2017");
        participantDto.setEndDate("02/02/2017");
        participantDto.setMemberId("1913");
        participantDto.setMemberType(ParticipantTypeEnum.patient.getCode());

        dto.setParticipants(Arrays.asList(participantDto));

        return dto;
    }

    List<ValueSetDto> getCareTeamCategories() {
        List<ValueSetDto> valueSetDtos = new ArrayList<>();
        ValueSetDto dto = new ValueSetDto();
        dto.setCode("longitudinal");
        dto.setDisplay("Longitudinal");
        valueSetDtos.add(dto);
        return valueSetDtos;
    }

    List<ValueSetDto> getCareTeamStatuses() {
        List<ValueSetDto> valueSetDtos = new ArrayList<>();
        ValueSetDto dto = new ValueSetDto();
        dto.setCode("active");
        dto.setDisplay("Active");
        valueSetDtos.add(dto);
        return valueSetDtos;
    }

    public void setUpLookups() {
        Mockito.when(lookUpService.getCareTeamCategories()).thenReturn(getCareTeamCategories());
        Mockito.when(lookUpService.getCareTeamStatuses()).thenReturn(getCareTeamStatuses());
    }

}
