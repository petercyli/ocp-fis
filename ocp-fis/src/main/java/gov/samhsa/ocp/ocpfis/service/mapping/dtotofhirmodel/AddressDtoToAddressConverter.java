package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.LocationInfoEnum;
import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import org.hl7.fhir.dstu3.model.Address;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

@Component
public class AddressDtoToAddressConverter extends AbstractConverter<AddressDto, Address> {
    @Override
    protected Address convert(AddressDto source) {
        Address fhirAddress = new Address();

        if (source != null) {
            if (source.getLine1() != null && !source.getLine1().isEmpty()) {
                fhirAddress.addLine(source.getLine1().trim());
            }
            if (source.getLine2() != null && !source.getLine2().isEmpty()) {
                fhirAddress.addLine(source.getLine2().trim());
            }
            if (source.getCity() != null && !source.getCity().isEmpty()) {
                fhirAddress.setCity(source.getCity().trim());
            }
            if (source.getStateCode() != null && !source.getStateCode().isEmpty()) {
                fhirAddress.setState(source.getStateCode().trim());
            }
            if (source.getPostalCode() != null && !source.getPostalCode().isEmpty()) {
                fhirAddress.setPostalCode(source.getPostalCode().trim());
            }
            if (source.getCountryCode() != null && !source.getCountryCode().isEmpty()) {
                fhirAddress.setCountry(source.getCountryCode().trim());
            }
            if(source.getUse() != null && !source.getUse().isEmpty()){
               for(LocationInfoEnum.LocationAddressUse addrUse : LocationInfoEnum.LocationAddressUse.values()){
                   if (source.getUse().equalsIgnoreCase(addrUse.name())){
                       fhirAddress.setUse(Address.AddressUse.valueOf(source.getUse()));
                   }
               }
            }
        }
        return fhirAddress;
    }
}
