package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/practitioners")
public class PractitionerController {
    public enum SearchType {
        identifier, name
    }

    @Autowired
    private PractitionerService practitionerService;

    @GetMapping("/search")
    public PageDto<PractitionerDto> searchPractitioners(@RequestParam Optional<SearchType> searchType, @RequestParam Optional<String> searchValue,Optional<String> organization, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size,Optional<Boolean> showAll) {
        return practitionerService.searchPractitioners(searchType, searchValue, organization, showInactive, page, size,showAll);
    }

    @GetMapping("/find")
    public PractitionerDto findPractitioner(@RequestParam Optional<String> organization, @RequestParam String firstName, @RequestParam Optional<String> middleName, @RequestParam String lastName, String identifierType, String identifier){
        return practitionerService.findPractitioner(organization,firstName,middleName,lastName,identifierType,identifier);
    }

    @GetMapping("/practitioner-references")
    public List<ReferenceDto> getPractitionersInOrganizationByPractitionerId(@RequestParam Optional<String> practitioner,@RequestParam Optional<String> organization, @RequestParam Optional<String> location, @RequestParam Optional<String> role) {
        return practitionerService.getPractitionersInOrganizationByPractitionerId(practitioner,organization,location, role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createPractitioner(@Valid @RequestBody PractitionerDto practitionerDto, Optional<String> loggedInUser) {
        practitionerService.createPractitioner(practitionerDto, loggedInUser);
    }

    @GetMapping("/{practitionerId}")
    public PractitionerDto getPractitioner(@PathVariable String practitionerId) {
        return practitionerService.getPractitioner(practitionerId);
    }

    @PutMapping("/{practitionerId}")
    @ResponseStatus(HttpStatus.OK)
    public void updatePractitioner(@PathVariable String practitionerId, @Valid @RequestBody PractitionerDto practitionerDto, Optional<String> loggedInUser) {
        practitionerService.updatePractitioner(practitionerId, practitionerDto, loggedInUser);
    }

    @GetMapping
    public PageDto<PractitionerDto> getPractitionersByOrganizationAndRole(@RequestParam String organization, @RequestParam Optional<String> role,@RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return practitionerService.getPractitionersByOrganizationAndRole(organization, role, page, size);
    }

    @GetMapping("/practitioner-id")
    public String getPractitionerId(@RequestParam String practitionerName){
        return practitionerService.getPractitionerByName(practitionerName);
    }

    @PutMapping("/{practitionerId}/assign")
    public void assignLocationsToPractitioner(@PathVariable String practitionerId,
                                              @RequestParam(value="organizationId") String organizationId,
                                              @RequestParam(value="locationId") String locationId){
        practitionerService.assignLocationToPractitioner(practitionerId, organizationId, locationId);
    }

    @PutMapping("/{practitionerId}/unassign")
    public void unassignLocationToPractitioner(@PathVariable String practitionerId,
                                                 @RequestParam(value="organizationId") String organizationId,
                                                 @RequestParam(value="locationId") String locationId){
        practitionerService.unassignLocationToPractitioner(practitionerId,organizationId,locationId);
    }


}