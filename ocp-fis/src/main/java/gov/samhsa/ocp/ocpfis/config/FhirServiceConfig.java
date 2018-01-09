package gov.samhsa.ocp.ocpfis.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirServiceConfig {

    private final OcpProperties ocpProperties;

    @Autowired
    public FhirServiceConfig(OcpProperties ocpProperties) {
        this.ocpProperties = ocpProperties;
    }

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = FhirContext.forDstu3();
        fhirContext.getRestfulClientFactory().setSocketTimeout(Integer.parseInt(ocpProperties.getFhir().getPublish().getClientSocketTimeoutInMs()));
        return fhirContext;
    }

    @Bean
    public IGenericClient fhirClient() {
        return fhirContext().newRestfulGenericClient(ocpProperties.getFhir().getPublish().getServerUrl().getResource());
    }

    @Bean
    public IParser fhirJsonParser() {
        return fhirContext().newJsonParser();
    }


}
