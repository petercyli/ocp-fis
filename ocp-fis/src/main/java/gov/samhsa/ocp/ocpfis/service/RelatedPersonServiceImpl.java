package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
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

    @Autowired
    public RelatedPersonServiceImpl(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    @Override
    public PageDto<RelatedPersonDto> searchRelatedPersons(RelatedPersonController.SearchType searchType, String searchValue, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberPerPage = PaginationUtil.getValidPageSize(size, ResourceType.RelatedPerson.name());

        Bundle relatedPersonBundle = fhirClient.search().forResource(RelatedPerson.class)
                .where(new StringClientParam("name").matches().value(searchValue.trim()))
                .returnBundle(Bundle.class)
                .execute();

        if (relatedPersonBundle == null || relatedPersonBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No RelatedPerson was found for the given member : " + searchValue + "with name : " + searchValue);
        }

        List<Bundle.BundleEntryComponent> retrievedRelatedPersonList = relatedPersonBundle.getEntry();

        List<RelatedPersonDto> relatedPersonList = retrievedRelatedPersonList.stream().map(relatedPersonBundleEntry -> {
            RelatedPersonDto dto = new RelatedPersonDto();
            RelatedPerson relatedPerson = (RelatedPerson) relatedPersonBundleEntry.getResource();

            dto.setId(relatedPerson.getIdElement().getIdPart());
            dto.setFirstName(convertToString(relatedPerson.getName().stream().findFirst().get().getGiven()));
            dto.setLastName(checkString(relatedPerson.getName().stream().findFirst().get().getFamily()));
            return dto;
        }).collect(Collectors.toList());

        return new PageDto<>(relatedPersonList, numberPerPage, 1, 1, relatedPersonList.size(), relatedPersonList.size());
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
