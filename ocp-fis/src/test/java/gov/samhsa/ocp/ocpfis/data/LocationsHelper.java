package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.location.TempLocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
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
public class LocationsHelper {

    public static void process(Sheet locations, Map<String, String> mapOrganizations) {
        log.info("last row number : " + locations.getLastRowNum());

        int rowNum = 0;

        List<TempLocationDto> locationDtos = new ArrayList<>();

        for (Row row : locations) {
            if (rowNum > 0) {
                int j = 0;
                TempLocationDto dto = new TempLocationDto();
                for (Cell cell : row) {
                    String cellValue = new DataFormatter().formatCellValue(cell);

                    if (j == 0) {
                        //get the id of the organization
                        dto.setManagingOrganization(mapOrganizations.get(cellValue));

                    } else if (j == 1) {
                        dto.setName(cellValue);

                    } else if (j == 2) {
                        dto.setAddress(CommonHelper.getAddress(cellValue));

                    } else if (j == 3) {
                        dto.setTelecoms(CommonHelper.getTelecoms(cellValue));

                    } else if (j == 5) {
                        dto.setIdentifiers(CommonHelper.getIdentifiers("Organization Tax ID", cellValue + "-" + System.currentTimeMillis()));

                    } else if (j == 6) {
                        dto.setStatus("active");
                    }
                    j++;
                }
                locationDtos.add(dto);
            }
            rowNum++;
        }

        RestTemplate rt = new RestTemplate();

        locationDtos.forEach(locationDto -> {
            HttpEntity<TempLocationDto> request = new HttpEntity<>(locationDto);

            List<IdentifierDto> identifierDtos = locationDto.getIdentifiers();

            identifierDtos.forEach(identifierDto -> {
                log.info("system : " + identifierDto.getSystem());
                log.info("value : " + identifierDto.getValue());
            });

            LocationDto foo = rt.postForObject("http://localhost:8444/organization/" + locationDto.getManagingOrganization() + "/locations", request, LocationDto.class);
        });
    }
}
