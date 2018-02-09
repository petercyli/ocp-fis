package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.web.RelatedPersonController;

import java.util.Optional;

public interface RelatedPersonService {

    PageDto<RelatedPersonDto> searchRelatedPersons(RelatedPersonController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    RelatedPersonDto getRelatedPersonById(String id);

}
