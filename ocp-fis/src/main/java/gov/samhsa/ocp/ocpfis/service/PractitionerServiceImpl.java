package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;

import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

        List<PractitionerDto> list=new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> retrievedPractitioners = bundle.getEntry();

        return retrievedPractitioners.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());

    }

}
