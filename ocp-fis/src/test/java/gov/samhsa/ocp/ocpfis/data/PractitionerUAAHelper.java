package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PractitionerUAAHelper {

    public static void createPractitioners(Map<String, String> practitionersMap, List<PractitionerDto> list) {

        for (Map.Entry<String, String> entry : practitionersMap.entrySet()) {

            Optional<PractitionerDto> oPractitionerDto = list.stream().filter(practitionerDto -> {
                NameDto nameDto = practitionerDto.getName().stream().findFirst().get();
                if (entry.getKey().contains(nameDto.getFirstName()) && entry.getKey().contains(nameDto.getLastName())) {
                    return true;
                }
                return false;
            }).findAny();

            if (oPractitionerDto.isPresent()) {
                String organizationId = "";
                String uaaRole = "";

                PractitionerDto dto = oPractitionerDto.get();
                uaaRole = dto.getUaaRole();
                log.info("uaaRole : " + uaaRole);

                PractitionerRoleDto roleDto = dto.getPractitionerRoles().stream().findFirst().get();
                organizationId = roleDto.getOrganization().getReference().replace(ResourceType.Organization + "/", "");
                log.info("organization : " + organizationId);

                UAAHelper.createEntityInUAA(entry.getKey(), entry.getValue(), uaaRole, organizationId, 1);
            }
        }

    }

}
