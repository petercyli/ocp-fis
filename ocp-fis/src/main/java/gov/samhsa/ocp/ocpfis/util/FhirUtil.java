package gov.samhsa.ocp.ocpfis.util;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations;

@Slf4j
public class FhirUtil {
    public static Enumerations.AdministrativeGender getPatientGender(String codeString) {
        switch (codeString.toUpperCase()) {
            case "MALE":
                return Enumerations.AdministrativeGender.MALE;
            case "M":
                return Enumerations.AdministrativeGender.MALE;
            case "FEMALE":
                return Enumerations.AdministrativeGender.FEMALE;
            case "F":
                return Enumerations.AdministrativeGender.FEMALE;
            case "OTHER":
                return Enumerations.AdministrativeGender.OTHER;
            case "O":
                return Enumerations.AdministrativeGender.OTHER;
            case "UNKNOWN":
                return Enumerations.AdministrativeGender.UNKNOWN;
            case "UN":
                return Enumerations.AdministrativeGender.UNKNOWN;
            default:
                return Enumerations.AdministrativeGender.UNKNOWN;

        }
    }

    public static Coding getCoding(String code, String display, String system) {
        Coding coding = new Coding();
        if (isStringNotNullAndNotEmpty(code)) {
            coding.setCode(code);
        }

        if (isStringNotNullAndNotEmpty(display)) {
            coding.setDisplay(display);
        }

        if (isStringNotNullAndNotEmpty(system)) {
            coding.setSystem(system);
        }
        return coding;
    }

    public static boolean isStringNotNullAndNotEmpty(String givenString) {
        return givenString != null && !givenString.trim().isEmpty();
    }
}
