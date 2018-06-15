package gov.samhsa.ocp.ocpfis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

@Configuration
@ConditionalOnProperty(value = "ocp-fis.fhir.serverSecurityEnabled")
public class OAuth2ClientCredentialsConfig {

    @Value("${security.oauth2.client.accessTokenUri}")
    private String accessTokenUri;

    @Bean
    public ClientCredentialsResourceDetails clientCredentialsResourceDetails(OAuth2ClientProperties oAuth2ClientProperties) {
        ClientCredentialsResourceDetails clientCredentialsResourceDetails = new ClientCredentialsResourceDetails();
        clientCredentialsResourceDetails.setAccessTokenUri(accessTokenUri);
        clientCredentialsResourceDetails.setClientId(oAuth2ClientProperties.getClientId());
        clientCredentialsResourceDetails.setClientSecret(oAuth2ClientProperties.getClientSecret());
        return clientCredentialsResourceDetails;
    }

    @Primary
    @Bean
    public OAuth2ClientContext oAuth2ClientContext() {
        return new DefaultOAuth2ClientContext();
    }

    @Bean
    public OAuth2RestTemplate createRestTemplate(OAuth2ClientContext oAuth2ClientContext, ClientCredentialsResourceDetails resource) {
        return new OAuth2RestTemplate(resource, oAuth2ClientContext);
    }
}
