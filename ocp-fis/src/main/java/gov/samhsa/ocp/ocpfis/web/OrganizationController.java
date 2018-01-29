package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.OrganizationService;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/organizations")

public class OrganizationController {
    public enum SearchType {
        identifier, name, logicalId
    }

    @Autowired
    private OrganizationService organizationService;

    @GetMapping
    public PageDto<OrganizationDto> getAllOrganizations(@RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return organizationService.getAllOrganizations(showInactive, page, size);
    }

    @GetMapping("/search")
    public PageDto<OrganizationDto> searchOrganizations(@RequestParam SearchType searchType, @RequestParam String searchValue, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return organizationService.searchOrganizations(searchType, searchValue, showInactive, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void createOrganization(@Valid @RequestBody OrganizationDto organizationDto) {
        organizationService.createOrganization(organizationDto);
        log.info("Organization successfully created");
    }

    @PutMapping("/{organizationId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateOrganization(@PathVariable String organizationId, @Valid @RequestBody OrganizationDto organizationDto) {
        organizationService.updateOrganization(organizationId, organizationDto);
        log.info("Organization successfully updated");
    }

    @PutMapping("/{organizationId}/inactive")
    @ResponseStatus(HttpStatus.OK)
    public void inactivateOrganization(@PathVariable String organizationId) {
        organizationService.inactivateOrganization(organizationId);
    }

}