package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.service.CommunicationService;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.web.CommunicationController;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebMvcTest(value = CommunicationController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")
public class CommunicationControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private static final String COMMUNICATION = "{\"logicalId\":\"123\",\"note\":\"thisNote\"}";


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommunicationService communicationService;

    @Test
    public void testGetCommunications() throws Exception {
        //Arrange
        CommunicationDto dto = createCommunicationDto();
        List<CommunicationDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos,10,1,1,dtos.size(),1);
        List<String> statusList = Arrays.asList("active");
        String organization = "organization";
        Integer page = 1;
        Integer size = 10;
        Mockito.when(communicationService.getCommunications(Optional.of(statusList),"10","10",Optional.of(organization),Optional.of(page),Optional.of(size))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/communications/search?statusList=active&searchKey=10&searchValue=10&organization=organization&pageNumber=1&pageSize=10");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("123"));
    }

    @Test
    public void testCreateCommunication() throws Exception {
        //Arrange
        doNothing().when(communicationService).createCommunication(isA(CommunicationDto.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/communications").content(COMMUNICATION).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Assert
        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void testUpdateCommunication() throws Exception {
        //Arrange
        doNothing().when(communicationService).updateCommunication(isA(String.class), isA(CommunicationDto.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/communications/101").content(COMMUNICATION).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isOk());
    }

    private CommunicationDto createCommunicationDto() {
        CommunicationDto dto = new CommunicationDto();
        dto.setLogicalId("123");
        dto.setNote("note");
        return dto;
    }
}