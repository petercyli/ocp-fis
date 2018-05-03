package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.organization.Element;
import gov.samhsa.ocp.ocpfis.data.model.organization.TempOrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommonHelper {

    public static AddressDto getAddress(String cellValue) {
        //TODO parse cellValue to get AddressDto
        return new AddressDto();
    }

    public static List<AddressDto> getAddresses(String cellValue) {
        System.out.println("getAddresses : " + cellValue);
        String [] elements = cellValue.split("\\|");
        System.out.println("size of elements" + elements.length);
        AddressDto dto = new AddressDto();

        if(elements.length == 5) {
            dto.setLine1(elements[0]);
            dto.setCity(elements[1]);
            dto.setStateCode(elements[2]);
            dto.setPostalCode(elements[3]);
            dto.setCountryCode(elements[4]);
        }

        return Arrays.asList(dto);
    }

    public static List<TelecomDto> getTelecoms(String cellValue) {
        TelecomDto telecomDto = new TelecomDto();
        telecomDto.setValue(Optional.of(cellValue.replaceAll("-", "")));
        return Arrays.asList(telecomDto);
    }

    public static List<IdentifierDto> getIdentifiers(String system, String cellValue) {
        IdentifierDto dto = new IdentifierDto();
        dto.setSystem(system);
        dto.setValue(cellValue);
        return Arrays.asList(dto);
    }

    public static Map<String, String> getLookup(String url) {
        RestTemplate rt = new RestTemplate();
        ResponseEntity<ValueSetDto[]> foo = rt.getForEntity(url, ValueSetDto[].class);

        ValueSetDto[] dtos = foo.getBody();

        Map<String, String> mapOfLookup = new HashMap<>();

        for(ValueSetDto dto : dtos) {
            mapOfLookup.put(dto.getDisplay(), dto.getCode());
        }
        return mapOfLookup;
    }

     public static Map<String, ValueSetDto> getLookupValueSet(String url){
        RestTemplate rt=new RestTemplate();
        ResponseEntity<ValueSetDto[]> foo=rt.getForEntity(url,ValueSetDto[].class);

        ValueSetDto[] dtos=foo.getBody();

        Map<String,ValueSetDto> mapOfLookupValueSet=new HashMap<>();

        for(ValueSetDto valueSetDto: dtos){
            mapOfLookupValueSet.put(valueSetDto.getDisplay(),valueSetDto);
        }
        return mapOfLookupValueSet;
     }

     public static String getOrganizationId(String name){
         String orgUrl="http://localhost:8444/organizations/search?searchType=name&searchValue="+name;
         RestTemplate rt = new RestTemplate();
         ResponseEntity<TempOrganizationDto> foo = rt.getForEntity(orgUrl, TempOrganizationDto.class);

         TempOrganizationDto tempOrganizationDto = foo.getBody();

         List<Element> elements = tempOrganizationDto.getElements();

         return elements.stream().findFirst().get().getLogicalId();
     }


}
