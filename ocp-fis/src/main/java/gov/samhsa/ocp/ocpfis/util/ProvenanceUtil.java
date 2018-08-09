package gov.samhsa.ocp.ocpfis.util;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.domain.CodeSystemEnum;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Reference;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class ProvenanceUtil {

    private final IGenericClient fhirClient;

    public ProvenanceUtil(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    public void createProvenance(List<String> idList, ProvenanceActivityEnum provenanceActivityEnum, Optional<String> loggedInUser) {
        Provenance provenance = new Provenance();

        //target
        List<Reference> referenceList = idList.stream().map(id -> {
            Reference reference = new Reference();
            reference.setReference(id);
            return reference;
        }).collect(toList());

        provenance.setTarget(referenceList);

        //recorded : When the activity was recorded/ updated
        provenance.setRecorded(new Date());

        //activity
        if(provenanceActivityEnum != null && FhirResourceUtil.isStringNotNullAndNotEmpty(provenanceActivityEnum.toString())){
            Coding coding = new Coding();
            coding.setCode(provenanceActivityEnum.toString());
            coding.setSystem(CodeSystemEnum.PROVENANCE_ACTIVITY_TYPE.getUrl());
            coding.setDisplay(provenanceActivityEnum.toString().toLowerCase());
            provenance.setActivity(coding);
        }

        //agent.whoReference
        Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        Reference whoRef = new Reference();
        if (loggedInUser.isPresent()) {
            whoRef.setReference(loggedInUser.get());
        } else {
            whoRef.setReference("NA");
        }

        agent.setWho(whoRef);

        provenance.setAgent(Collections.singletonList(agent));
        log.info("Creating Provenance");
        fhirClient.create().resource(provenance).execute();
    }
}
