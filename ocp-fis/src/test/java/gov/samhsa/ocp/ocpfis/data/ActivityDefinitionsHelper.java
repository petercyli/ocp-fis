package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ActivityDefinitionsHelper {

    public static void process(Sheet activityDefinitions, Map<String, String> mapOrganizations) {
        log.info("last row number :"+activityDefinitions.getLastRowNum());

        int rowNum=0;

        List<ActivityDefinitionDto> activityDefinitoinDto=new ArrayList<>();

        for(Row row: activityDefinitions){
            if(rowNum>0){
                int j=0;
                ActivityDefinitionDto dto=new ActivityDefinitionDto();
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

                    }
                }
            }
        }
    }
}
