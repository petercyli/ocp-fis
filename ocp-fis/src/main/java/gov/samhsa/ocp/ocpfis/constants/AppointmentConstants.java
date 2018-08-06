package gov.samhsa.ocp.ocpfis.constants;

public final class AppointmentConstants {
    public static final String ACCEPTED_PARTICIPATION_STATUS = "accepted";
    public static final String DECLINED_PARTICIPATION_STATUS = "declined";
    public static final String TENTATIVE_PARTICIPATION_STATUS = "tentative";
    public static final String NEEDS_ACTION_PARTICIPATION_STATUS = "needs-action";

    public static final String PENDING_APPOINTMENT_STATUS = "pending";
    public static final String BOOKED_APPOINTMENT_STATUS = "booked";
    public static final String CANCELLED_APPOINTMENT_STATUS = "cancelled";
    public static final String PROPOSED_APPOINTMENT_STATUS = "proposed";

    public static final String REQUIRED = "required";
    public static final String INFORMATION_ONLY = "information-only";

    public static final String ATTENDER_PARTICIPANT_TYPE_CODE = "ATND";
    public static final String ATTENDER_PARTICIPANT_TYPE_DISPLAY = "attender";
    public static final String AUTHOR_PARTICIPANT_TYPE_CODE = "AUT";
    public static final String AUTHOR_PARTICIPANT_TYPE_DISPLAY = "author (originator)";

    public static final String PATIENT_ACTOR_REFERENCE = "Patient";
    public static final String DATE_TIME_FORMATTER_PATTERN_DATE = "MM/dd/yyyy";

    // PRIVATE //
    private AppointmentConstants(){
        throw new AssertionError();
    }
}
