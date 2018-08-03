package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.lang.ref.Reference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = PractitionerController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")
public class PractitionerControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private static final String PRACTITIONER = "{\"logicalId\":\"123\",\"uaaRole\":\"thisRole\"}";

    private final String REFERENCE = "[{\"reference\":\"123\",\"display\":\"thisDisplay\"}]";


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PractitionerService practitionerService;

    @Test
    public void testGetPractitionersInOrganizationByPractitionerId() throws Exception {
        //Arrange
        ReferenceDto referenceDto = createReference();
        List<ReferenceDto> referenceDtoList = new ArrayList<>();
        referenceDtoList.add(referenceDto);
        Mockito.when(practitionerService.getPractitionersInOrganizationByPractitionerId(Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty())).thenReturn(referenceDtoList);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/practitioners/practitioner-references");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(REFERENCE));
    }

    @Test
    public void testCreatePractitioner() throws Exception {
        //Arrange
        doNothing().when(practitionerService).createPractitioner(isA(PractitionerDto.class), Mockito.any(Optional.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/practitioners").content(PRACTITIONER).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Assert
        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void testUpdatePractitioner() throws  Exception {
        //Arrange
        doNothing().when(practitionerService).updatePractitioner(isA(String.class), isA(PractitionerDto.class), Mockito.any(Optional.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/practitioners/101").content(PRACTITIONER).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Assert
        resultActions.andExpect(status().isOk());
    }

    private ReferenceDto createReference() {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference("123");
        referenceDto.setDisplay("thisDisplay");
        return referenceDto;
    }
}