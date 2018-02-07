package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

@Component
public class ValuesetDtoToCodeableConceptConverter extends AbstractConverter<ValueSetDto, CodeableConcept> {

    @Override
    protected CodeableConcept convert(ValueSetDto source) {
        CodeableConcept tempCodeableConcept = new CodeableConcept();
        if (source != null) {
                 Coding tempCoding = new Coding();
                tempCoding.setCode(source.getCode());
                tempCoding.setSystem(source.getSystem());
                tempCoding.setDisplay(source.getDisplay());
                tempCodeableConcept.addCoding(tempCoding);
        }
        return tempCodeableConcept;
    }
}