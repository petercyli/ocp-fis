package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface HealthCareServiceService {
    PageDto<HealthCareServiceDto> getAllHealthCareServices(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    PageDto<HealthCareServiceDto> getAllHealthCareServicesByOrganization(String organizationResourceId, Optional<String> locationResourceId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);
}
