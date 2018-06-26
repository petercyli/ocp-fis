package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.EpisodeOfCareService;
import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/episode-of-cares")
public class EpisodeOfCareController {

    @Autowired
    EpisodeOfCareService episodeOfCareService;

    @GetMapping
    private List<ReferenceDto> getEpisodeOfCares(@RequestParam String patient,@RequestParam Optional<String> organization, @RequestParam Optional<String> status) {
        return episodeOfCareService.getEpisodeOfCaresForReference(patient, organization, status);
    }

}
