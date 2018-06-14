package gov.samhsa.ocp.ocpfis.data;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class ValueSetHelper {

    public static void process(final String valueSetsDir) {
        Gson gson = new Gson();

        File[] files = listFiles(new File(valueSetsDir));

        RestTemplate rt = new RestTemplate();

        for (File file : files) {
            String fileName = file.getAbsolutePath();
            log.info("file " + fileName);

            try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
                JsonParser parser = new JsonParser();
                parser.parse(br);

            } catch (JsonSyntaxException | IOException e ) {
                e.printStackTrace();

            }

            log.info("Finished checking json validity ...");

            try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
                ValueSetDto valueSetDto = gson.fromJson(br, ValueSetDto.class);

                log.info("Value retrieved and set in the object");

                HttpEntity<ValueSetDto> request = new HttpEntity<>(valueSetDto);
                ResponseEntity<ValueSetDto> response = rt.exchange(getUrl(), HttpMethod.PUT, request, ValueSetDto.class);

                log.info("response : " + response.getStatusCode());

            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();

            }

        }
    }

    private static File[] listFiles(final File folder) {
        return folder.listFiles();
    }

    private static String getUrl() {
        return DataConstants.serverUrl + "valuesets";
    }


}
