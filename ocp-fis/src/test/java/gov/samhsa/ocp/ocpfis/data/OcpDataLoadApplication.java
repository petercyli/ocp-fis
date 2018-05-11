package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.organization.Element;
import gov.samhsa.ocp.ocpfis.data.model.organization.TempOrganizationDto;
import gov.samhsa.ocp.ocpfis.data.model.patientlist.WrapperPatientDto;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.Code;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.Name;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.PractitionerRole;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.WrapperPractitionerDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OcpDataLoadApplication {

    private static final String XSLX_FILE = "C://data//OCP.xlsx";

    public static void main(String[] args) throws IOException, InvalidFormatException {

        //for intercepting the requests and debugging
        setFiddler();

        //ValueSets
        ValueSetHelper.process();

        //Create a workbook form excel file
        Workbook workbook = WorkbookFactory.create(new File(XSLX_FILE));

        //Check number of sheets
        log.info("Number of sheets : " + workbook.getNumberOfSheets());

        //Organizations
        Sheet organizations = workbook.getSheet("Organizations");
        OrganizationHelper.process(organizations);


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
        PatientsHelper.process(patients, mapOfPractitioners);
        log.info("Populated patients");

        Map<String, String> mapOfPatients = retrievePatients();

        Sheet relationPersons = workbook.getSheet("Patient Related Persons");
        RelatedPersonsHelper.process(relationPersons, mapOfPatients);
        log.info("Populated relationPersons");

        Sheet careTeams = workbook.getSheet("Patient Care Teams");
        CareTeamsHelper.process(careTeams, mapOfPractitioners, mapOfPatients);
        log.info("Populated careTeams");

        Sheet taskOwners = workbook.getSheet("Tasks");
        TasksHelper.process(taskOwners, mapOfPatients, mapOfPractitioners, mapOrganizations);
        log.info("Populated taskOwners");

        Sheet todos = workbook.getSheet("To Do");
        TodosHelper.process(todos, mapOfPatients, mapOrganizations);
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
        String url = "http://localhost:8444/practitioners/search?showAll=true";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<WrapperPractitionerDto> practitioners = rt.getForEntity(url, WrapperPractitionerDto.class);

        WrapperPractitionerDto wrapperDto = practitioners.getBody();

        List<gov.samhsa.ocp.ocpfis.data.model.practitioner.Element> dtos = wrapperDto.getElements();

        log.info("Number of Practitioners retrieved : " + dtos.size());

        Map<String, String> practitionersMap = new HashMap<>();
        for (gov.samhsa.ocp.ocpfis.data.model.practitioner.Element practitionerDto : dtos) {

            Name name = new Name();
            if (practitionerDto.getName().stream().findFirst().isPresent()) {
                name = practitionerDto.getName().stream().findFirst().get();
            } else {
                name.setFirstName("Unknown");
                name.setLastName("Unknown");
            }

            List<PractitionerRole> practitionRoles = practitionerDto.getPractitionerRoles();

            PractitionerRole practitionerRole = new PractitionerRole();
            if (practitionRoles.stream().findFirst().isPresent()) {
                practitionerRole = practitionRoles.stream().findFirst().get();
            } else {
                Code code = new Code();
                code.setCode("214"); //Org Admin
                code.setDisplay("Organization Administrator");
                practitionerRole.setLogicalId("214");
                practitionerRole.setCode(Arrays.asList(code));

            }

            log.info("practitionerRole : " + practitionerRole.getLogicalId() + " Value : " + practitionerRole.getCode().stream().findFirst().get().getDisplay());

            practitionersMap.put(name.getLastName().trim(), practitionerDto.getLogicalId());
        }

        return practitionersMap;
    }

    private static Map<String, String> retrievePatients() {
        String url = "http://localhost:8444/patients/search?showAll=true";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<WrapperPatientDto> responseEntity = rt.getForEntity(url, WrapperPatientDto.class);
        WrapperPatientDto wrapperDto = responseEntity.getBody();

        List<gov.samhsa.ocp.ocpfis.data.model.patientlist.Element> dtos = wrapperDto.getElements();
        Map<String, String> patientsMap = new HashMap<>();

        for (gov.samhsa.ocp.ocpfis.data.model.patientlist.Element patientDto : dtos) {
            gov.samhsa.ocp.ocpfis.data.model.patientlist.Name name = new gov.samhsa.ocp.ocpfis.data.model.patientlist.Name();
            if (patientDto.getName().stream().findFirst().isPresent()) {
                name = patientDto.getName().stream().findFirst().get();
            } else {
                name.setFirstName("Unknown");
                name.setLastName("Unknown");
            }

            patientsMap.put(name.getFirstName().trim() + " " + name.getLastName(), patientDto.getId());
        }

        return patientsMap;
    }

    private static void setFiddler() {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyPort", "8888");
    }


}
