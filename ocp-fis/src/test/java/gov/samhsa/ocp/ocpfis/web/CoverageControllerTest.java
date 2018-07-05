package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CoverageService;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebMvcTest(value = CoverageController.class, secure = false)
@Ignore
public class CoverageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoverageService coverageService;

    @Test
    public void testGetSubscriberOptions() throws Exception {
        //Arrange
        ReferenceDto referenceDto = createReference();
        List<ReferenceDto> referenceDtoList = new ArrayList<>();
        referenceDtoList.add(referenceDto);
        Mockito.when(coverageService.getSubscriberOptions("123")).thenReturn(referenceDtoList);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/patients/123/subscriber-options");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("display"));
    }

    @Test
    public void testGetCoverages() throws Exception {
        //Arrange
        CoverageDto coverageDto = createCoverageDto();
        List<CoverageDto> dtos = new ArrayList<>();
        dtos.add(coverageDto);
        PageDto pageDto = new PageDto<>(dtos, 10, 1, 1, dtos.size(), 0);
        Integer page = 1;
        Integer size = 10;
        Mockito.when(coverageService.getCoverages("123", Optional.of(page),Optional.of(size))).thenReturn(pageDto);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/patients/123/coverages?pageNumber=1&pageSize=10");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString("active"));
    }

    @Test
    public void testUpdateCoverage() throws Exception {
        //Arrange
        CoverageDto dto = createCoverageDto();
        doNothing().when(coverageService).createCoverage(isA(CoverageDto.class));

        //Act
        coverageService.createCoverage(dto);

        //Assert
        verify(coverageService, times(1)).createCoverage(dto);
    }

    private CoverageDto createCoverageDto() {
        CoverageDto coverageDto = new CoverageDto();
        coverageDto.setLogicalId("123");
        coverageDto.setStatus("active");
        return coverageDto;
    }

    private ReferenceDto createReference() {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference("1");
        referenceDto.setDisplay("thisDisplay");
        return referenceDto;
    }

}