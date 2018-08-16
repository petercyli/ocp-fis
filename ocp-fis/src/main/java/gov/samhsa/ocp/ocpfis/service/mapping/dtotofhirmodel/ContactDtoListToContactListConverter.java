package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ContactDto;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirResourceUtil;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Organization.OrganizationContactComponent;
import org.modelmapper.AbstractConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ContactDtoListToContactListConverter extends AbstractConverter<Optional<List<ContactDto>>, List<OrganizationContactComponent>> {
    @Autowired
    private LookUpService lookUpService;

    @Override
    protected List<OrganizationContactComponent> convert(Optional<List<ContactDto>> source) {
        List<ContactDto> contactDtos = source.orElse(null);
        return Objects.requireNonNull(contactDtos).stream()
                .map(contactDto -> {
                    OrganizationContactComponent organizationContact = new OrganizationContactComponent();
                    HumanName humanName = new HumanName();
                    humanName.setFamily(contactDto.getName().getLastName());
                    humanName.addGiven(contactDto.getName().getFirstName());
                    organizationContact.setName(humanName);
                    organizationContact.setPurpose(FhirDtoUtil.convertValuesetDtoToCodeableConcept(FhirDtoUtil.convertCodeToValueSetDto(contactDto.getPurpose(), lookUpService.getContactPurpose())));
                    organizationContact.setTelecom(FhirResourceUtil.convertTelecomDtoListToTelecomList(contactDto.getTelecoms(), lookUpService));
                    organizationContact.setAddress(FhirResourceUtil.convertAddressDtoToAddress(contactDto.getAddress(), lookUpService));
                    return organizationContact;
                })
                .collect(Collectors.toList());
    }
}
