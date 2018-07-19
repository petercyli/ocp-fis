package gov.samhsa.ocp.ocpfis.util;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Reference;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Service
public class ProvenanceUtil {

    private final IGenericClient fhirClient;

    public ProvenanceUtil(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public void createProvenance(String id, ProvenanceActivityEnum provenanceActivityEnum, Optional<String> loggedInUser) {
        Provenance provenance = new Provenance();

        //target
        Reference reference = new Reference();
        reference.setReference(id);
        provenance.setTarget(Arrays.asList(reference));

        //recorded : When the activity was recorded/ updated
        provenance.setRecorded(new Date());

        //activity
        Coding coding = new Coding();
        coding.setCode(provenanceActivityEnum.toString());
        provenance.setActivity(coding);

        //agent.whoReference
        Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        Reference whoRef = new Reference();
        if (loggedInUser.isPresent()) {
            whoRef.setReference(loggedInUser.get());
        } else {
            whoRef.setReference("NA");
        }

        agent.setWho(whoRef);

        provenance.setAgent(Arrays.asList(agent));

        fhirClient.create().resource(provenance).execute();
    }
}
