package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class CommunicationsHelper {

    public static void process(Sheet communications, Map<String, String> mapOfPatients, Map<String,String> mapOfPractitioners) {
        log.info("last row number: "+ communications.getLastRowNum());
        Map<String,String> statusLookup=CommonHelper.getLookup(DataConstants.serverUrl + "lookups/communication-statuses");
        Map<String,ValueSetDto> notDoneReasonValueSetLookup=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/communication-not-done-reasons");
        Map<String,ValueSetDto> categoryValueSetLookup=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/communication-categories");
        Map<String,ValueSetDto> contactMediumValueSetLookup=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/communication-mediums");
        int rowNum=0;

        List<CommunicationDto> communicationDtos=new ArrayList<>();
        for(Row row:communications){
            if(rowNum>0){
                int j=0;
                CommunicationDto dto=new CommunicationDto();
                String sentDateTime=null;
                for(Cell cell:row){
                    String cellValue=new DataFormatter().formatCellValue(cell);

                    if(j==0){
                        ReferenceDto referenceDto=new ReferenceDto();
                        referenceDto.setReference("Practitioner/"+mapOfPractitioners.get(cellValue));
                        referenceDto.setDisplay(cellValue);
                        dto.setSender(referenceDto);
                    }else if(j==1){
                       sentDateTime=cellValue;
                    }else if(j==2){
                        sentDateTime=sentDateTime+" "+cellValue;
                        dto.setSent(sentDateTime);
                    } else if(j==3){
                        dto.setStatusCode(statusLookup.get(cellValue));
                    }else if(j==4){
                        dto.setCategoryCode(categoryValueSetLookup.get(cellValue).getCode());
                        dto.setCategoryValue(categoryValueSetLookup.get(cellValue).getDisplay());
                    }else if(j==5){
                        dto.setMediumCode(contactMediumValueSetLookup.get(cellValue).getCode());
                        dto.setMediumValue(contactMediumValueSetLookup.get(cellValue).getDisplay());
                    }else if(j==6){
                        ReferenceDto referenceDto=new ReferenceDto();
                        referenceDto.setReference("Patient/"+mapOfPatients.get(cellValue));
                        referenceDto.setDisplay(cellValue);
                        dto.setSubject(referenceDto);
                    }else if(j==7){
                        dto.setPayloadContent(cellValue);
                    }else if(j==8){
                        dto.setNote(cellValue);
                    }else if(j==9){
                        ReferenceDto referenceDto=new ReferenceDto();
                        referenceDto.setReference("Practitioner/"+mapOfPractitioners.get(cellValue));
                        referenceDto.setDisplay(cellValue);
                        dto.setRecipient(Arrays.asList(referenceDto));
                    }else if(j==10){
                        if(!cellValue.isEmpty()) {
                            if ((cellValue.equalsIgnoreCase("true"))) {
                                dto.setNotDone(true);
                            }
                        }
                    }else if(j==11){
                        if(!cellValue.isEmpty()) {
                            dto.setNotDoneReasonCode(notDoneReasonValueSetLookup.get(cellValue).getCode());
                            dto.setNotDoneReasonValue(notDoneReasonValueSetLookup.get(cellValue).getDisplay());
                        }
                    }
                    j++;
                }
                communicationDtos.add(dto);
            }
            rowNum++;
        }

        RestTemplate rt=new RestTemplate();

        communicationDtos.forEach(communicationDto -> {
            log.info("communications : "+communicationDto);

            try {
                    HttpEntity<CommunicationDto> request = new HttpEntity<>(communicationDto);
                    rt.postForObject(DataConstants.serverUrl + "communications/", request, CommunicationDto.class);
            } catch (Exception e) {
                log.info("This communications could not be posted : " + communicationDto);
                e.printStackTrace();

            }

        });



    }
}
