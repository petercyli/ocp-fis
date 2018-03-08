package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.mapping.EpisodeOfCareToEpisodeOfCareDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class EpisodeOfCareServiceImpl implements EpisodeOfCareService {

    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final LookUpService lookUpService;
    private final FisProperties fisProperties;

    @Autowired
    public EpisodeOfCareServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }

    @Override
    public List<EpisodeOfCareDto> getEpisodeOfCares(String patient, Optional<String> status) {
        List<EpisodeOfCareDto> episodeOfCareDtos = new ArrayList<>();

        IQuery iQuery = fhirClient.search().forResource(EpisodeOfCare.class).where(new ReferenceClientParam("patient").hasId("Patient/" + patient));

        if (status.isPresent()) {
            iQuery.where(new TokenClientParam("status").exactly().code(status.get().trim()));
        }

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> eocCompoents = bundle.getEntry();

            if (eocCompoents != null) {
                episodeOfCareDtos = eocCompoents.stream()
                        .map(it -> (EpisodeOfCare) it.getResource())
                        .map(it -> EpisodeOfCareToEpisodeOfCareDtoMapper.map(it))
                        .collect(toList());
            }
        }

        return episodeOfCareDtos;
    }

    @Override
    public List<ReferenceDto> getEpisodeOfCaresForReference(String patient, Optional<String> status) {
        List<ReferenceDto> referenceDtos = new ArrayList<>();

        IQuery iQuery = fhirClient.search().forResource(Task.class)
                .where(new ReferenceClientParam("patient").hasId(ResourceType.Patient + "/" + patient))
                .include(Task.INCLUDE_CONTEXT);

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> eocCompoents = bundle.getEntry();

            if (eocCompoents != null) {
                referenceDtos = eocCompoents.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.Task))
                        .map(it -> (Task) it.getResource())
                        .filter(task -> task.hasContext())
                        .map(it -> EpisodeOfCareToEpisodeOfCareDtoMapper.mapToReferenceDto(it))
                        .collect(toList());
            }
        }

        return referenceDtos;

    }
}
