package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.Address;
import org.modelmapper.AbstractConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressDtoToAddressConverter extends AbstractConverter<AddressDto, Address> {
    @Autowired
    private LookUpService lookUpService;

    @Override
    protected Address convert(AddressDto source) {
        Address fhirAddress = new Address();
        List<ValueSetDto> validAddressUses =  lookUpService.getAddressUses();

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
               for(ValueSetDto validAddrUse : validAddressUses){
                   if (source.getUse().equalsIgnoreCase(validAddrUse.getDisplay())){
                       fhirAddress.setUse(Address.AddressUse.valueOf(source.getUse()));
                   }
               }
            }
        }
        return fhirAddress;
    }
}
