package gov.samhsa.ocp.ocpfis.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class RolesUAAHelper {

    public static void createRoles() {
        int[] inputs = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

        Arrays.stream(inputs).forEach(i -> {
            runScript(i);
        });
    }

    private static void runScript(int choice) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./create-role-with-scope.sh");
            System.out.println("created processbuilder..");

            final Process process = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            PrintWriter pw = new PrintWriter(process.getOutputStream());
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
                pw.println(choice);
                pw.flush();
            }
            System.out.println("Program terminated!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
