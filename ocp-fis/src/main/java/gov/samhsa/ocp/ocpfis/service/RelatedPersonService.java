package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.web.RelatedPersonController;

import java.util.Optional;

public interface RelatedPersonService {

    PageDto<RelatedPersonDto> searchRelatedPersons(RelatedPersonController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    RelatedPersonDto getRelatedPersonById(String id);

    void createRelatedPerson(RelatedPersonDto relatedPersonDtoDto);

    void updateRelatedPerson(String relatedPersonId, RelatedPersonDto relatedPersonDto);

}
