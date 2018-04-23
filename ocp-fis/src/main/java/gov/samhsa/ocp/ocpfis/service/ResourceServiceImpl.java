package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private IGenericClient fhirClient;

    @Override
    public void deleteResource(String resource, String id) {
        fhirClient.delete().resourceConditionalByType(resource).where(new TokenClientParam("_id").exactly().code(id)).execute();

    }
}
