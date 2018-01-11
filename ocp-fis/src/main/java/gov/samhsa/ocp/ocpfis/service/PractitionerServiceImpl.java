package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;

import gov.samhsa.ocp.ocpfis.config.OcpProperties;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.PractitionerNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class PractitionerServiceImpl implements  PractitionerService{

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final OcpProperties ocpProperties;

    @Autowired
    public PractitionerServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, OcpProperties ocpProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.ocpProperties=ocpProperties;
    }

    @Override
    public List<PractitionerDto> getAllPractitioners(Optional<String> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = size.filter(s -> s > 0 &&
                 s <= ocpProperties.getPractitioner().getPagination().getMaxSize()).orElse(ocpProperties.getPractitioner().getPagination().getDefaultSize());
        IQuery iQuery = fhirClient.search().forResource(Practitioner.class);
                if(showInactive.isPresent()) {
                    if(showInactive.get().equalsIgnoreCase("false"))
                        iQuery.where(new TokenClientParam("active").exactly().code("true"));
                }
        Bundle bundle= (Bundle) iQuery.count(numberOfPractitionersPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if(bundle==null || bundle.isEmpty() || bundle.getEntry().size()<1){
            throw new PractitionerNotFoundException("No practitioners were found in the FHIR server.");
        }

        if(page.isPresent() && bundle.getLink(Bundle.LINK_NEXT) !=null){
            bundle=getPractitionerSearchBundleByPageAndSize(bundle,page,numberOfPractitionersPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedPractitioners = bundle.getEntry();

        return retrievedPractitioners.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());

    }

    @Override
    public Set<PractitionerDto> searchPractitioners(String value, Optional<String> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionerPerPage = size.filter(s -> s > 0 &&
                s <= ocpProperties.getPractitioner().getPagination().getMaxSize()).orElse(ocpProperties.getPractitioner().getPagination().getDefaultSize());

        Bundle bundleByName = fhirClient.search().forResource(Practitioner.class)
                .where(new StringClientParam("name").matches().value(value.trim()))
                .returnBundle(Bundle.class)
                .execute();

        Set<PractitionerDto> practitionerDtosByName=convertBundleToPractitionerDtos(bundleByName);

        Bundle bundleByIdentifier=fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("identifier").exactly().code(value.trim()))
                .returnBundle(Bundle.class)
                .execute();

       Set<PractitionerDto> practitionerDtosById=convertBundleToPractitionerDtos(bundleByIdentifier);

       Set<PractitionerDto> practitionerDtos= Stream.concat(practitionerDtosByName.stream(), practitionerDtosById.stream())
               .distinct()
               .collect(Collectors.toSet());

       if(practitionerDtos.size()<1 || practitionerDtos.isEmpty()){
           throw new PractitionerNotFoundException("No practitioner with given name or identifier.");
       }

        return practitionerDtos;

     }

    private Bundle getPractitionerSearchBundleByPageAndSize(Bundle locationSearchBundle, Optional<Integer> page, int numberOfPractitionersPerPage) {
        if (locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page.filter(p -> p >= 1).orElse(1)) - 1) * numberOfPractitionersPerPage;

            if (offset >= locationSearchBundle.getTotal()) {
                throw new PractitionerNotFoundException("No practitioners were found in the FHIR server for this page number");
            }

            String pageUrl = ocpProperties.getFhir().getPublish().getServerUrl().getResource()
                    + "?_getpages=" + locationSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + numberOfPractitionersPerPage
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return locationSearchBundle;
    }

    private Set<PractitionerDto> convertBundleToPractitionerDtos(Bundle response){
        Set<PractitionerDto> practitionerDtos = new HashSet<>();
        if (null == response || response.isEmpty() || response.getEntry().size() < 1) {
            log.info("No practitioners in FHIR Server.");
        } else {
            practitionerDtos = response.getEntry().stream()
                    .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Practitioner))
                    .map(bundleEntryComponent -> (Practitioner) bundleEntryComponent.getResource()) // practitioner resources
                    .map(patient ->{
                        PractitionerDto practitionerDto = modelMapper.map(patient, PractitionerDto.class);
                        return practitionerDto;
                    })
                    .collect(Collectors.toSet());
        }
        return practitionerDtos;
    }

}
