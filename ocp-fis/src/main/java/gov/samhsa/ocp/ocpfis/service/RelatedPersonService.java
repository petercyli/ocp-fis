package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;

import java.util.Optional;

public interface RelatedPersonService {

    PageDto<RelatedPersonDto> searchRelatedPersons(String patientId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize, Optional<Boolean> showAll);

    RelatedPersonDto getRelatedPersonById(String id);

    void createRelatedPerson(RelatedPersonDto relatedPersonDtoDto, Optional<String> loggedInUser);

    void updateRelatedPerson(String relatedPersonId, RelatedPersonDto relatedPersonDto, Optional<String> loggedInUser);

}
