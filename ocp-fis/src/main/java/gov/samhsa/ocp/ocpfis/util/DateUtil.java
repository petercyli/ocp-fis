package gov.samhsa.ocp.ocpfis.util;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

@Slf4j
public class DateUtil {

    public static Date convertStringToDate(String dateString) throws ParseException {
        DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        if (dateString != null) {
            return format.parse(dateString);
        }
        return null;
    }

    public static LocalDate convertDateToLocalDate(Date date) {
        //the system default time zone will be appended
        ZoneId defaultZoneId = ZoneId.systemDefault();

        //1. Convert Date -> Instant
        Instant instant = date.toInstant();

        //2. Instant + system default time zone + toLocalDate() = LocalDate

        return instant.atZone(defaultZoneId).toLocalDate();
    }

    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        //the system default time zone will be appended
        ZoneId defaultZoneId = ZoneId.systemDefault();

        //1. Convert Date -> Instant
        Instant instant = date.toInstant();

        //2. Instant + system default time zone + toLocalDateTime() = LocalDateTime

        return instant.atZone(defaultZoneId).toLocalDateTime();
    }

    public static String convertDateToString(Date date) {
        DateFormat df = new SimpleDateFormat("MM/dd/YYYY", Locale.US);

        if (date != null) {
            return df.format(date);
        }
        return "";
    }

    public static String convertLocalDateToString(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        if(date != null) {
            return formatter.format(date);
        }
        return "";
    }

    public static Date convertLocalDateTimeToDate(LocalDateTime dateTime){
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

}
