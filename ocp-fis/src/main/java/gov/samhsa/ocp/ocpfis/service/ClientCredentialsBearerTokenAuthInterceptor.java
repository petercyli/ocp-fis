package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.util.CoverageIgnore;
import lombok.Data;
import org.apache.commons.lang3.Validate;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

@Data
public class ClientCredentialsBearerTokenAuthInterceptor implements IClientInterceptor {

    private OAuth2RestTemplate oAuth2RestTemplate;

    @CoverageIgnore
    public ClientCredentialsBearerTokenAuthInterceptor() {
        // nothing
    }

    public ClientCredentialsBearerTokenAuthInterceptor(OAuth2RestTemplate myOAuth2RestTemplate) {
        Validate.notNull(myOAuth2RestTemplate);
        oAuth2RestTemplate = myOAuth2RestTemplate;
    }

    @Override
    public void interceptRequest(IHttpRequest iHttpRequest) {
        iHttpRequest.addHeader(Constants.HEADER_AUTHORIZATION, (Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER + oAuth2RestTemplate.getAccessToken().getValue()));
    }

    @Override
    public void interceptResponse(IHttpResponse iHttpResponse){
        // nothing
    }
}
