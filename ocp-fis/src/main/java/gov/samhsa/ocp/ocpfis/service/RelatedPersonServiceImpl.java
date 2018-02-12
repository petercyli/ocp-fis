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
    public PageDto<RelatedPersonDto> searchRelatedPersons(RelatedPersonController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.RelatedPerson.name());

        IQuery relatedPersonIQuery = fhirClient.search().forResource(RelatedPerson.class)
                .where(new StringClientParam("name").matches().value(searchValue.trim()));

        Bundle firstPageBundle;
        Bundle otherPageBundle;
        boolean firstPage = true;

        firstPageBundle = (Bundle) relatedPersonIQuery.count(numberPerPage).returnBundle(Bundle.class).execute();

        if (firstPageBundle == null || firstPageBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No RelatedPerson was found for the given name : " + searchValue);
        }

        otherPageBundle = firstPageBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            firstPage = false;

            otherPageBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageBundle, pageNumber.get(), numberPerPage);
        }

        List<Bundle.BundleEntryComponent> relatedPersons = otherPageBundle.getEntry();

        List<RelatedPersonDto> relatedPersonList = relatedPersons.stream().map(this::convertToRelatedPerson).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPageBundle.getTotal() / numberPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(relatedPersonList, numberPerPage, totalPages, currentPage, relatedPersonList.size(), otherPageBundle.getTotal());
    }

    private RelatedPersonDto convertToRelatedPerson(Bundle.BundleEntryComponent bundleEntryComponent) {
        RelatedPersonDto dto = new RelatedPersonDto();
        RelatedPerson relatedPerson = (RelatedPerson) bundleEntryComponent.getResource();

        dto.setId(relatedPerson.getIdElement().getIdPart());
        dto.setFirstName(convertToString(relatedPerson.getName().stream().findFirst().get().getGiven()));
        dto.setLastName(checkString(relatedPerson.getName().stream().findFirst().get().getFamily()));
        return dto;
    }

    @Override
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

    private String convertToString(List<StringType> nameList) {
        return checkString(nameList.stream().findFirst().get().toString());
    }

    private String checkString(String string) {
        return string == null ? "" : string;
    }
}
