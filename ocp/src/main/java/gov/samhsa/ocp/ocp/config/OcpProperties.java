package gov.samhsa.ocp.ocp.config;

import ca.uhn.fhir.rest.api.EncodingEnum;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "ocp")
@Data
public class OcpProperties {

    @NotNull
    @Valid
    private Fhir fhir;

    @Data
    public static class Fhir {

        private Publish publish;

        @Data
        public static class Publish {
            @NotBlank
            private ServerUrl serverUrl;
            @NotBlank
            private String clientSocketTimeoutInMs;
            @NotNull
            private EncodingEnum encoding = EncodingEnum.JSON;
        }
    }

    @Data
    public static class ServerUrl {
        @NotBlank
        private String resource;
    }
}
