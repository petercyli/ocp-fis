package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.service.ConsentService;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.DetailedConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.web.ConsentController;
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
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ConsentController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")
public class ConsentControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private static final String CONSENT = "{\"logicalId\":\"123\",\"status\":\"active\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsentService consentService;

    @Test
    public void testGetConsents() throws Exception {
        //Arrange
        DetailedConsentDto dto = createDetailedConsentDto();
        List<DetailedConsentDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos,10,1,1,dtos.size(),1);
        String patient = dto.getLogicalId();
        String practitioner = "practitioner1";
        String status = "active1";
        Boolean designation = true;
        Integer page = 1;
        Integer size = 10;
        Mockito.when(consentService.getConsents(Optional.of(patient),Optional.of(practitioner),Optional.of(status),Optional.of(designation),Optional.of(page),Optional.of(size))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/consents?patient=123&practitioner=practitioner1&status=active1&generalDesignation=true&pageNumber=1&pageSize=10");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("123"));
    }

    @Test
    public void testCreateConsent() throws Exception {
        //Arrange
        doNothing().when(consentService).createConsent(isA(ConsentDto.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/consents").content(CONSENT).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Assert
        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void testUpdateConsent() throws Exception {
        //Arrange
        doNothing().when(consentService).updateConsent(isA(String.class), isA(ConsentDto.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/consents/101").content(CONSENT).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isOk());
    }

    private DetailedConsentDto createDetailedConsentDto() {
        DetailedConsentDto dto = new DetailedConsentDto();
        dto.setLogicalId("123");
        dto.setStatus("active");
        return dto;
    }
}