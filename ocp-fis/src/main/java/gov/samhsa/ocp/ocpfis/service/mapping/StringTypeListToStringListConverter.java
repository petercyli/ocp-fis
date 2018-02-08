package gov.samhsa.ocp.ocpfis.service.mapping;

import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StringTypeListToStringListConverter extends AbstractConverter<List<StringType>, List<String>> {

    @Override
    protected List<String> convert(List<StringType> source) {
        List<String> strings = new ArrayList<>();

        if (source != null && source.size() > 0) {
            for (StringType tempStringType : source) {
                String tempString = tempStringType.getValue();
                strings.add(tempString);
            }
        }
        return strings;
    }
}
