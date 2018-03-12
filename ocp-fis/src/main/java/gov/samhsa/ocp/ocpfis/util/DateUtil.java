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
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");

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

    /**
     * Returns true if endDate is after startDate or if startDate equals endDate.
     * Returns false if either value is null.  If equalOK, returns true if the
     * dates are equal.
     **/
    public static boolean isValidDateRange(Date startDate, Date endDate, boolean equalOK) {
        // false if either value is null
        if (startDate == null || endDate == null) { return false; }

        if (equalOK) {
            // true if they are equal
            if (startDate.equals(endDate)) { return true; }
        }

        // true if endDate after startDate
        return endDate.after(startDate);

    }

    /**
     * Returns true if endDateTime is after startDateTime or if startDateTime equals endDateTime.
     * Returns false if either value is null.  If equalOK, returns true if the
     * datesTimes are equal.
     **/
    public static boolean isValidDateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime, boolean equalOK) {
        // false if either value is null
        if (startDateTime == null || endDateTime == null) { return false; }

        if (equalOK) {
            // true if they are equal
            if (startDateTime.equals(endDateTime)) { return true; }
        }

        // true if endDateTime after startDateTime
        return endDateTime.isAfter(startDateTime);

    }

}
