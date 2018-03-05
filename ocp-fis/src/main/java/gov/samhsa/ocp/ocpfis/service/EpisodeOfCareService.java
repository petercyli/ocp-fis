package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;

import java.util.List;
import java.util.Optional;

public interface EpisodeOfCareService {

    List<EpisodeOfCareDto> getEpisodeOfCares(String patient, Optional<String> status);
}
