package gov.samhsa.ocp.ocpfis.service;

public class SearchKeyEnum {
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
}
