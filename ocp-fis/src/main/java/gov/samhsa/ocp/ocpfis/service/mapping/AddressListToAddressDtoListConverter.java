package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import org.hl7.fhir.dstu3.model.Address;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AddressListToAddressDtoListConverter extends AbstractConverter<List<Address>, List<AddressDto>> {
    @Override
    protected List<AddressDto> convert(List<Address> source) {
        List<AddressDto> addressDtoList = new ArrayList<>();

        if (source != null && source.size() > 0) {

            AddressDto tempAddressDto = new AddressDto();

            for (Address tempAddress : source) {
                if (source != null) {


                    int numberOfLines = tempAddress.getLine().size();
                    if (numberOfLines > 0) {
                        tempAddressDto.setLine1(tempAddress.getLine().get(0).toString());

                        if (numberOfLines > 1) {
                            tempAddressDto.setLine2(tempAddress.getLine().get(1).toString());
                        }
                    }

                    tempAddressDto.setCity(tempAddress.getCity());
                    if (tempAddress.getCountry() != null)
                        tempAddressDto.setCountryCode(tempAddress.getCountry());
                    if (tempAddress.getState() != null)
                        tempAddressDto.setStateCode(tempAddress.getState());
                    if (tempAddress.getUse() != null)
                        tempAddressDto.setUse(tempAddress.getUse().toString());
                    tempAddressDto.setPostalCode(tempAddress.getPostalCode());
                }

                if (tempAddress.getUse() != null)
                    tempAddressDto.setUse(tempAddress.getUse().toString());
                addressDtoList.add(tempAddressDto);
            }
        }
        return addressDtoList;
    }
}
