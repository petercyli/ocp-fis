package gov.samhsa.ocp.ocpfis.controller;

import gov.samhsa.ocp.ocpfis.service.EpisodeOfCareService;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.web.EpisodeOfCareController;
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

@RunWith(SpringRunner.class)
@WebMvcTest(value = EpisodeOfCareController.class, secure = false)
@Ignore
public class EpisodeOfCareControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EpisodeOfCareService episodeOfCareService;

    private final String REFERENCE_TASK_JSON = "[{\"reference\":\"1\",\"display\":\"display\"}]";

    @Test
    public void testGetEpisodeOfCare() throws Exception {
        //Arrange
        ReferenceDto referenceDto = createReference();
        List<ReferenceDto> referenceDtoList = new ArrayList<>();
        referenceDtoList.add(referenceDto);
        Mockito.when(episodeOfCareService.getEpisodeOfCaresForReference("12",Optional.empty(),Optional.empty())).thenReturn(referenceDtoList);

        //Act
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get("/episode-of-cares?patient=12");
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        //Assert
        Assert.assertThat(result.getResponse().getContentAsString(), CoreMatchers.containsString(REFERENCE_TASK_JSON));
    }

    private ReferenceDto createReference() {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference("1");
        referenceDto.setDisplay("display");
        return referenceDto;
    }
}