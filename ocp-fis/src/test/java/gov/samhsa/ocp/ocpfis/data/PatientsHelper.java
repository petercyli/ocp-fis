package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointSystem;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointUse;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PatientsHelper {

    public static void process(Sheet patients, Map<String, String> mapOfPractitioners, Map<String, String> mapOfOrganizations) {
        int rowNum = 0;

        List<PatientDto> patientDtos = new ArrayList<>();
        Map<String, String> genderCodeLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/administrative-genders");
        Map<String, String> birthSexLookup=CommonHelper.getLookup(DataConstants.serverUrl + "lookups/us-core-birthsexes");
        Map<String, String> raceLookup=CommonHelper.getLookup(DataConstants.serverUrl + "lookups/us-core-races");
        Map<String,String> ethnicityLookup=CommonHelper.getLookup(DataConstants.serverUrl + "lookups/us-core-ethnicities");
        Map<String, String> languageLookup=CommonHelper.getLookup(DataConstants.serverUrl + "lookups/languages");
        Map<String,String> identifierTypeLookup=CommonHelper.identifierTypeDtoValue(DataConstants.serverUrl + "lookups/identifier-systems");
        for (Row row : patients) {
            if (rowNum > 0) {
                int j = 0;
                PatientDto dto = new PatientDto();
                NameDto nameDto = new NameDto();
                IdentifierDto tempIdentifiereDto=new IdentifierDto();
                for (Cell cell : row) {
                    String cellValue = new DataFormatter().formatCellValue(cell);

                    if (j == 0) {
                        nameDto.setFirstName(cellValue);
                    }
                    else if (j == 1) {
                        nameDto.setLastName(cellValue);
                        dto.setName(Arrays.asList(nameDto));
                    }
                    else if (j == 2) {
                        //TODO: fix the date issue
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                        String date = "11/08/1980";
                        LocalDate localDate = LocalDate.parse(date, formatter);
                        dto.setBirthDate(localDate);
                    }
                    else if (j == 3) {
                        dto.setGenderCode(genderCodeLookup.get(cellValue));
                    }
                    else if (j == 4) {
                        dto.setBirthSex(birthSexLookup.get(cellValue));
                    }
                    else if(j==5){
                        dto.setRace(raceLookup.get(cellValue));
                    }
                    else if(j==6){
                        dto.setEthnicity(ethnicityLookup.get(cellValue));
                    }
                    else if(j==7){
                        dto.setLanguage(languageLookup.get(cellValue));
                    }
                    else if(j==8){
                        tempIdentifiereDto.setSystem(identifierTypeLookup.get(cellValue));
                    }
                    else if(j==9){
                        tempIdentifiereDto.setValue(cellValue);
                        dto.setIdentifier(Arrays.asList(tempIdentifiereDto));
                    }
                    else if(j==10){
                        TelecomDto telecomDto=new TelecomDto();
                        telecomDto.setSystem(java.util.Optional.of(ContactPointSystem.PHONE.toCode()));
                        telecomDto.setUse(java.util.Optional.of(ContactPointUse.WORK.toCode()));
                        telecomDto.setValue(java.util.Optional.ofNullable(cellValue));
                        dto.setTelecoms(Arrays.asList(telecomDto));
                    }
                    else if(j==11){
                        dto.setAddresses(CommonHelper.getAddresses(cellValue));
                    }
                    else if(j==12){
                        dto.setOrganizationId(Optional.of(mapOfOrganizations.get(cellValue.trim())));
                    }
                    else if(j==13){
                       dto.setPractitionerId(Optional.of(mapOfPractitioners.get(cellValue.trim())));
                    }
                    j++;
                }
                patientDtos.add(dto);
            }
            rowNum++;
        }

        RestTemplate rt=new RestTemplate();

        patientDtos.stream().forEach(patientDto->{
            try {
                HttpEntity<PatientDto> request = new HttpEntity<>(patientDto);
                rt.postForObject(DataConstants.serverUrl + "patients/", request, PatientDto.class);
            } catch (Exception e) {
                log.info("This patient could not be posted : " + patientDto);
                e.printStackTrace();
            }
        });
    }

}
