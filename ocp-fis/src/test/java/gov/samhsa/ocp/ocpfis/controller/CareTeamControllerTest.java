package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.web.CareTeamController;
import org.hamcrest.CoreMatchers;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RunWith(SpringRunner.class)
@WebMvcTest(value = CareTeamController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")
public class CareTeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CareTeamService careTeamService;

    @Test
    public void testGetCareTeamByValue() throws Exception {
        //Arrange
        CareTeamDto dto = createCareTeamDto();
        List<CareTeamDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Mockito.when(careTeamService.getCareTeams(Mockito.any(Optional.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(Optional.class), Mockito.any(Optional.class))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/care-teams/search?searchValue=1913&searchType=patientId&pageNumber=1&pageSize=10&statusList=active");

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
    }

    @Test
    public void testGetCareTeamById() throws Exception {
        //Arrange
        CareTeamDto dto = createCareTeamDto();
        Mockito.when(careTeamService.getCareTeamById(Mockito.anyString())).thenReturn(dto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/care-teams/101").accept(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("101"));
    }

    @Test
    public void testCreateCareTeam() {
        //Arrange
        CareTeamDto dto = new CareTeamDto();
        doNothing().when(careTeamService).createCareTeam(isA(CareTeamDto.class));

        //Act
        careTeamService.createCareTeam(dto);

        //Assert
        verify(careTeamService, times(1)).createCareTeam(dto);
    }

    @Test
    public void testUpdateCareTeam() {
        //Arrange
        CareTeamDto dto = new CareTeamDto();
        doNothing().when(careTeamService).updateCareTeam(isA(String.class), isA(CareTeamDto.class));

        //Act
        careTeamService.updateCareTeam("101", dto);

        //Assert
        verify(careTeamService, times(1)).updateCareTeam("101", dto);
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
}
