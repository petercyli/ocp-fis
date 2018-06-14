package gov.samhsa.ocp.ocpfis.data;

import gov.samhsa.ocp.ocpfis.data.model.organization.Element;
import gov.samhsa.ocp.ocpfis.data.model.organization.TempOrganizationDto;
import gov.samhsa.ocp.ocpfis.data.model.patientlist.WrapperPatientDto;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.Code;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.Name;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.PractitionerRole;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.WrapperPractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;

@Slf4j
public class OcpDataLoadApplication {

    private static String XSLX_FILE;

    public static void main(String[] args) throws IOException, InvalidFormatException {
        String[] values = readPropertiesFile();

        if(values.length == 3) {
            populateFhirResources(values[0], values[1]);

            populateUAA(values[0], values[2]);
        } else {
            log.error("Incorrect number of keys in the properties file");
        }
    }

    private static String[] readPropertiesFile() {
        Properties prop = new Properties();
        String[] values = new String[3];
        try (InputStream input = new FileInputStream("data.properties")) {
            prop.load(input);

            values[0] = prop.getProperty("xlsxfile");
            values[1] = prop.getProperty("valuesetsdir");
            values[2] = prop.getProperty("scriptsdir");

        } catch (IOException e) {
            log.error("Please provide a file data.properties at the root directory");
        }
        return values;

    }

    private static void populateUAA(final String XSLX_FILE, final String scriptsDir) throws IOException, InvalidFormatException {
        //1. Create roles and scopes
        RolesUAAHelper.createRoles();
        log.info("Finished creating roles and scopes in UAA");

        //2. Create ocpAdmin
        OCPAdminUAAHelper.createOCPAdmin();

        Workbook workbook = WorkbookFactory.create(new File(XSLX_FILE));

        //Get all organizations
        Map<String, String> organizationsMap = retrieveOrganizations();
        log.info("Retrieved organizations");

        //3. Populate Practitioners
        Map<String, String> practitionersMap = retrievePractitioners();
        List<PractitionerDto> practitionersSheet = PractitionersHelper.retrieveSheet(workbook.getSheet("Practitioners"), organizationsMap);
        log.info("Retrieved practitioners");

        PractitionerUAAHelper.createPractitioners(practitionersMap, practitionersSheet);
        log.info("Finished creating practitioners in UAA");

        //4. Populate Patients
        Map<String, String> patientsMap = retrievePatients();
        log.info("Retrieved patients");

        List<PatientDto> patientsSheet = PatientsHelper.retrieveSheet(workbook.getSheet("Patient"), practitionersMap, organizationsMap);

        PatientUAAHelper.createPatients(patientsMap, organizationsMap, patientsSheet);
        log.info("Finished creating patients in UAA");
    }

    private static void populateFhirResources(final String XSLX_FILE, final String valueSetsDir) throws IOException, InvalidFormatException {

        //for intercepting the requests and debugging
        setFiddler();

        //ValueSets
        ValueSetHelper.process(valueSetsDir);

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
        //Do not enable
        ActivityDefinitionsHelper.process(activityDefinitions, mapOrganizations);
        log.info("Populated practitioners");

        Sheet practitioners = workbook.getSheet("Practitioners");
        PractitionersHelper.process(practitioners, mapOrganizations);
        log.info("Populated practitioners");

        Map<String, String> mapOfPractitioners = retrievePractitioners();

        Sheet patients = workbook.getSheet("Patient");
        PatientsHelper.process(patients, mapOfPractitioners, mapOrganizations);
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
        CommunicationsHelper.process(communications, mapOfPatients, mapOfPractitioners);
        log.info("Populated communications");

        Sheet appointments = workbook.getSheet("Appointments");
        AppointmentsHelper.process(appointments, mapOfPatients, mapOfPractitioners);
        log.info("Populated appointments");

        workbook.close();
        log.info("Workbook closed");

    }

    private static Map<String, String> retrieveOrganizations() {
        String orgsUrl = DataConstants.serverUrl + "organizations/search";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<TempOrganizationDto> foo = rt.getForEntity(orgsUrl, TempOrganizationDto.class);

        TempOrganizationDto tempOrganizationDto = foo.getBody();

        List<Element> elements = tempOrganizationDto.getElements();

        Map<String, String> mapOrganizations = new HashMap<>();
        for (Element element : elements) {
            mapOrganizations.put(element.getName().trim(), element.getLogicalId());
        }

        return mapOrganizations;
    }

    private static Map<String, String> retrievePractitioners() {
        String url = DataConstants.serverUrl + "practitioners/search?showAll=true";
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

            practitionersMap.put(name.getFirstName().trim() + " " + name.getLastName().trim(), practitionerDto.getLogicalId());
        }

        return practitionersMap;
    }

    private static Map<String, String> retrievePatients() {
        String url = DataConstants.serverUrl + "patients/search?showAll=true";
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

            patientsMap.put(name.getFirstName().trim() + " " + name.getLastName().trim(), patientDto.getId());
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
