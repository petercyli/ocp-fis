package gov.samhsa.ocp.ocpfis.util;

import gov.samhsa.ocp.ocpfis.service.dto.LookupPathUrls;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ValueSet;

@Slf4j
public class LookUpUtil {

    public static boolean checkIfValueSetAvailableInServer(ValueSet response, String type) {
        boolean isAvailable = true;
        if (type.equalsIgnoreCase(LookupPathUrls.US_STATE.getType())
                || type.equalsIgnoreCase(LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY_2.getType())) {
            if (response == null || response.getCompose() == null ||
                    response.getCompose().getInclude() == null ||
                    response.getCompose().getInclude().isEmpty() ||
                    response.getCompose().getInclude().get(0).getConcept() == null ||
                    response.getCompose().getInclude().get(0).getConcept().isEmpty()) {
                isAvailable = false;
            }
        } else {
            if (response == null ||
                    response.getExpansion() == null ||
                    response.getExpansion().getContains() == null ||
                    response.getExpansion().getContains().isEmpty()) {
                isAvailable = false;
            }
        }
        return isAvailable;
    }

    public static boolean isValueSetAvailableInServer(ValueSet response, String type) {
        return isValueSetAvailableInServer(response, type, Boolean.TRUE);
    }

    public static boolean isValueSetAvailableInServer(ValueSet response, String type, boolean isThrow) {
        boolean isAvailable = checkIfValueSetAvailableInServer(response, type);
        if (!isAvailable && isThrow) {
            log.error("Query was successful, but found no " + type + " codes in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no " + type + " codes in the configured FHIR server");
        }
        return isAvailable;
    }

    public static ValueSetDto convertConceptReferenceToValueSetDto(ValueSet.ConceptReferenceComponent conceptReferenceComponent) {
        ValueSetDto valueSetDto = new ValueSetDto();
        valueSetDto.setCode(conceptReferenceComponent.getCode());
        valueSetDto.setDisplay(conceptReferenceComponent.getDisplay());
        return valueSetDto;
    }

    public static ValueSetDto convertExpansionComponentToValueSetDto(ValueSet.ValueSetExpansionContainsComponent expansionComponent) {
        ValueSetDto valueSetDto = new ValueSetDto();
        valueSetDto.setSystem(expansionComponent.getSystem());
        valueSetDto.setCode(expansionComponent.getCode());
        valueSetDto.setDisplay(expansionComponent.getDisplay());
        return valueSetDto;
    }
}
