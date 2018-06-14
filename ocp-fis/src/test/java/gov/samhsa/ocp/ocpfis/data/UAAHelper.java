package gov.samhsa.ocp.ocpfis.data;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@Slf4j
public class UAAHelper {

    public static void createEntityInUAA(String key, String value, String role, String organizationId, int fhirResourceType) {
        log.info("Creating UAA record for entity : " + key + " with id : " + value + " and role : " + role + " and organizationId : " + organizationId);

        final String[] name = key.split("\\s+");
        String firstName = "";
        String lastName = "";

        if (name.length == 2) {
            firstName = name[0];
            lastName = name[1];
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./create-user-with-attributes.sh");

            log.info("created processbuilder..");

            final Process process = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            PrintWriter pw = new PrintWriter(process.getOutputStream());
            String line;

            while ((line = br.readLine()) != null) {
                log.info(line);
                if (line.contains("Please enter firstName")) {
                    pw.println(firstName);
                    log.info("Entered firstName : " + firstName);
                    pw.flush();

                } else if (line.contains("Please enter lastName")) {
                    pw.println(lastName);
                    log.info("Entered lastName : " + lastName);
                    pw.flush();

                } else if (line.contains("Please enter username")) {
                    pw.println(firstName);
                    log.info("Entered username : " + firstName);
                    pw.flush();

                } else if (line.contains("Please enter password")) {
                    pw.println("P@ssword123");
                    log.info("Entered password : ***********");
                    pw.flush();

                } else if (line.contains("Please enter EMAIL")) {
                    pw.println(firstName + "." + lastName + "@gmail.com");
                    log.info("Entered email :" + firstName + "." + lastName + "@gmail.com");
                    pw.flush();

                } else if (line.contains("Please enter role")) {
                    pw.println(role);
                    log.info("Entered role : " + role);
                    pw.flush();

                } else if (line.contains("Enter fhir resource type")) {
                    pw.println(fhirResourceType);
                    log.info("Entered fhir type: " + fhirResourceType);
                    pw.flush();

                } else if (line.contains("enter the Org ID")) {
                    pw.println(organizationId);
                    log.info("Entered organization ID: 53");
                    pw.flush();

                } else if (line.contains("fhir resource id")) {
                    pw.println(value);
                    log.info("Entered id of the practitioner : " + value);
                    pw.flush();

                }

            }
            log.info("Program terminated!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
