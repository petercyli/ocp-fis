package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface EpisodeOfCareService {

    List<EpisodeOfCareDto> getEpisodeOfCares(String patient, Optional<String> status);

    List<ReferenceDto> getEpisodeOfCaresForReference(String patient, Optional<String> organization, Optional<String> status);

}
