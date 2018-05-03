package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.patient.TempIdentifierDto;
import gov.samhsa.ocp.ocpfis.data.model.patient.TempIdentifierTypeDto;
import gov.samhsa.ocp.ocpfis.data.model.patient.TempPatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointSystem;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointUse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PatientsHelper {

    public static void process(Sheet patients) {
        int rowNum = 0;

        List<TempPatientDto> patientDtos = new ArrayList<>();
        Map<String, String> genderCodeLookup = CommonHelper.getLookup("http://localhost:8444/lookups/administrative-genders");
        Map<String, String> birthSexLookup=CommonHelper.getLookup("http://localhost:8444/lookups/us-core-birthsexes");
        Map<String, String> raceLookup=CommonHelper.getLookup("http://localhost:8444/lookups/us-core-races");
        Map<String,String> ethnicityLookup=CommonHelper.getLookup("http://localhost:8444/lookups/us-core-ethnicities");
        Map<String, String> languageLookup=CommonHelper.getLookup("http://localhost:8444/lookups/languages");
        Map<String,String> identifierTypeLookup=PatientsHelper.identifierTypeDtoValue("http://localhost:8444/lookups/identifier-systems");
        for (Row row : patients) {
            if (rowNum > 0) {
                int j = 0;
                TempPatientDto dto = new TempPatientDto();
                NameDto nameDto = new NameDto();
                TempIdentifierDto tempIdentifiereDto=new TempIdentifierDto();
                for (Cell cell : row) {
                    String cellValue = new DataFormatter().formatCellValue(cell);

                    if (j == 0) {
                        nameDto.setFirstName(cellValue);
                    }
                    if (j == 1) {
                        nameDto.setLastName(cellValue);
                        dto.setName(Arrays.asList(nameDto));
                    }
                    if (j == 2) {
                        dto.setBirthDate(cellValue);
                    }
                    if (j == 3) {
                        dto.setGenderCode(genderCodeLookup.get(cellValue));
                    }
                    if (j == 4) {
                        dto.setBirthSex(birthSexLookup.get(cellValue));
                    }
                    if(j==5){
                        dto.setRace(raceLookup.get(cellValue));
                    }
                    if(j==6){
                        dto.setEthnicity(ethnicityLookup.get(cellValue));
                    }
                    if(j==7){
                        dto.setLanguage(languageLookup.get(cellValue));
                    }
                    if(j==8){
                        tempIdentifiereDto.setSystem(identifierTypeLookup.get(cellValue));
                    }
                    if(j==9){
                        tempIdentifiereDto.setValue(cellValue);
                        dto.setIdentifier(Arrays.asList(tempIdentifiereDto));
                    }
                    if(j==10){
                        TelecomDto telecomDto=new TelecomDto();
                        telecomDto.setSystem(java.util.Optional.of(ContactPointSystem.PHONE.toCode()));
                        telecomDto.setUse(java.util.Optional.of(ContactPointUse.WORK.toCode()));
                        telecomDto.setValue(java.util.Optional.ofNullable(cellValue));
                        dto.setTelecoms(Arrays.asList(telecomDto));
                    }
                    if(j==11){
                        dto.setAddresses(CommonHelper.getAddresses(cellValue));
                    }
                    if(j==13){
                        dto.setOrganizationId(java.util.Optional.ofNullable(CommonHelper.getOrganizationId(cellValue)));
                    }
                }
            }
        }
    }

    public static Map<String,String> identifierTypeDtoValue(String url){
        RestTemplate rt=new RestTemplate();
        ResponseEntity<TempIdentifierTypeDto[]> foo=rt.getForEntity(url, TempIdentifierTypeDto[].class);

        TempIdentifierTypeDto[] dtos=foo.getBody();

        Map<String,String> mapOfLookupIdentifiers=new HashMap<>();

        for(TempIdentifierTypeDto tempIdentifierTypeDto:dtos){
            mapOfLookupIdentifiers.put(tempIdentifierTypeDto.getDisplay(),tempIdentifierTypeDto.getOid());
        }
        return mapOfLookupIdentifiers;
    }
}
