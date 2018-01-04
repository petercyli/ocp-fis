package gov.samhsa.ocp.ocp.service;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocp.config.OcpProperties;
import gov.samhsa.ocp.ocp.service.exception.MultiplePatientsFoundException;
import gov.samhsa.ocp.ocp.service.exception.PatientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {


    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private Map<Class<? extends Resource>, IGenericClient> fhirClients;
    @Autowired
    private IParser fhirJsonParser;

    @Autowired
    private FhirValidator fhirValidator;

    @Autowired
    private OcpProperties ocpProperties;

    final String ENCODING = "UTF-8";

    public String getFhirResourceByPatientIdentifier(String patientIdentifierSystem, String patientIdentifierValue) {

        Bundle patientSearchResponse = fhirClients.getOrDefault(Patient.class, fhirClients.get(Resource.class)).search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier")
                        .exactly()
                        .systemAndCode(patientIdentifierSystem, patientIdentifierValue))
                .encodedJson()
                .returnBundle(Bundle.class)
                .execute();

        if(patientSearchResponse == null || patientSearchResponse.getEntry().size() < 1){
            log.debug("No patient found in FHIR server with the given MRN: "  + patientIdentifierSystem + "|" + patientIdentifierValue);
            throw new PatientNotFoundException("No patient found for the given MRN");
        }

        if(patientSearchResponse.getEntry().size() > 1){
            log.warn("Multiple patients were found in FHIR server for the same given MRN: " + patientIdentifierSystem + "|" + patientIdentifierValue);
            log.debug("       URL of FHIR Server: " + fhirClients.getOrDefault(Patient.class, fhirClients.get(Resource.class)).getServerBase());
            throw new MultiplePatientsFoundException("Multiple patients found in FHIR server with the given MRN");
        }

        Patient patientObj = (Patient) patientSearchResponse.getEntry().get(0).getResource();

        String patientResourceId = null;
        try {
            patientResourceId = URLEncoder.encode(patientObj.getIdElement().getIdPart(),ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Parameters outParams = fhirClients.get(Resource.class)
                .operation()
                .onInstance(new IdDt("Patient", patientResourceId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .encodedJson()
                .useHttpGet()
                .execute();
        Bundle responseBundle = (Bundle) outParams.getParameter().get(0).getResource();
        return fhirJsonParser.setPrettyPrint(true).encodeResourceToString(responseBundle);
    }

}


