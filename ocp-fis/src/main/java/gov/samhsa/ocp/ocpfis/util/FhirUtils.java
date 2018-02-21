package gov.samhsa.ocp.ocpfis.util;

import org.hl7.fhir.dstu3.model.Enumerations;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.time.*;


public class FhirUtils {

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

    public static Date convertToDate(String dateString) throws ParseException {
        DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        if (dateString != null) {
            Date date = format.parse(dateString);
            return date;
        }

        return null;
    }

    public static LocalDate convertToLocalDate(Date date) {
        //the system default time zone will be appended
        ZoneId defaultZoneId = ZoneId.systemDefault();

        //1. Convert Date -> Instant
        Instant instant = date.toInstant();

        //2. Instant + system default time zone + toLocalDate() = LocalDate
        LocalDate localDate = instant.atZone(defaultZoneId).toLocalDate();

        return localDate;
    }

    public static LocalDateTime convertToLocalDateTime(Date date) {
        //the system default time zone will be appended
        ZoneId defaultZoneId = ZoneId.systemDefault();

        //1. Convert Date -> Instant
        Instant instant = date.toInstant();

        //2. Instant + system default time zone + toLocalDateTime() = LocalDateTime
        LocalDateTime localDateTime = instant.atZone(defaultZoneId).toLocalDateTime();

        return localDateTime;
    }

    public static String convertToString(Date date) {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        if (date != null) {
            return df.format(date);
        }

        return "";
    }
}
