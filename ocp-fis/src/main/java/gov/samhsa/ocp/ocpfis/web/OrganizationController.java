package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.OrganizationService;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/organizations")

public class OrganizationController {
    public enum SearchType {
        identifier, name
    }

    @Autowired
    private OrganizationService organizationService;

   // @GetMapping
   // public List<OrganizationDto> getAllOrganization(@Valid @RequestParam(value = "name") Optional<String> name) {
   //     return organizationService.getAllOrganizations(name);
   // }

    @GetMapping
    public List<OrganizationDto> getAllOrganizations(@RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return organizationService.getAllOrganizations(showInactive, page, size);
    }

    @GetMapping("/search")
    public List<OrganizationDto> searchOrganizations(@RequestParam SearchType searchType, @RequestParam String searchValue, @RequestParam Optional<Boolean> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size) {
        return organizationService.searchOrganizations(searchType, searchValue, showInactive, page, size);
    }
}