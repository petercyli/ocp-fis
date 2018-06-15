package gov.samhsa.ocp.ocpfis.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.service.ClientCredentialsBearerTokenAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.util.Optional;

@Configuration
public class FhirServiceConfig {

    private final FisProperties fisProperties;

    private Optional<OAuth2RestTemplate> oAuth2RestTemplate;

    @Autowired
    public FhirServiceConfig(FisProperties fisProperties, Optional<OAuth2RestTemplate> oAuth2RestTemplate) {
        this.fisProperties = fisProperties;
        this.oAuth2RestTemplate = oAuth2RestTemplate;
    }

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = FhirContext.forDstu3();
        fhirContext.getRestfulClientFactory().setSocketTimeout(Integer.parseInt(fisProperties.getFhir().getClientSocketTimeoutInMs()));
        return fhirContext;
    }

    @Bean
    public IGenericClient fhirClient() {
        IGenericClient fhirClient = fhirContext().newRestfulGenericClient(fisProperties.getFhir().getServerUrl());
        if (fisProperties.getFhir().isServerSecurityEnabled() && oAuth2RestTemplate.isPresent()) {
            ClientCredentialsBearerTokenAuthInterceptor authInterceptor = new ClientCredentialsBearerTokenAuthInterceptor(oAuth2RestTemplate.get());
            fhirClient.registerInterceptor(authInterceptor);
        }
        return fhirClient;
    }

    @Bean
    public IParser fhirJsonParser() {
        return fhirContext().newJsonParser();
    }

    @Bean
    public FhirValidator fhirValidator() {
        return fhirContext().newValidator();
    }
}
