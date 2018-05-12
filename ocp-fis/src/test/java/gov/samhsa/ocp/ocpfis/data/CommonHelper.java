package gov.samhsa.ocp.ocpfis.data;

import com.github.javafaker.Faker;
import gov.samhsa.ocp.ocpfis.data.model.organization.Element;
import gov.samhsa.ocp.ocpfis.data.model.organization.TempOrganizationDto;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommonHelper {

    public static AddressDto getAddress(String cellValue) {
        AddressDto dto = new AddressDto();
        Faker faker = new Faker();
        dto.setLine1(faker.address().streetAddress());
        dto.setCity(faker.address().city());
        dto.setStateCode(faker.address().state());
        dto.setPostalCode(faker.address().zipCode());
        dto.setCountryCode("US");
        return dto;
    }

    public static List<AddressDto> getAddresses(String cellValue) {
        return Arrays.asList(getAddress(cellValue));
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

    public static Map<String,String> identifierTypeDtoValue(String url){
        RestTemplate rt=new RestTemplate();
        ResponseEntity<IdentifierDto[]> foo=rt.getForEntity(url, IdentifierDto[].class);

        IdentifierDto[] dtos=foo.getBody();

        Map<String,String> mapOfLookupIdentifiers=new HashMap<>();

        for(IdentifierDto tempIdentifierTypeDto:dtos){
            mapOfLookupIdentifiers.put(tempIdentifierTypeDto.getDisplay(),tempIdentifierTypeDto.getOid());
        }
        return mapOfLookupIdentifiers;
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
             String orgUrl = "http://localhost:8444/organizations/search?searchType=name&searchValue=" + name;
         RestTemplate rt = new RestTemplate();
         ResponseEntity<TempOrganizationDto> foo = rt.getForEntity(orgUrl, TempOrganizationDto.class);

         TempOrganizationDto tempOrganizationDto = foo.getBody();

         List<Element> elements = tempOrganizationDto.getElements();

         return elements.stream().findFirst().get().getLogicalId();

     }

     public static String getActivityDefinitionId(String orgId,String name){
         String activityDefinitionUrl="http://localhost:8444/organizations/"+orgId+"/activity-definitions/definition-reference?name="+name;
         RestTemplate rt=new RestTemplate();
         ResponseEntity<String> foo=rt.getForEntity(activityDefinitionUrl, String.class);
         return foo.getBody();
     }

     public static String getPractitionerId(String name){
         String practitionerUrl="http://localhost:8444/practitioners/practitioner-id?practitionerName="+name;
         RestTemplate rt=new RestTemplate();
         ResponseEntity<String> foo=rt.getForEntity(practitionerUrl, String.class);
         return foo.getBody();
     }

     public static String getTodoMainTask(String patient,String organization){
         String todoUrl="http://localhost:8444/tasks/task-references?definition=To-Do&patient="+patient+"&orgainization="+organization;
         RestTemplate rt=new RestTemplate();
         List<LinkedHashMap<String,String>> todo=rt.getForEntity(todoUrl,ArrayList.class).getBody();

         if(!todo.isEmpty()) {
             return todo.get(0).get("reference");
         }else{
             return null;
         }
     }

}
