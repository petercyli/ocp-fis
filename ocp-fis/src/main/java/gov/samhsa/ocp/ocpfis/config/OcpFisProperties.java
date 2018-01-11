package gov.samhsa.ocp.ocpfis.config;

import ca.uhn.fhir.rest.api.EncodingEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "ocp-fis")
@Data
public class OcpFisProperties {

    @NotNull
    @Valid
    private Fhir fhir;

    @NotNull
    @Valid
    private Location location;

    @NotNull
    @Valid
    private Practitioner practitioner;

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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Location {
        @Valid
        private Pagination pagination = new Pagination();

        @Data
        public static class Pagination {
            @Min(1)
            @Max(500)
            private int defaultSize = 10;
            @Min(1)
            @Max(500)
            private int maxSize = 50;
        }
    }
}

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Practitioner {
        @Valid
        private Pagination pagination = new Pagination();

        @Data
        public static class Pagination {
            @Min(1)
            @Max(500)
            private int defaultSize = 10;
            @Min(1)
            @Max(500)
            private int maxSize = 50;
        }
    }
}