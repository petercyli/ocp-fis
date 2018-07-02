package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.service.AppointmentService;
import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;;
import gov.samhsa.ocp.ocpfis.web.AppointmentController;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = AppointmentController.class, secure = false)
@Ignore
public class AppointmentControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    private static final String APPOINTMENT = "{\"patientName\":\"name\",\"description\":\"thisDescription\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private LookUpService lookUpService;

    @Test
    public void testGetAppointments() throws Exception {
        //Arrange
        AppointmentDto dto = createAppointmentDto();
        List<AppointmentDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos,10,1,1,dtos.size(),1);
        List<String> statusList = Arrays.asList("active");
        Integer page = 1;
        Integer size = 10;
        String reference = "10";
        String patientId= "10";
        String practitionerId = "10";
        String searchKey = "10";
        String searchValue = "10";
        Boolean pastAppointments = true;
        String date = "10";
        Boolean start = true;
        Mockito.when(appointmentService.getAppointments(Optional.of(statusList),Optional.of(reference),Optional.of(patientId),Optional.of(practitionerId),Optional.of(searchKey),
                Optional.of(searchValue),Optional.of(pastAppointments),Optional.of(date),Optional.of(start),Optional.of(page),Optional.of(size))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/appointments/search?requesterReference=10&patientId=10&practitionerId=10&searchKey=10&searchValue=10&showPastAppointments=true&filterDateOption=10&sortByStartTimeAsc=true&pageNumber=1&pageSize=10&statusList=active");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("patient name"));
    }

    @Test
    public void testGetAppointmentsWithNoPagination() throws Exception {
        //Arrange
        AppointmentDto dto = createAppointmentDto();
        List<AppointmentDto> dtos = new ArrayList<>();
        dtos.add(dto);
        List<String> statusList = Arrays.asList("active");
        String patientId= "10";
        String practitionerId = "10";
        String searchKey = "10";
        String searchValue = "10";
        Boolean pastAppt = true;
        Boolean sortStartTime = true;
        Mockito.when(appointmentService.getAppointmentsWithNoPagination(Optional.of(statusList),Optional.of(patientId),Optional.of(practitionerId),Optional.of(searchKey),
                Optional.of(searchValue),Optional.of(pastAppt), Optional.of(sortStartTime))).thenReturn(dtos);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/appointments/search-with-no-pagination?patientId=10&practitionerId=10&searchKey=10&searchValue=10&showPastAppointments=true&sortByStartTimeAsc=true&statusList=active");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("patient name"));
    }

    @Test
    public void testCreateAppointment() throws Exception {
        //Arrange
        doNothing().when(appointmentService).createAppointment(isA(AppointmentDto.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/appointments").content(APPOINTMENT).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isCreated());
    }

    @Test
    public void testUpdateAppointment() throws Exception {
        //Arrange
        doNothing().when(appointmentService).updateAppointment(isA(String.class), isA(AppointmentDto.class));

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/appointments/101").content(APPOINTMENT).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isOk());
    }

    private AppointmentDto createAppointmentDto() {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientName("patient name");
        dto.setDescription("this is description");
        return dto;
    }

}