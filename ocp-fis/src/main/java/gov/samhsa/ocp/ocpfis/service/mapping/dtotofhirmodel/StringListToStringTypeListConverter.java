package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StringListToStringTypeListConverter extends AbstractConverter <List<String>, List<StringType> > {
    @Override
    protected List<StringType> convert(List<String> source) {
        List<StringType> stringTypes = new ArrayList<>();

        if (source != null && source.size() > 0) {
            for (String tempString : source) {
                StringType tempStringType = new StringType();
                tempStringType.setValue(tempString);
                stringTypes.add(tempStringType);
            }
        }
        return stringTypes;
    }
}
