package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValuesetDtoListToCodeableConceptListConverter extends AbstractConverter<List<ValueSetDto>, List<CodeableConcept>> {


    @Override
    protected List<CodeableConcept> convert(List<ValueSetDto> source) {
        List<CodeableConcept> codeableConceptList = new ArrayList<>();

        if (source != null && source.size() > 0) {
            for (ValueSetDto valueSetDto : source) {
                CodeableConcept tempCodeableConcept = new CodeableConcept();
                if (valueSetDto.getCode() != null){
                    Coding tempCoding = new Coding();
                    tempCoding.setCode(valueSetDto.getCode());
                    tempCoding.setSystem(valueSetDto.getSystem());
                    tempCoding.setDisplay(valueSetDto.getDisplay());
                    tempCodeableConcept.addCoding(tempCoding);
                }
                codeableConceptList.add(tempCodeableConcept);
            }
        }
        return codeableConceptList;
    }
}
