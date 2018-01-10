package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService{

    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;

    public OrganizationServiceImpl(ModelMapper modelMapper , IGenericClient fhirClient) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
    }

    @Override
    public List<OrganizationDto> getAllOrganizations(Optional<String> name) {

        IQuery allOrganizationsSearchQuery = fhirClient.search().forResource(Organization.class);

        if (name.isPresent())
            allOrganizationsSearchQuery.where(new StringClientParam("name").matches().value(name.get()));

        Bundle allOrganizationsSearchBundle= (Bundle) allOrganizationsSearchQuery.returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (allOrganizationsSearchBundle == null || allOrganizationsSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server");
        }
        log.info("FHIR Organization(s) bundle retrieved from FHIR server successfully");
        List<Bundle.BundleEntryComponent> retrievedOrganizations = allOrganizationsSearchBundle.getEntry();

        return retrievedOrganizations.stream().map(bundleEntryComponent -> {
            OrganizationDto organizationDto = modelMapper.map(bundleEntryComponent.getResource(), OrganizationDto.class);
            organizationDto.setId(bundleEntryComponent.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(Collectors.toList());
    }

}
