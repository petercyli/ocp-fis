package gov.samhsa.ocp.ocpfis.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.service.ClientCredentialsBearerTokenAuthInterceptor;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.hapi.ctx.IValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@Slf4j
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
        FhirValidator validator = fhirContext().newValidator();
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator();
        validator.registerValidatorModule(instanceValidator);

        IValidationSupport valSupport = new IValidationSupport() {

            public org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionComponent expandValueSet(FhirContext theContext, org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent theInclude) {
                // TODO: implement
                return null;
            }


            public List<IBaseResource> fetchAllConformanceResources(FhirContext theContext) {
                // TODO: implement
                return null;
            }


            public List<StructureDefinition> fetchAllStructureDefinitions(FhirContext theContext) {
                Bundle structureDefinitionBundle = (Bundle) FhirUtil.searchNoCache(fhirClient(), StructureDefinition.class, Optional.empty()).count(1000).returnBundle(Bundle.class).execute();

                if (structureDefinitionBundle != null && !structureDefinitionBundle.getEntry().isEmpty()) {
                    log.info("Number of Structure Definitions found:" + structureDefinitionBundle.getTotal());

                    List<StructureDefinition> temp = structureDefinitionBundle.getEntry().stream()
                            .filter(bundle -> bundle.getResource().getResourceType().equals(ResourceType.StructureDefinition))
                            .map(structureDefinition -> (StructureDefinition) structureDefinition.getResource())
                            .collect(Collectors.toList());
                    return temp;

                }
                log.info("NO Structure Definitions found:");
                return null;
            }

            @Override
            public CodeSystem fetchCodeSystem(FhirContext theContext, String theSystem) {
                // TODO: implement
                log.info("fetchCodeSystem: theSystem" + theSystem);
                return null;
            }


            @Override
            public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
                // TODO: implement
                log.info("FetchResource: theClass" + theClass.getName());
                log.info("FetchResource: theUri" + theUri);
                return null;
            }


            public StructureDefinition fetchStructureDefinition(FhirContext theCtx, String theUrl) {
                // TODO: implement
                log.info("fetchStructureDefinition: theUri" + theUrl);
                return null;
            }

            @Override
            public boolean isCodeSystemSupported(FhirContext theContext, String theSystem) {
                // TODO: implement
                log.info("isCodeSystemSupported: theSystem" + theSystem);
                return false;
            }

            @Override
            public CodeValidationResult validateCode(FhirContext theContext, String theCodeSystem, String theCode, String theDisplay) {
                // TODO: implement
                log.info("validateCode: theCodeSystem" + theCodeSystem);
                log.info("validateCode: theCode" + theCode);
                log.info("validateCode: theDisplay" + theDisplay);
                return null;
            }

        };
        ValidationSupportChain support = new ValidationSupportChain(new DefaultProfileValidationSupport(), valSupport);
        instanceValidator.setValidationSupport(support);
        return validator;
    }
}
