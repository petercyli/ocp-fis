package gov.samhsa.ocp.ocpfis.service;


import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchType;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Type;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatientServiceImpl implements PatientService {

    public static final String CODING_SYSTEM_LANGUAGE = "http://hl7.org/fhir/ValueSet/languages";
    public static final String EXTENSION_URL_LANGUAGE = "http://hl7.org/fhir/us/core/ValueSet/simple-language";
    public static final String CODING_SYSTEM_RACE = "http://hl7.org/fhir/v3/Race";
    public static final String EXTENSION_URL_RACE = "http://hl7.org/fhir/StructureDefinition/us-core-race";
    public static final String CODING_SYSTEM_ETHNICITY = "http://hl7.org/fhir/v3/Ethnicity";
    public static final String EXTENSION_URL_ETHNICITY = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity";
    public static final String CODING_SYSTEM_BIRTHSEX = "http://hl7.org/fhir/v3/AdministrativeGender";
    public static final String EXTENSION_URL_BIRTHSEX = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex";
    public static final String RACE_CODE = "Race";
    public static final String LANGUAGE_CODE = "language";
    public static final String ETHNICITY_CODE = "Ethnicity";
    public static final String GENDER_CODE = "Gender";
    private final IGenericClient fhirClient;
    private final IParser iParser;
    private final ModelMapper modelMapper;
    private final FisProperties fisProperties;
    private final FhirValidator fhirValidator;

    public PatientServiceImpl(IGenericClient fhirClient, IParser iParser, ModelMapper modelMapper, FisProperties fisProperties, FhirValidator fhirValidator) {
        this.fhirClient = fhirClient;
        this.iParser = iParser;
        this.modelMapper = modelMapper;
        this.fisProperties = fisProperties;
        this.fhirValidator = fhirValidator;
    }

    @Override
    public List<PatientDto> getPatients() {
        log.debug("Patients Query to FHIR Server: START");
        Bundle response = fhirClient.search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Query to FHIR Server: END");
        return convertBundleToPatientDtos(response, Boolean.FALSE);
    }

    @Override
    public PageDto<PatientDto> getPatientsByValue(String value, String type, boolean showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPatientsPerPage = size.filter(s -> s > 0 &&
                s <= fisProperties.getLocation().getPagination().getMaxSize()).orElse(fisProperties.getPatient().getPagination().getDefaultSize());

        IQuery PatientSearchQuery = fhirClient.search().forResource(Patient.class);
        if (!showInactive) {
            // show only active patients
            PatientSearchQuery.where(new TokenClientParam("active").exactly().code(Boolean.TRUE.toString()));
        }

        if (type.equalsIgnoreCase(SearchType.NAME.name())) {
            PatientSearchQuery.where(new StringClientParam("name").matches().value(value.trim()));
        } else if (type.equalsIgnoreCase(SearchType.IDENTIFIER.name())) {
            PatientSearchQuery.where(new TokenClientParam("identifier").exactly().code(value.trim()));
        } else {
            throw new BadRequestException("Invalid Type Values");
        }

        Bundle firstPagePatientSearchBundle;
        Bundle otherPagePatientSearchBundle;
        boolean firstPage = true;
        log.debug("Patients Search Query to FHIR Server: START");
        firstPagePatientSearchBundle = (Bundle) PatientSearchQuery.count(numberOfPatientsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Search Query to FHIR Server: END");

        otherPagePatientSearchBundle = firstPagePatientSearchBundle;
        if (page.isPresent() && page.get() > 1 && firstPagePatientSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            firstPage = false;
            otherPagePatientSearchBundle = getSearchBundleAfterFirstPage(firstPagePatientSearchBundle, page.get(), numberOfPatientsPerPage);
        }

        //Arrange Page related info
        List<PatientDto> patientDtos = convertBundleToPatientDtos(otherPagePatientSearchBundle, Boolean.FALSE);
        double totalPages = Math.ceil((double) otherPagePatientSearchBundle.getTotal() / numberOfPatientsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(patientDtos, numberOfPatientsPerPage, totalPages, currentPage, patientDtos.size(), otherPagePatientSearchBundle.getTotal());
    }

    private int getPatientsByIdentifier(String system, String value) {
        log.info("Searching patients with identifier.system : " + system + " and value : " + value);
        IQuery searchQuery = fhirClient.search().forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(system, value));
        Bundle searchBundle = (Bundle) searchQuery.returnBundle(Bundle.class).execute();
        return searchBundle.getTotal();
    }

    @Override
    public void createPatient(PatientDto patientDto) {
        int existingNumberOfPatients = this.getPatientsByIdentifier(patientDto.getIdentifier().get(0).getSystem(), patientDto.getIdentifier().get(0).getValue());

        if(existingNumberOfPatients == 0) {

            final Patient patient = modelMapper.map(patientDto, Patient.class);
            patient.setActive(Boolean.TRUE);
            patient.setGender(getPatientGender(patientDto.getGenderCode()));
            patient.setBirthDate(java.sql.Date.valueOf(patientDto.getBirthDate()));

            setExtensionFields(patient, patientDto);

            final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
            if (validationResult.isSuccessful()) {
                fhirClient.create().resource(patient).execute();
            } else {
                throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
            }
        } else {
            log.info("Patient already exists with the given identifier system and value");
            throw new DuplicateResourceFoundException("Patient already exists with the given identifier system and value");
        }
    }

    @Override
    public void updatePatient(PatientDto patientDto) {
        final Patient patient = modelMapper.map(patientDto, Patient.class);
        patient.setId(new IdType(patientDto.getId()));

        final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        if (validationResult.isSuccessful()) {
                log.debug("Calling FHIR Patient Update");

                fhirClient.update().resource(patient)
                        //.conditional()
                        //.where(Patient.IDENTIFIER.exactly().systemAndCode(getCodeSystemByValue(patientDto.getIdentifier(), patient.getId()), patient.getId()))
                        .execute();
        } else {
            throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
        }
    }

    private List<PatientDto> convertBundleToPatientDtos(Bundle response, boolean isSearch) {
        List<PatientDto> patientDtos = new ArrayList<>();
        if (null == response || response.isEmpty() || response.getEntry().size() < 1) {
            log.info("No patients in FHIR Server");
            // Search throw patient not found exception and list will show empty list
            if (isSearch) throw new PatientNotFoundException();
        } else {
            patientDtos = response.getEntry().stream()
                    .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Patient))  //patient entries
                    .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource()) // patient resources
                    .peek(patient -> log.debug(iParser.encodeResourceToString(patient)))
                    .map(patient -> {
                        PatientDto patientDto = modelMapper.map(patient, PatientDto.class);
                        patientDto.setId(patient.getIdElement().getIdPart());
                        mapExtensionFields(patient, patientDto);
                        return patientDto;
                    })
                    .collect(Collectors.toList());
        }
        log.info("Total Patients retrieved from Server #" + patientDtos.size());
        return patientDtos;
    }

    private Bundle getSearchBundleAfterFirstPage(Bundle resourceSearchBundle, int page, int size) {
        if (resourceSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page >= 1 ? page : 1) - 1) * size;

            if (offset >= resourceSearchBundle.getTotal()) {
                throw new PatientNotFoundException("No Patients were found in the FHIR server for this page number");
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + resourceSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + size
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return resourceSearchBundle;
    }

    private Enumerations.AdministrativeGender getPatientGender(String codeString) {
        switch (codeString.toUpperCase()) {
            case "MALE":
                return Enumerations.AdministrativeGender.MALE;
            case "M":
                return Enumerations.AdministrativeGender.MALE;
            case "FEMALE":
                return Enumerations.AdministrativeGender.FEMALE;
            case "F":
                return Enumerations.AdministrativeGender.FEMALE;
            case "OTHER":
                return Enumerations.AdministrativeGender.OTHER;
            case "O":
                return Enumerations.AdministrativeGender.OTHER;
            case "UNKNOWN":
                return Enumerations.AdministrativeGender.UNKNOWN;
            case "UN":
                return Enumerations.AdministrativeGender.UNKNOWN;
            default:
                return Enumerations.AdministrativeGender.UNKNOWN;

        }
    }

    private void setIdentifiers(Patient patient, PatientDto patientDto) {
        patient.setId(new IdType(patientDto.getId()));
        patientDto.getIdentifier().stream()
                .forEach(identifier -> {
                            final Identifier id = patient.addIdentifier()
                                    .setSystem(identifier.getSystem())
                                    .setValue(identifier.getValue());
                            if (id.getValue().equals(patientDto.getId())) {
                                // if mrn, set use to official
                                id.setUse(Identifier.IdentifierUse.OFFICIAL);
                            }
                        }
                );
    }

    private String getCodeSystemByValue(List<IdentifierDto> identifierList, String value) {
        // TODO: review business logic
        //return identifierList.stream().filter(identifier -> identifier.getValue().equalsIgnoreCase(value)).findFirst().get().getSystem();
        return identifierList.stream().findFirst().get().getSystem();
    }

    private void setExtensionFields(Patient patient, PatientDto patientDto) {
        List<Extension> extensionList = new ArrayList<>();

        //language
        //TODO: Check the codeSystem value
        Coding langCoding = createCoding(CODING_SYSTEM_LANGUAGE, patientDto.getLanguage());
        Extension langExtension = createExtension(EXTENSION_URL_LANGUAGE, new CodeableConcept().addCoding(langCoding));
        extensionList.add(langExtension);

        //race
        Coding raceCoding = createCoding(CODING_SYSTEM_RACE, patientDto.getRace());
        Extension raceExtension = createExtension(EXTENSION_URL_RACE, new CodeableConcept().addCoding(raceCoding));
        extensionList.add(raceExtension);
        //add other extensions to the list

        //ethnicity
        Coding ethnicityCoding = createCoding(CODING_SYSTEM_ETHNICITY, patientDto.getEthnicity());
        Extension ethnicityExtension = createExtension(EXTENSION_URL_ETHNICITY, new CodeableConcept().addCoding(ethnicityCoding));
        extensionList.add(ethnicityExtension);

        //us-core-birthsex
        Coding birthsexCoding = createCoding(CODING_SYSTEM_BIRTHSEX, patientDto.getBirthSex());
        Extension birthsexExtension = createExtension(EXTENSION_URL_BIRTHSEX, new CodeableConcept().addCoding(birthsexCoding));
        extensionList.add(birthsexExtension);

        patient.setExtension(extensionList);
    }

    private Coding createCoding(String codeSystem, String code) {
        Coding coding = new Coding();
        coding.setSystem(codeSystem);
        coding.setCode(code);
        return coding;
    }

    private Extension createExtension(String url, Type t) {
        Extension ext = new Extension();
        ext.setUrl(url);
        ext.setValue(t);
        return ext;
    }

    private void mapExtensionFields(Patient patient, PatientDto patientDto) {
        List<Extension> extensionList = patient.getExtension();

        extensionList.stream().map(extension -> convertExtensionToCoding(extension)).forEach(obj -> {
            if(obj.isPresent()) {
                Coding coding = obj.get();
                if(coding.getSystem().contains(RACE_CODE)) {
                    patientDto.setRace(coding.getCode());
                } else if (coding.getSystem().contains(LANGUAGE_CODE)) {
                    patientDto.setLanguage(coding.getCode());
                } else if (coding.getSystem().contains(ETHNICITY_CODE)) {
                    patientDto.setEthnicity(coding.getCode());
                } else if (coding.getSystem().contains(GENDER_CODE)) {
                    patientDto.setBirthSex(coding.getCode());
                }
            }
        });
    }

    private Optional<Coding> convertExtensionToCoding(Extension extension) {
        Type type = extension.getValue();
        CodeableConcept codeableConcept = (CodeableConcept) type;
        List<Coding> codingList = codeableConcept.getCoding();
        return Optional.ofNullable(codingList.get(0));
    }
}


