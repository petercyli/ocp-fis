package gov.samhsa.ocp.ocpfis.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.validation.FhirValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@Configuration
public class FhirServiceConfig {

    private final FisProperties fisProperties;

    private final OAuth2ClientCredentialsConfig oAuth2ClientCredentialsConfig;

    @Autowired
    public FhirServiceConfig(FisProperties fisProperties, OAuth2ClientCredentialsConfig oAuth2ClientCredentialsConfig) {
        this.fisProperties = fisProperties;
        this.oAuth2ClientCredentialsConfig = oAuth2ClientCredentialsConfig;
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
        if (fisProperties.getFhir().isServerSecurityEnabled()) {
            ClientCredentialsResourceDetails clientCredentialsResourceDetails = oAuth2ClientCredentialsConfig.clientCredentialsResourceDetails();
            DefaultOAuth2ClientContext clientContext = new DefaultOAuth2ClientContext();
            OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(clientCredentialsResourceDetails, clientContext);
            OAuth2AccessToken token = oAuth2RestTemplate.getAccessToken();
            BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token.getValue());
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
