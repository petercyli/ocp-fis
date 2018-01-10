package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;

import gov.samhsa.ocp.ocpfis.config.OcpProperties;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.exception.PractitionerNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PractitionerServiceImpl implements  PractitionerService{

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    @Autowired
    public PractitionerServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
    }

    @Override
    public List<PractitionerDto> getAllPractitioners() {
        Bundle bundle = fhirClient.search().forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .execute();

        if(bundle==null || bundle.isEmpty() || bundle.getEntry().size()<1){
            throw new PractitionerNotFoundException("No practitioners were found in the FHIR server.");
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = bundle.getEntry();

        return retrievedPractitioners.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());

    }

    @Override
    public List<PractitionerDto> searchPractitioners(String value) {
        Bundle bundleByName = fhirClient.search().forResource(Practitioner.class)
                .where(new StringClientParam("name").matches().value(value))
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> retrievedPractitionersByName = bundleByName.getEntry();

        List<PractitionerDto> practitionersByName= retrievedPractitionersByName.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());

        Bundle bundleByIdentifier=fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("identifier").exactly().code(value))
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> retrievedPractitionersByIdentifier=bundleByIdentifier.getEntry();

        List<PractitionerDto> practitionerDtoById=retrievedPractitionersByIdentifier.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());

        practitionersByName.addAll(practitionerDtoById);

        if(practitionersByName.size()<1 || practitionersByName.isEmpty() || practitionersByName.isEmpty()){
            throw new PractitionerNotFoundException("No practitioners with the given name or id value were found in the FHIR server.");
        }

        return practitionersByName;

     }

}
