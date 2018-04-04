package gov.samhsa.ocp.ocpfis.domain;

import java.util.Arrays;
import java.util.stream.Stream;

public enum DateRangeEnum {
    ONE_DAY (1),
    ONE_WEEK (7),
    ONE_MONTH (30),
    ALL (0);

    private final int day;

    DateRangeEnum(int day) {
        this.day = day;
    }

    public static Stream<DateRangeEnum> asStream() {
        return Arrays.stream(values());
    }

    public int getDay() {
        return day;
    }

}
