package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PractitionerService {
    List<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);
    List<PractitionerDto> searchPractitioners(PractitionerController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);
}
