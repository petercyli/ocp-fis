package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerRoleToPractitionerRoleDtoMap extends PropertyMap<PractitionerRole,PractitionerRoleDto> {

    @Autowired
    private CodeableConceptListToValueSetDtoListConverter codeableConceptListToValueSetDtoListConverter;

    @Override
    protected void configure() {
        map().setActive(source.getActive());
        using(codeableConceptListToValueSetDtoListConverter).map(source.getCode()).setCode(null);
        using(codeableConceptListToValueSetDtoListConverter).map(source.getSpecialty()).setSpecialty(null);
    }
}
