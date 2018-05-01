package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.organization.Element;
import gov.samhsa.ocp.ocpfis.data.model.organization.TempOrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OcpDataLoadApplication {

    private static final String XSLX_FILE = "C://data//OCP.xlsx";

    public static void main(String[] args) throws IOException, InvalidFormatException {
        //Create a workbook form excel file
        Workbook workbook = WorkbookFactory.create(new File(XSLX_FILE));

        //Check number of sheets
        log.info("Number of sheets : " + workbook.getNumberOfSheets());

        //Organizations
        Sheet organizations = workbook.getSheet("Organizations");
        //OrganizationHelper.process(organizations);

        //Get all organizations
        Map<String, String> mapOrganizations = retrieveOrganizations();
        log.info("Retrieved organizations");

        Sheet locations = workbook.getSheet("Locations");
        LocationsHelper.process(locations, mapOrganizations);
        log.info("Populated locations");

        Sheet healthCareServices = workbook.getSheet("Healthcare Services");
        HealthCareServicesHelper.process(healthCareServices, mapOrganizations);
        log.info("Populated healthCareServices");

        Sheet activityDefinitions = workbook.getSheet("Activity Definitions");
        ActivityDefinitionsHelper.process(activityDefinitions, mapOrganizations);
        log.info("Populated practitioners");

        Sheet practitioners = workbook.getSheet("Practitioners");
        PractitionersHelper.process(practitioners, mapOrganizations);
        log.info("Populated practitioners");

        Map<String, String> mapOfPractitioners = retrievePractitioners();

        Sheet patients = workbook.getSheet("Patient");
        PatientsHelper.process(patients);
        log.info("Populated organizations");

        Map<String, String> mapOfPatients = retrievePatients();

        Sheet relationPersons = workbook.getSheet("Patient Related Persons");
        RelatedPersonsHelper.process(relationPersons);
        log.info("Populated relationPersons");

        Sheet careTeams = workbook.getSheet("Patient Care Teams");
        CareTeamsHelper.process(careTeams, mapOfPractitioners);
        log.info("Populated careTeams");

        Sheet taskOwners = workbook.getSheet("Tasks");
        TasksHelper.process(taskOwners, mapOfPractitioners);
        log.info("Populated taskOwners");

        Sheet todos = workbook.getSheet("To Do");
        TodosHelper.process(todos, mapOfPractitioners);
        log.info("Populated todosHelper");

        Sheet communications = workbook.getSheet("Communication");
        CommunicationsHelper.process(communications, mapOfPatients);
        log.info("Populated communications");

        Sheet appointments = workbook.getSheet("Appointments");
        AppointmentsHelper.process(appointments, mapOfPatients, mapOfPractitioners);
        log.info("Populated appointments");

        workbook.close();
        log.info("Workbook closed");
    }

    private static Map<String, String> retrieveOrganizations() {
        String orgsUrl = "http://localhost:8444/organizations/search";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<TempOrganizationDto> foo = rt.getForEntity(orgsUrl, TempOrganizationDto.class);

        TempOrganizationDto tempOrganizationDto = foo.getBody();

        List<Element> elements = tempOrganizationDto.getElements();

        Map<String, String> mapOrganizations = new HashMap<>();
        for (Element element : elements) {
            mapOrganizations.put(element.getName(), element.getLogicalId());
        }

        return mapOrganizations;
    }

    private static Map<String, String> retrievePractitioners() {
        //TODO: Implement
        return new HashMap<String, String>();
    }

    private static Map<String, String> retrievePatients() {
        //TODO: Implement
        return new HashMap<>();
    }


}
