package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PatientService;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.web.PatientController;
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
@WebMvcTest(value = PatientController.class, secure = false)
@Ignore("Depends on config-server on bootstrap")
public class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @Test
    public void testGetPatentByValue() throws Exception {
        //Arrange
        PatientDto dto = createPatientDto();
        List<PatientDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Mockito.when(patientService.getPatientsByValue(Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/patients/search?type=name&value=101");

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
    }

    @Test
    public void testGetPatientsByValue() throws Exception {
        //Arrange
        PatientDto mockPatient = createPatientDto();
        Mockito.when(patientService.getPatientById(Mockito.anyString())).thenReturn(mockPatient);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/patients/101").accept(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("101"));
    }

    @Test
    public void testCreatePatient() {
        //Arrange
        PatientDto dto = createPatientDto();
        doNothing().when(patientService).createPatient(isA(PatientDto.class), Mockito.any(Optional.class));

        //Act
        patientService.createPatient(dto, Optional.of("Practitioner/123"));

        //Assert
        verify(patientService, times(1)).createPatient(dto, Optional.of("Practitioner/123"));
    }

    @Test
    public void testUpdatePatient() {
        //Arrange
        PatientDto dto = createPatientDto();
        doNothing().when(patientService).updatePatient(isA(PatientDto.class));

        //Act
        patientService.updatePatient(dto);

        //Assert
        verify(patientService, times(1)).updatePatient(dto);
    }

    private PatientDto createPatientDto() {
        PatientDto dto = new PatientDto();
        IdentifierDto identifierDto = new IdentifierDto();
        identifierDto.setSystem("identifierSystem");
        identifierDto.setValue("identifierValue");

        dto.setId("101");
        dto.setActive(true);
        dto.setIdentifier(Arrays.asList(identifierDto));
        return dto;
    }

}
