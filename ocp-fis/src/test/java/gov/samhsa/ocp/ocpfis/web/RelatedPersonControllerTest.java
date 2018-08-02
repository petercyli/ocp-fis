package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.RelatedPersonService;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.web.RelatedPersonController;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = RelatedPersonController.class, secure = false)
@Ignore
public class RelatedPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RelatedPersonService relatedPersonService;

    @MockBean
    private LookUpService lookUpService;

    private final String RELATED_PERSON_ID = "101";
    private final String RELATED_PERSON_JSON = "{\"identifierType\":\"2.16.840.1.113883.4.3.55\",\"identifierValue\":\"552348413\",\"active\":\"true\",\"patient\":\"2421\",\"relationshipCode\":\"FTH\",\"relationshipValue\":\"father\",\"firstName\":\"Manoj\",\"lastName\":\"Raj\",\"telecomCode\":\"phone\",\"telecomUse\":\"Home\",\"telecomValue\":\"4103706987\",\"genderCode\":\"female\",\"genderValue\":\"Female\",\"birthDate\":\"12/12/1982\",\"address1\":\"201 Maint St\",\"address2\":\"Apt 101\",\"city\":\"Seattle\",\"state\":\"WA\",\"zip\":\"98052\",\"country\":\"USA\",\"startDate\":\"12/12/2016\",\"endDate\":\"12/12/2017\"}";

    @Test
    public void testMethod_Given_RelatedPersonsAvailable_When_RequestedWithTypeAndValue_Then_ReturnListOfRelatedPersons() throws Exception {
        //Arrange
        RelatedPersonDto dto = createRelatedPersonDto();
        List<RelatedPersonDto> dtos = new ArrayList<>();
        dtos.add(dto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Integer pageNumber = 1;
        Integer pageSize = 10;
        //faced an issue passing actual values of searchKey and searchValue
        Mockito.when(relatedPersonService.searchRelatedPersons(eq("1965"), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class), Mockito.any(Optional.class),Mockito.any(Optional.class))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/related-persons/search?patientId=1965&showInactive=true&pageNumber=1&pageSize=10");
        //PageDto<RelatedPersonDto> list = relatedPersonService.searchRelatedPersons("1965", Optional.of("relatedPerson"), Optional.of("shyam"), Optional.of(true), Optional.of(pageNumber), Optional.of(pageSize));

        //Assert
        mockMvc.perform(requestBuilder).andExpect((ResultMatcher) jsonPath("$.hasElements", CoreMatchers.is(true)));
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(RELATED_PERSON_ID));
    }

    @Test
    public void testMethod_Given_RelatedPersonsAvailable_When_RequestedWithRelatedPersonId_Then_ReturnListOfRelatedPersons() throws Exception {
        //Arrange
        RelatedPersonDto dto = createRelatedPersonDto();
        Mockito.when(relatedPersonService.getRelatedPersonById(RELATED_PERSON_ID)).thenReturn(dto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/related-persons/" + RELATED_PERSON_ID);
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(RELATED_PERSON_ID));
    }

    @Test
    public void testMethod_Given_ARelatedPersonDto_When_PostedWithValidJson_Then_CreateRelatedPerson() throws Exception {
        //Arrange
        doNothing().when(relatedPersonService).createRelatedPerson(isA(RelatedPersonDto.class), Mockito.any(Optional.class));
        setUpLookups();

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/related-persons").content(RELATED_PERSON_JSON).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isCreated());
    }

    @Test
    public void testMethod_Given_ARelatedPersonDtoWithId_When_PutWithValidJson_Then_UpdateRelatedPerson() throws Exception {
        //Arrange
        doNothing().when(relatedPersonService).updateRelatedPerson(isA(String.class), isA(RelatedPersonDto.class), Mockito.any(Optional.class));
        setUpLookups();

        //Act
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/related-persons/" + RELATED_PERSON_ID).content(RELATED_PERSON_JSON).contentType(APPLICATION_JSON_UTF8);
        ResultActions resultAction = mockMvc.perform(requestBuilder);

        //Assert
        resultAction.andExpect(status().isOk());
    }

    private RelatedPersonDto createRelatedPersonDto() {
        RelatedPersonDto dto = new RelatedPersonDto();
        dto.setRelatedPersonId(RELATED_PERSON_ID);
        dto.setIdentifierType("2.16.840.1.113883.4.3.55");
        dto.setIdentifierValue("155234841");
        dto.setActive(true);
        dto.setPatient("1965");
        dto.setRelationshipCode("FTH");
        dto.setRelationshipValue("father");
        dto.setFirstName("Shya");
        dto.setLastName("Ajk");
        dto.setGenderCode("male");
        dto.setGenderValue("Male");
        dto.setBirthDate("12/12/1986");
        dto.setStartDate("12/12/2017");
        dto.setEndDate("12/30/2017");
        return dto;
    }

    private List<IdentifierSystemDto> getIdentifierSytems() {
        IdentifierSystemDto dto = new IdentifierSystemDto();
        dto.setOid("2.16.840.1.113883.4.3.55");
        dto.setDisplay("SomeDisplay");
        return Arrays.asList(dto);
    }

    private List<ValueSetDto> getRelationshipTypes() {
        ValueSetDto dto = new ValueSetDto();
        dto.setCode("FTH");
        dto.setDisplay("father");
        return Arrays.asList(dto);
    }

    private List<ValueSetDto> getTelecoms() {
        ValueSetDto dto = new ValueSetDto();
        dto.setCode("phone");
        dto.setDisplay("Phone");
        return Arrays.asList(dto);
    }

    private List<ValueSetDto> getGenders() {
        ValueSetDto dto = new ValueSetDto();
        dto.setCode("male");
        dto.setDisplay("Male");

        ValueSetDto dto2 = new ValueSetDto();
        dto2.setCode("female");
        dto2.setDisplay("Female");

        return Arrays.asList(dto, dto2);
    }

    private List<ValueSetDto> getStates() {
        ValueSetDto dto = new ValueSetDto();
        dto.setCode("WA");
        dto.setDisplay("Washington");

        return Arrays.asList(dto);
    }

    private void setUpLookups() {
        Mockito.when(lookUpService.getIdentifierSystems(Optional.of(Arrays.asList("DL", "PPN", "TAX", "MR", "DR", "SB")))).thenReturn(getIdentifierSytems());
        Mockito.when(lookUpService.getRelatedPersonPatientRelationshipTypes()).thenReturn(getRelationshipTypes());
        Mockito.when(lookUpService.getTelecomSystems()).thenReturn(getTelecoms());
        Mockito.when(lookUpService.getAdministrativeGenders()).thenReturn(getGenders());
        Mockito.when(lookUpService.getTelecomSystems()).thenReturn(getTelecoms());
        Mockito.when(lookUpService.getUspsStates()).thenReturn(getStates());

    }

}
