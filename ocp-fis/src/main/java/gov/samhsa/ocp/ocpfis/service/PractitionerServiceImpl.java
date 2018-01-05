package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;

import gov.samhsa.ocp.ocpfis.config.OcpProperties;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PractitionerServiceImpl implements  PractitionerService{

    private FhirContext fhirContext;

    private Map<Class<? extends Resource>, IGenericClient> fhirClients;

    private IParser fhirJsonParser;

    private FhirValidator fhirValidator;

    private OcpProperties ocpProperties;

    private IGenericClient practitionerFhirClient;

    @Autowired
    public PractitionerServiceImpl(FhirContext fhirContext, FhirValidator fhirValidator, Map<Class<? extends Resource>, IGenericClient> fhirClients, OcpProperties ocpProperties) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirValidator;
        this.fhirClients = fhirClients;
        this.ocpProperties = ocpProperties;
        this.practitionerFhirClient = fhirClients.getOrDefault(Practitioner.class, fhirClients.get(Resource.class));
    }

    @Override
    public List<PractitionerDto> readPractitioners() {

        List<PractitionerDto> list=new ArrayList<>();

        Bundle bundle = practitionerFhirClient.search().forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .execute();

        for(int i=0;i<bundle.getEntry().size();i++) {
            Practitioner practitioner = (Practitioner) bundle.getEntry().get(i).getResource();

            PractitionerDto practitionerDto=new PractitionerDto();
            practitionerDto.setFamilyName((practitioner.getName().isEmpty()) ?"": practitioner.getName().get(0).getFamily());

            list.add(practitionerDto);
        }

        return list;

    }
}
