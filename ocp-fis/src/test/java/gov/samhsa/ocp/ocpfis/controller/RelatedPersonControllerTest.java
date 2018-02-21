package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.RelatedPersonService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.web.RelatedPersonController;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = RelatedPersonController.class, secure = false)
public class RelatedPersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RelatedPersonService relatedPersonService;

    @Test
    public void testMethod_Given_RelatedPersonsAvailable_When_RequestedWithTypeAndValue_Then_ReturnListOfRelatedPersons() throws Exception {

    }

    @Test
    public void testMethod_Given_RelatedPersonsAvailable_When_RequestedWithRelatedPersonId_Then_ReturnListOfRelatedPersons() throws Exception {

    }

    @Test
    public void testMethod_Given_ARelatedPersonDto_When_PostedWithValidJson_Then_CreateRelatedPerson() throws Exception {

    }

    @Test
    public void testMethod_Given_ARelatedPersonDtoWithId_When_PutWithValidJson_Then_UpdateRelatedPerson() throws Exception {

    }

    private RelatedPersonDto createRelatedPersonDto() {
        RelatedPersonDto dto = new RelatedPersonDto();
        dto.setIdentifierType("2.16.840.1.113883.4.3.55");
        dto.setIdentifierType("155234841");
        dto.setActive(true);
        dto.setPatient("1965");
        dto.setRelationshipCode("FTH");
        dto.setRelationshipValue("father");
        dto.setFirstName("Shya");
        dto.setLastName("Ajk");
        dto.setTelecomUse("phone");
        dto.setTelecomUse("home");
        dto.setTelecomValue("4101472589");
        dto.setGenderCode("male");
        dto.setGenderValue("Male");
        dto.setBirthDate("12/12/1986");
        dto.setAddress1("100 Main St");
        dto.setAddress2("Apt 100");
        dto.setCity("Ellicott City");
        dto.setState("MD");
        dto.setZip("21044");
        dto.setCountry("USA");
        dto.setStartDate("12/12/2017");
        dto.setEndDate("12/30/2017");
        return dto;
    }

}
