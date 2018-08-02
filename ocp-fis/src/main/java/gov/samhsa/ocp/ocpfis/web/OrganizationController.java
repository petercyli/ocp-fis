package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.OrganizationService;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
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
@RequestMapping("/organizations")
public class OrganizationController {
    public enum SearchType {
        identifier, name, logicalId
    }

    @Autowired
    private OrganizationService organizationService;

    // Todo: Resolve endpoint conflicts with getOrganizationsByPractitioner
    @GetMapping("all")
    public PageDto<OrganizationDto> getOrganizations(@RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return organizationService.getAllOrganizations(showInactive, page, size);
    }

    @GetMapping("/{organizationId}")
    public OrganizationDto getOrganization(@PathVariable String organizationId) {
        return organizationService.getOrganization(organizationId);
    }

    @GetMapping("/search")
    public PageDto<OrganizationDto> searchOrganizations(@RequestParam Optional<SearchType> searchType, @RequestParam Optional<String> searchValue, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size, Optional<Boolean> showAll) {
        return organizationService.searchOrganizations(searchType, searchValue, showInactive, page, size, showAll);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createOrganization(@Valid @RequestBody OrganizationDto organizationDto, Optional<String> loggedInUser) {
        organizationService.createOrganization(organizationDto, loggedInUser);
    }

    @PutMapping("/{organizationId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateOrganization(@PathVariable String organizationId, @Valid @RequestBody OrganizationDto organizationDto, Optional<String> loggedInUser) {
        organizationService.updateOrganization(organizationId, organizationDto, loggedInUser);
    }

    @PutMapping("/{organizationId}/inactive")
    @ResponseStatus(HttpStatus.OK)
    public void inactivateOrganization(@PathVariable String organizationId) {
        organizationService.inactivateOrganization(organizationId);
    }

    @GetMapping
    public List<ReferenceDto> getOrganizationsByPractitioner(@RequestParam(value = "practitioner") String practitioner) {
        return organizationService.getOrganizationsByPractitioner(practitioner);
    }
}
