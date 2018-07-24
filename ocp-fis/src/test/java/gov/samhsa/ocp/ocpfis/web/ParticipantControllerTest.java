package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.ParticipantService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantMemberDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantSearchDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.web.ParticipantController;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.eq;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ParticipantController.class, secure = false)
@Ignore
public class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParticipantService participantService;

    private final String ORGANIZATION_NAME = "OrgName";
    private final String PATIENT_NAME = "PatientName";
    private final String RELATED_PERSON_NAME = "RelatedPersonName";

    /**
     * Tests participants of type relatedPerson
     *
     * @throws Exception
     */
    @Test
    public void testMethod_Given_RelatedPersonsAvailable_When_RequestedWithTypeAndValueForAPatient_Then_ReturnListOfParticipants() throws Exception {
        //Arrange
        ParticipantSearchDto dto = createRelatedPersonParticipantDto();
        List<ParticipantSearchDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Integer page = 1;
        Integer size = 10;
        Mockito.when(participantService.getAllParticipants(eq("2421"), eq(ParticipantTypeEnum.relatedPerson), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class),Mockito.any(Optional.class),Mockito.any(Optional.class))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/participants/search?member=relatedPerson&patientId=2421&value=" + RELATED_PERSON_NAME);

        //Assert
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("relatedPerson"));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("201"));

    }

    /**
     * Tests participants of type organization
     *
     * @throws Exception
     */
    @Test
    public void testMethod_Given_OrganizationsAvailable_When_RequestedWithTypeAndValueForAPatient_Then_ReturnListOfParticipants() throws Exception {
        //Arrange
        ParticipantSearchDto dto = createOrganizationParticipantDto();
        List<ParticipantSearchDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Integer page = 1;
        Integer size = 10;
        Mockito.when(participantService.getAllParticipants(eq("2421"), eq(ParticipantTypeEnum.organization), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class),Mockito.any(Optional.class),Mockito.any(Optional.class))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/participants/search?member=organization&patientId=2421&value=" + ORGANIZATION_NAME);

        //Assert
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("organization"));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("101"));

    }

    /**
     * Tests participants of type patient
     *
     * @throws Exception
     */
    @Test
    public void testMethod_Given_PatientsAvailable_When_RequestedWithTypeAndValueForAPatient_Then_ReturnListOfParticipants() throws Exception {
        //Arrange
        ParticipantSearchDto dto = createPatientParticipantDto();
        List<ParticipantSearchDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Integer page = 1;
        Integer size = 10;
        Mockito.when(participantService.getAllParticipants(eq("2421"), eq(ParticipantTypeEnum.patient), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class),Mockito.any(Optional.class),Mockito.any(Optional.class))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/participants/search?member=patient&patientId=2421&value=" + PATIENT_NAME);

        //Assert
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("patient"));
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("301"));

    }

    private ParticipantSearchDto createPatientParticipantDto() {
        ParticipantSearchDto dto = new ParticipantSearchDto();
        ValueSetDto role = new ValueSetDto();

        ParticipantMemberDto participantMemberDto = new ParticipantMemberDto();
        participantMemberDto.setType("patient");
        participantMemberDto.setId("301");

        dto.setRole(role);
        dto.setMember(participantMemberDto);

        return dto;
    }

    private ParticipantSearchDto createOrganizationParticipantDto() {
        ParticipantSearchDto dto = new ParticipantSearchDto();
        ValueSetDto role = new ValueSetDto();

        ParticipantMemberDto participantMemberDto = new ParticipantMemberDto();
        participantMemberDto.setType("organization");
        participantMemberDto.setId("101");

        dto.setRole(role);
        dto.setMember(participantMemberDto);

        return dto;
    }

    private ParticipantSearchDto createRelatedPersonParticipantDto() {
        ParticipantSearchDto dto = new ParticipantSearchDto();
        ValueSetDto role = new ValueSetDto();

        ParticipantMemberDto participantMemberDto = new ParticipantMemberDto();
        participantMemberDto.setType("relatedPerson");
        participantMemberDto.setId("201");

        dto.setRole(role);
        dto.setMember(participantMemberDto);

        return dto;
    }

}
