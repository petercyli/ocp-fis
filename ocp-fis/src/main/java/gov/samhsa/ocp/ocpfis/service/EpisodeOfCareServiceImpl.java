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
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
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

        //Set Sort order
        iQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(iQuery, true);

        if (status.isPresent()) {
            iQuery.where(new TokenClientParam("status").exactly().code(status.get().trim()));
        }

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> eocCompoents = bundle.getEntry();

            if (eocCompoents != null) {
                episodeOfCareDtos = eocCompoents.stream()
                        .map(it -> (EpisodeOfCare) it.getResource())
                        .map(it -> EpisodeOfCareToEpisodeOfCareDtoMapper.map(it,lookUpService))
                        .collect(toList());
            }
        }

        return episodeOfCareDtos;
    }

    @Override
    public List<ReferenceDto> getEpisodeOfCaresForReference(String patient, Optional<String> organization, Optional<String> status) {
        List<ReferenceDto> referenceDtos = new ArrayList<>();
        IQuery iQuery = fhirClient.search().forResource(EpisodeOfCare.class);

        if (organization.isPresent())
            iQuery.where(new ReferenceClientParam("patient").hasId(patient))
                    .where(new ReferenceClientParam("organization").hasId(organization.get()));
        else
            iQuery.where(new ReferenceClientParam("patient").hasId(patient));

        //Set Sort order
        iQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(iQuery, true);

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> eocComponents = bundle.getEntry();

            if (eocComponents != null) {
                referenceDtos = eocComponents.stream()
                        .map(it -> (EpisodeOfCare) it.getResource())
                        .map(it -> {
                            ReferenceDto referenceDto=new ReferenceDto();
                            referenceDto.setReference("EpisodeOfCare/"+it.getIdElement().getIdPart());
                            Patient p=fhirClient.read().resource(Patient.class).withId(patient).execute();
                            String name=p.getName().stream().findAny().get().getGiven().stream().findAny().get()+p.getName().stream().findAny().get().getFamily();
                            Organization org=fhirClient.read().resource(Organization.class).withId(it.getManagingOrganization().getReference().split("/")[1]).execute();
                            String orgName=org.getName();
                            referenceDto.setDisplay(name+"-"+orgName);
                            return referenceDto;
                        })
                        .collect(toList());
            }
        }

        return referenceDtos;

    }




}
