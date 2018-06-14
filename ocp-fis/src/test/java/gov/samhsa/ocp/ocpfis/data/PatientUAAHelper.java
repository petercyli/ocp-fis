package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PatientUAAHelper {

    public static void createPatients(Map<String, String> patientsMap, Map<String, String> organizationsMap, List<PatientDto> patientsSheet) {
        for (Map.Entry<String, String> entry : patientsMap.entrySet()) {

            Optional<PatientDto> oPatientRow = patientsSheet.stream().filter(row -> {
                NameDto name = row.getName().stream().findFirst().get();
                if (entry.getKey().contains(name.getFirstName()) && entry.getKey().contains(name.getLastName())) {
                    return true;
                }
                return false;
            }).findAny();

            if (oPatientRow.isPresent()) {

                String organizationId = oPatientRow.get().getOrganizationId().get();
                log.info("key " + entry.getKey());
                log.info("organization : " + oPatientRow.get().getOrganizationId().get());
                log.info("practitioner : " + oPatientRow.get().getPractitionerId().get());

                UAAHelper.createEntityInUAA(entry.getKey(), entry.getValue(), "ocp.role.patient", organizationId, 2);
            }
        }

    }
}
