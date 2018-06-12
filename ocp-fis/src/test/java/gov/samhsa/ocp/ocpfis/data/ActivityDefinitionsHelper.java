package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.activitydefinition.TempActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.data.model.activitydefinition.TempPeriodDto;
import gov.samhsa.ocp.ocpfis.data.model.activitydefinition.TempTimingDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ActivityDefinitionsHelper {

    public static void process(Sheet activityDefinitions, Map<String, String> mapOrganizations) {
        log.info("last row number :"+activityDefinitions.getLastRowNum());

        int rowNum=0;

        Map<String, ValueSetDto> publicationStatusLookups = CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/publication-status");
        Map<String,ValueSetDto> topicLookups=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/definition-topic");
        Map<String,ValueSetDto> actionParticipantTypeLookups=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/action-participant-type");
        Map<String,ValueSetDto> actionParticipantRoleLookups=CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/action-participant-role");
        Map<String,ValueSetDto> actionResourceTypeLookups= CommonHelper.getLookupValueSet(DataConstants.serverUrl + "lookups/resource-type");

        List<TempActivityDefinitionDto> activityDefinitionDtos=new ArrayList<>();

        for(Row row: activityDefinitions){
            if(rowNum>0){
                int j=0;
                TempActivityDefinitionDto dto=new TempActivityDefinitionDto();
                TempPeriodDto periodDto=new TempPeriodDto();
                for(Cell cell: row){
                    String cellValue=new DataFormatter().formatCellValue(cell);

                    if(j==0){
                        dto.setPublisher("Organization/"+mapOrganizations.get(cellValue));
                    }else if(j==1){
                        dto.setVersion(cellValue);
                    }else if(j==2){
                        dto.setName(cellValue);
                    } else if(j==3){
                        dto.setTitle(cellValue);
                    } else if(j==4){
                        dto.setStatus(publicationStatusLookups.get(cellValue));
                    } else if(j==5){
                        dto.setDate(String.valueOf(cellValue));
                    } else if(j==6){
                        dto.setDescription(cellValue);
                    } else if(j==7){
                       periodDto.setStart(String.valueOf(cellValue));
                    } else if(j==8){
                        periodDto.setEnd(String.valueOf(cellValue));
                        dto.setEffectivePeriod(periodDto);
                    } else if(j==9){
                        dto.setTopic(topicLookups.get(cellValue));
                    } else if(j==10){
                        dto.setKind(actionResourceTypeLookups.get(cellValue));
                    } else if(j==11){
                        dto.setActionParticipantType(actionParticipantTypeLookups.get(cellValue));
                    }else if(j==12){
                        dto.setActionParticipantRole(actionParticipantRoleLookups.get(cellValue));
                    }else if(j==13){
                        TempTimingDto timingDto =new TempTimingDto();
                        timingDto.setDurationMax(cellValue);
                        dto.setTiming(timingDto);
                    }

                    j++;
                }
                activityDefinitionDtos.add(dto);
            }
            rowNum++;
        }
        RestTemplate rt=new RestTemplate();

        activityDefinitionDtos.stream().filter(activityDef->!activityDef.getPublisher().split("/")[1].equalsIgnoreCase("null")).forEach(activityDefinitionDto->{
            try {
                log.info("activityDefinitionDto : " + activityDefinitionDto);
                HttpEntity<TempActivityDefinitionDto> request = new HttpEntity<>(activityDefinitionDto);
                rt.postForObject(DataConstants.serverUrl + "organizations/" + activityDefinitionDto.getPublisher().split("/")[1] + "/activity-definitions", request, TempActivityDefinitionDto.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
