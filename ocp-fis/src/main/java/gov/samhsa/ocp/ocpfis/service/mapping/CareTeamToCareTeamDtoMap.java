package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CareTeamToCareTeamDtoMap extends PropertyMap<CareTeam, CareTeamDto> {

    @Autowired
    private IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter;

    @Autowired
    private CareTeamStatusToValueSetDtoConverter careTeamStatusToValueSetDtoConverter;

    @Autowired
    private CategoryToValueSetDtoListConverter categoryToValueSetDtoListConverter;


    @Override
    protected void configure() {
        map().setName(source.getName());
        using(careTeamStatusToValueSetDtoConverter).map(source.getStatus()).setStatus(null);
        using(categoryToValueSetDtoListConverter).map(source.getCategory()).setCategories(null);
    }
}
