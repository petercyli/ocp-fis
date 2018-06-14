package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
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

        OrganizationDto orgDto=new OrganizationDto();
        orgDto.setName("Omnibus Care Plan (SAMSHA)");
        orgDto.setAddresses(CommonHelper.getAddresses("5600 Fishers Lane| Rockville| MD| 20857|US"));
        orgDto.setIdentifiers(CommonHelper.getIdentifiers("Organization Tax Id","530196960"));
        List<TelecomDto> telecomDtos=new ArrayList<>();
        telecomDtos.addAll(CommonHelper.getTelecoms("phone","(240)276-2827"));
        telecomDtos.addAll(CommonHelper.getTelecoms("email","Kenneth.Salyards@SAMHSA.hhs.gov"));
        orgDto.setTelecoms(telecomDtos);
        organizationDtos.add(orgDto);

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

        String fooResourceUrl = DataConstants.serverUrl + "organizations";

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
