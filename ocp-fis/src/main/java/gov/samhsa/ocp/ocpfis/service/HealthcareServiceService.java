package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.HealthcareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface HealthcareServiceService {
    PageDto<HealthcareServiceDto> getAllHealthcareServices(Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    PageDto<HealthcareServiceDto> getAllHealthcareServicesByOrganization(String organizationResourceId, Optional<String> assignedToLocationId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    PageDto<HealthcareServiceDto> getAllHealthcareServicesByLocation(String organizationResourceId, String locationId, Optional<List<String>> statusList, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> page, Optional<Integer> size);

    HealthcareServiceDto getHealthcareService(String healthcareServiceId);

    void createHealthcareService(String organizationId, HealthcareServiceDto healthcareServiceDto, Optional<String> loggedInUser);

    void updateHealthcareService(String organizationId, String healthcareServiceId, HealthcareServiceDto healthcareServiceDto, Optional<String> loggedInUser);

    void inactivateHealthcareService(String healthcareServiceId);

    void assignLocationsToHealthcareService(String healthcareServiceId, String organizationResourceId, List<String> locationIdList);

    void unassignLocationsFromHealthcareService(String healthcareServiceId, String organizationResourceId, List<String> locationIdList);
}
