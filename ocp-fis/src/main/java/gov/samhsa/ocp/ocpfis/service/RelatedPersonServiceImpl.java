package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.web.RelatedPersonController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RelatedPersonServiceImpl implements RelatedPersonService {

    private final IGenericClient fhirClient;
    private final FisProperties fisProperties;

    @Autowired
    public RelatedPersonServiceImpl(IGenericClient fhirClient, FisProperties fisProperties) {
        this.fhirClient = fhirClient;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<RelatedPersonDto> searchRelatedPersons(RelatedPersonController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.RelatedPerson.name());

        IQuery relatedPersonIQuery = fhirClient.search().forResource(RelatedPerson.class)
                .where(new StringClientParam("name").matches().value(searchValue.trim()));

        Bundle firstPageBundle;
        Bundle otherPageBundle;
        boolean firstPage = true;

        firstPageBundle = (Bundle) relatedPersonIQuery.count(numberPerPage).returnBundle(Bundle.class).execute();

        if (bundleNotAvailable(firstPageBundle)) {
            throw new ResourceNotFoundException("No RelatedPerson was found for the given name : " + searchValue);
        }

        otherPageBundle = firstPageBundle;

        if (morePagesAvailable(page, otherPageBundle)) {
            firstPage = false;

            otherPageBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageBundle, page.get(), numberPerPage);
        }

        List<Bundle.BundleEntryComponent> relatedPersons = otherPageBundle.getEntry();

        List<RelatedPersonDto> relatedPersonList = relatedPersons.stream().map(relatedPersonBundleEntry -> {
            RelatedPersonDto dto = new RelatedPersonDto();
            RelatedPerson relatedPerson = (RelatedPerson) relatedPersonBundleEntry.getResource();

            dto.setId(relatedPerson.getIdElement().getIdPart());
            dto.setFirstName(convertToString(relatedPerson.getName().stream().findFirst().get().getGiven()));
            dto.setLastName(checkString(relatedPerson.getName().stream().findFirst().get().getFamily()));
            return dto;
        }).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPageBundle.getTotal() / numberPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(relatedPersonList, numberPerPage, totalPages, currentPage, relatedPersonList.size(), otherPageBundle.getTotal());
    }

    private boolean morePagesAvailable(Optional<Integer> page, Bundle otherPageBundle) {
        return page.isPresent() && page.get() > 1 && otherPageBundle.getLink(Bundle.LINK_NEXT) != null;
    }

    private boolean bundleNotAvailable(Bundle firstPageBundle) {
        return firstPageBundle == null || firstPageBundle.isEmpty() || firstPageBundle.getEntry().size() < 1;
    }

    public RelatedPersonDto getRelatedPersonById(String relatedPersonId) {
        Bundle relatedPersonBundle = fhirClient.search().forResource(RelatedPerson.class)
                .where(new TokenClientParam("_id").exactly().code(relatedPersonId))
                .returnBundle(Bundle.class)
                .execute();

        Bundle.BundleEntryComponent relatedPersonBundleEntry = relatedPersonBundle.getEntry().get(0);
        RelatedPerson relatedPerson = (RelatedPerson) relatedPersonBundleEntry.getResource();
        RelatedPersonDto relatedPersonDto = new RelatedPersonDto();
        relatedPersonDto.setId(relatedPerson.getIdElement().getIdPart());
        relatedPersonDto.setFirstName(convertToString(relatedPerson.getName().stream().findFirst().get().getGiven()));
        relatedPersonDto.setLastName(checkString(relatedPerson.getName().stream().findFirst().get().getFamily()));

        return relatedPersonDto;
    }

    private String toFirstNameFromHumanName(HumanName humanName) {
        List<StringType> fNameList = humanName.getGiven();

        return checkString(fNameList.stream().findFirst().get().toString());
    }

    private String convertToString(List<StringType> nameList) {
        return checkString(nameList.stream().findFirst().get().toString());
    }

    private String checkString(String string) {
        return string == null ? "" : string;
    }
}
