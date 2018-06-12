package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
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
import java.util.Optional;

@Slf4j
public class CareTeamsHelper {

    public static void process(Sheet careTeams, Map<String, String> mapOfPractitioners, Map<String, String> mapOfPatients) {
        log.info("last row number : " + careTeams.getLastRowNum());

        int rowNum = 0;

        Map<String, String> careTeamsCategories = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/care-team-categories");
        Map<String, String> careTeamReasonCodes = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/care-team-reasons");
        Map<String, String> participantRoles = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/participant-roles");

        List<CareTeamDto> careTeamDtos = new ArrayList<>();

        for (Row row : careTeams) {
            if (rowNum > 0) {
                int j = 0;
                CareTeamDto dto = new CareTeamDto();
                ParticipantDto participantDto = new ParticipantDto();

                for (Cell cell : row) {
                    String cellValue = new DataFormatter().formatCellValue(cell);

                    if (j == 0) {
                        //patient

                        String[] name = cellValue.split(" ");

                        if (name.length > 1) {
                            dto.setSubjectLastName(name[1].trim());
                            dto.setSubjectFirstName(name[0].trim());
                            dto.setSubjectId(mapOfPatients.get(cellValue));
                        }

                    } else if (j == 1) {
                        //care team name

                        dto.setName(cellValue.trim());

                    } else if (j == 2) {
                        //category

                        dto.setCategoryCode(careTeamsCategories.get(cellValue.trim()));
                        dto.setCategoryDisplay(cellValue.trim());

                    } else if (j == 3) {
                        //status

                        dto.setStatusCode("active");
                        dto.setStatusDisplay("Active");

                    }  else if (j == 4) {
                        //reason
                        dto.setReasonCode(careTeamReasonCodes.get(cellValue.trim()));
                        dto.setReasonDisplay(cellValue.trim());

                    } else if (j == 5) {
                        //start date

                        dto.setStartDate("01/01/2018");

                    } else if (j == 6) {
                        //end date

                        dto.setEndDate("01/01/2020");

                    } else if (j == 7) {
                        //participant

                        participantDto.setMemberId(mapOfPractitioners.get(cellValue));
                        participantDto.setMemberLastName(Optional.of(cellValue));
                        participantDto.setMemberFirstName(Optional.of(""));

                    } else if (j == 8) {
                        //participant role

                        participantDto.setRoleCode(participantRoles.get(cellValue));
                        participantDto.setMemberType(participantRoles.get(cellValue));
                        participantDto.setRoleDisplay(cellValue);
                        participantDto.setStartDate("01/01/2018");
                        participantDto.setEndDate("01/01/2020");

                        dto.setParticipants(Arrays.asList(participantDto));
                    }

                    j++;
                }

                careTeamDtos.add(dto);
            }
            rowNum++;
        }

        RestTemplate rt = new RestTemplate();


        careTeamDtos.forEach(careTeamDto -> {
            try {
                if(careTeamDto.getSubjectId() != null && careTeamDto.getParticipants() != null) {
                    log.info("Getting ready to post: " + careTeamDto);
                    HttpEntity<CareTeamDto> request = new HttpEntity<>(careTeamDto);
                    rt.postForObject(DataConstants.serverUrl + "care-teams/", request, CareTeamDto.class);
                }
            } catch (Exception e) {
                log.info("This careteam could not be posted : " + careTeamDto);
                e.printStackTrace();

            }

        });

    }
}
