package gov.samhsa.ocp.ocpfis.domain;

import java.util.Arrays;

public class SearchKeyEnum {

    public enum CommonSearchKey {
        NAME, IDENTIFIER
    }

    public enum RelatedPersonSearchKey {
        NAME, PATIENTID
    }

    public enum LocationSearchKey {
        /**
         * Locations can be searched based on the following keys
         */
        NAME, LOGICALID, IDENTIFIERVALUE;

        public static boolean contains(String s) {
            return Arrays.stream(values()).anyMatch(locationSearchKey -> locationSearchKey.name().equalsIgnoreCase(s));
        }
    }

    public enum HealthcareServiceSearchKey {
        /**
         * Healthcare Services can be searched based on the following keys
         */
        NAME, LOGICALID, IDENTIFIERVALUE;

        public static boolean contains(String s) {
            return Arrays.stream(values()).anyMatch(healthcareServiceSearchKey -> healthcareServiceSearchKey.name().equalsIgnoreCase(s));
        }
    }

    public enum AppointmentSearchKey {
        /**
         * Appointments be searched based on the following keys
         */
        LOGICALID;

        public static boolean contains(String s) {
            return Arrays.stream(values()).anyMatch(appointmentSearchKey -> appointmentSearchKey.name().equalsIgnoreCase(s));
        }
    }

    public enum AppointmentFilterKey {
        /**
         * Appointments be filtered based on the following keys
         */
        TODAY, WEEK, MONTH;

        public static boolean contains(String s) {
            return Arrays.stream(values()).anyMatch(AppointmentFilterKey -> AppointmentFilterKey.name().equalsIgnoreCase(s));
        }
    }

    public enum PatientFilterKey {
        ORGANIZATION, ASSOCIATECARETEAMPATIENT, UNASSIGNPATIENT;

        public static boolean contains(String s){
            return Arrays.stream(values()).anyMatch(PatientFilterKey->PatientFilterKey.name().equalsIgnoreCase(s));
        }
    }

}
