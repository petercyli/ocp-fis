package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService{

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    @Autowired
    public OrganizationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
    }

    /**
     * Gets all available organizations in the configured FHIR server
     *
     * @return
     */
    @Override
    public List<OrganizationDto> getAllOrganizations() {

        Bundle allOrganizationsSearchBundle = fhirClient.search().forResource(Organization.class)
                .returnBundle(Bundle.class)
                .execute();

        if (allOrganizationsSearchBundle == null || allOrganizationsSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server");
        }
        log.info("FHIR Organization(s) bundle retrieved from FHIR server successfully");
        List<Bundle.BundleEntryComponent> retrievedOrganizations = allOrganizationsSearchBundle.getEntry();

        return retrievedOrganizations.stream().map(organization -> modelMapper.map(organization.getResource(), OrganizationDto.class)).collect(Collectors.toList());
    }

}
