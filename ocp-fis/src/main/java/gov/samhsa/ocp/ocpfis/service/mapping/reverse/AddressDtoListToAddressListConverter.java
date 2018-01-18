package gov.samhsa.ocp.ocpfis.service.mapping.reverse;


import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import org.hl7.fhir.dstu3.model.Address;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AddressDtoListToAddressListConverter extends AbstractConverter<List<AddressDto>, List<Address>> {

    @Override
    protected List<Address> convert(List<AddressDto> source) {
        List<Address> addressList = new ArrayList<>();

        if (source != null && source.size() > 0) {
            for (AddressDto addressDto : source) {
                Address tempAddress = new Address();
                if (addressDto.getLine1() != null)
                    tempAddress.addLine(addressDto.getLine1());
                if (addressDto.getLine2() != null)
                    tempAddress.addLine(addressDto.getLine2());
                if (addressDto.getCity() != null)
                    tempAddress.setCity(addressDto.getCity());
                if (addressDto.getPostalCode() != null)
                    tempAddress.setPostalCode(addressDto.getPostalCode());
                if (addressDto.getStateCode() != null)
                    tempAddress.setState(addressDto.getStateCode());
                if (addressDto.getCountryCode() != null)
                    tempAddress.setCountry(addressDto.getCountryCode());

                addressList.add(tempAddress);
            }
        }
        return addressList;
    }
}
