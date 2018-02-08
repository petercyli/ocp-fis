package gov.samhsa.ocp.ocpfis.config;

import ca.uhn.fhir.rest.api.EncodingEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "ocp-fis")
@Data
public class FisProperties {

    @NotNull
    @Valid
    private Fhir fhir;

    @NotNull
    @Valid
    private HealthcareService healthcareService;

    @NotNull
    @Valid
    private Location location;

    @NotNull
    @Valid
    private Practitioner practitioner;

    @NotNull
    @Valid
    private Organization organization;

    @NotNull
    @Valid
    private Patient patient;

    @NotNull
    @Valid
    private RelatedPerson relatedPerson;

    @Data
    public static class Fhir {

        @NotBlank
        private String serverUrl;
        @NotBlank
        private String clientSocketTimeoutInMs;
        @NotNull
        private EncodingEnum encoding = EncodingEnum.JSON;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HealthcareService {
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Organization {
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Patient {
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RelatedPerson {
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

