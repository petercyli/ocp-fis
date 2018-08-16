package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ContactDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.Organization.OrganizationContactComponent;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ContactListToContactDtoListConverter extends AbstractConverter<List<OrganizationContactComponent>, Optional<List<ContactDto>>> {

    @Override
    protected Optional<List<ContactDto>> convert(List<OrganizationContactComponent> source) {
        return Optional.of(source.stream()
                .map(organizationContact -> {
                    NameDto nameDto = new NameDto();
                    nameDto.setLastName(organizationContact.getName().getFamily());
                    organizationContact.getName().getGiven().stream().findAny().ifPresent(gn -> nameDto.setFirstName(gn.toString()));

                    return ContactDto.builder()
                            .name(nameDto)
                            .purpose(FhirDtoUtil.convertCodeableConceptToValueSetDto(organizationContact.getPurpose()).getCode())
                            .purposeDisplay(Optional.of(FhirDtoUtil.convertCodeableConceptToValueSetDto(organizationContact.getPurpose()).getDisplay()))
                            .address(FhirDtoUtil.convertAddressToAddressDto(organizationContact.getAddress()))
                            .telecoms(FhirDtoUtil.convertTelecomListToTelecomDtoList(organizationContact.getTelecom()))
                            .build();
                })
                .collect(Collectors.toList()));
    }
}
