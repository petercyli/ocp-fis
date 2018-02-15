package gov.samhsa.ocp.ocpfis.domain;

public class SearchKeyEnum {

    public enum SearchType {
        NAME, IDENTIFIER;
    }

    public enum LocationSearchKey {
        /**
         * Locations can be searched based on the following keys
         */
        NAME, LOGICALID, IDENTIFIERVALUE;

        public static boolean contains(String s) {
            for (LocationSearchKey locationSearchKey : values())
                if (locationSearchKey.name().equalsIgnoreCase(s)) {
                    return true;
                }
            return false;
        }
    }

    public enum HealthcareServiceSearchKey {
        /**
         * Healthcare Services can be searched based on the following keys
         */
        NAME, LOGICALID, IDENTIFIERVALUE;

        public static boolean contains(String s) {
            for (HealthcareServiceSearchKey healthcareServiceSearchKey : values())
                if (healthcareServiceSearchKey.name().equalsIgnoreCase(s)) {
                    return true;
                }
            return false;
        }
    }
}
