package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
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
public class OrganizationHelper {

    private static DataFormatter dataFormatter = new DataFormatter();


    public static void process(Sheet organizations) {
        log.info("last row number : " + organizations.getLastRowNum());
        int rowNum = 0;

        List<OrganizationDto> organizationDtos = new ArrayList<>();

        for (Row row : organizations) {
            if (rowNum > 0) {
                int j = 0;
                OrganizationDto dto = new OrganizationDto();
                for (Cell cell : row) {
                    String cellValue = dataFormatter.formatCellValue(cell);

                    if (j == 0) {
                        dto.setName(cellValue);
                    } else if (j == 2) {
                        dto.setAddresses(CommonHelper.getAddresses(cellValue));
                    } else if (j == 3) {
                        dto.setTelecoms(CommonHelper.getTelecoms("phone",cellValue));
                    } else if (j == 4) {
                        dto.setIdentifiers(CommonHelper.getIdentifiers("Organization Tax ID", cellValue));
                    } else if (j == 5) {
                        dto.setActive(true);
                    }
                    j++;
                }
                organizationDtos.add(dto);
            }
            rowNum++;
        }

        RestTemplate rt = new RestTemplate();

        String fooResourceUrl = "http://localhost:8444/organizations";

        organizationDtos.forEach(organizationDto -> {
            try {
                HttpEntity<OrganizationDto> request = new HttpEntity<>(organizationDto);
                OrganizationDto foo = rt.postForObject(fooResourceUrl, request, OrganizationDto.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
