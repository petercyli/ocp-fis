package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;

import gov.samhsa.ocp.ocpfis.config.OcpFisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.PractitionerNotFoundException;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private final OcpFisProperties ocpFisProperties;

    @Autowired
    public PractitionerServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, OcpFisProperties ocpFisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.ocpFisProperties = ocpFisProperties;
    }

    @Override
    public List<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getPractitioner().getPagination().getMaxSize()).orElse(ocpFisProperties.getPractitioner().getPagination().getDefaultSize());
        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);
        if (showInactive.isPresent()) {
            if (!showInactive.get())
                practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle bundle = fhirClient.search().forResource(Practitioner.class)
                .count(numberOfPractitionersPerPage)
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> retrievedPractitioners = bundle.getEntry();

        return retrievedPractitioners.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());
    }

    @Override
    public List<PractitionerDto> searchPractitioners(PractitionerController.SearchType type, String value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getPractitioner().getPagination().getMaxSize()).orElse(ocpFisProperties.getPractitioner().getPagination().getDefaultSize());

        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);

        if (type.equals(PractitionerController.SearchType.name))
            practitionerIQuery.where(new StringClientParam("name").matches().value(value.trim()));

        if (type.equals(PractitionerController.SearchType.identifier))
            practitionerIQuery.where(new TokenClientParam("identifier").exactly().code(value.trim()));

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPagePractitionerSearchBundle = (Bundle) practitionerIQuery.count(numberOfPractitionersPerPage).returnBundle(Bundle.class)
                .execute();

        if (firstPagePractitionerSearchBundle == null || firstPagePractitionerSearchBundle.isEmpty() || firstPagePractitionerSearchBundle.getEntry().size() < 1) {
            throw new PractitionerNotFoundException("No practitioners were found in the FHIR server.");
        }

        if (page.isPresent() && firstPagePractitionerSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPagePractitionerSearchBundle = getPractitionerSearchBundleAfterFirstPage(firstPagePractitionerSearchBundle, page, numberOfPractitionersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = firstPagePractitionerSearchBundle.getEntry();

        return retrievedPractitioners.stream().map(retrievedPractitioner -> modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class)).collect(Collectors.toList());
    }


    private Bundle getPractitionerSearchBundleAfterFirstPage(Bundle practitionerSearchBundle, Optional<Integer> page, int numberOfPractitionersPerPage) {
        if (practitionerSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page.filter(p -> p >= 1).orElse(1)) - 1) * numberOfPractitionersPerPage;

            if (offset >= practitionerSearchBundle.getTotal()) {
                throw new PractitionerNotFoundException("No practitioners were found in the FHIR server for this page number");
            }

            String pageUrl = ocpFisProperties.getFhir().getPublish().getServerUrl().getResource()
                    + "?_getpages=" + practitionerSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + numberOfPractitionersPerPage
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return practitionerSearchBundle;
    }

}
