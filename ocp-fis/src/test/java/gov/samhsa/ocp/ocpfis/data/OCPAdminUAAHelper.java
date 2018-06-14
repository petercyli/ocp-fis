package gov.samhsa.ocp.ocpfis.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class OCPAdminUAAHelper {

    public static void createOCPAdmin() {

        UAAHelper.createEntityInUAA("James Joyce", null, "ocp.role.ocpAdmin", null, 3);

    }
}
