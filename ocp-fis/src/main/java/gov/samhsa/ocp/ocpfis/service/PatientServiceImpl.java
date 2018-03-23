package gov.samhsa.ocp.ocpfis.service;


import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.FlagDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Flag;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static gov.samhsa.ocp.ocpfis.util.FhirUtil.createExtension;
import static gov.samhsa.ocp.ocpfis.util.FhirUtil.getCoding;
import static java.util.stream.Collectors.toList;

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
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;
    private final LookUpService lookUpService;

    public PatientServiceImpl(IGenericClient fhirClient, IParser iParser, ModelMapper modelMapper, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService) {
        this.fhirClient = fhirClient;
        this.iParser = iParser;
        this.modelMapper = modelMapper;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService = lookUpService;
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
    public PageDto<PatientDto> getPatientsByValue(String value, String type, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPatientsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Patient.name());

        IQuery PatientSearchQuery = fhirClient.search().forResource(Patient.class);

        if (showInactive.isPresent()) {
            if (!showInactive.get()) {
                // show only active patients
                PatientSearchQuery.where(new TokenClientParam("active").exactly().code(Boolean.TRUE.toString()));
            }
        }

        if (type.equalsIgnoreCase(SearchKeyEnum.CommonSearchKey.NAME.name())) {
            PatientSearchQuery.where(new RichStringClientParam("name").contains().value(value.trim()));
        } else if (type.equalsIgnoreCase(SearchKeyEnum.CommonSearchKey.IDENTIFIER.name())) {
            PatientSearchQuery.where(new TokenClientParam("identifier").exactly().code(value.trim()));
        } else {
            throw new BadRequestException("Invalid Type Values");
        }

        Bundle firstPagePatientSearchBundle;
        Bundle otherPagePatientSearchBundle;
        boolean firstPage = true;
        log.debug("Patients Search Query to FHIR Server: START");
        firstPagePatientSearchBundle = (Bundle) PatientSearchQuery
                .count(numberOfPatientsPerPage)
                .revInclude(Flag.INCLUDE_PATIENT)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Search Query to FHIR Server: END");

        if (firstPagePatientSearchBundle == null || firstPagePatientSearchBundle.getEntry().isEmpty()) {
            log.info("No patients were found for the given criteria.");
            return new PageDto<>(new ArrayList<>(), numberOfPatientsPerPage, 0, 0, 0, 0);
        }

        otherPagePatientSearchBundle = firstPagePatientSearchBundle;
        if (page.isPresent() && page.get() > 1 && firstPagePatientSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            firstPage = false;
            otherPagePatientSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPagePatientSearchBundle, page.get(), numberOfPatientsPerPage);
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

        if (existingNumberOfPatients == 0) {

            final Patient patient = modelMapper.map(patientDto, Patient.class);
            patient.setActive(Boolean.TRUE);
            patient.setGender(FhirUtil.getPatientGender(patientDto.getGenderCode()));
            patient.setBirthDate(java.sql.Date.valueOf(patientDto.getBirthDate()));

            setExtensionFields(patient, patientDto);

            final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
            if (validationResult.isSuccessful()) {
                MethodOutcome methodOutcome = fhirClient.create().resource(patient).execute();
                //Assign fhir Patient resource id.
                Reference patientId = new Reference();
                patientId.setReference("Patient/" + methodOutcome.getId().getIdPart());

                //Create flag for the patient
                patientDto.getFlags().forEach(flagDto -> {
                    Flag flag = convertFlagDtoToFlag(patientId, flagDto);
                    fhirClient.create().resource(flag).execute();
                });
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
        patient.setGender(FhirUtil.getPatientGender(patientDto.getGenderCode()));
        patient.setBirthDate(java.sql.Date.valueOf(patientDto.getBirthDate()));

        setExtensionFields(patient, patientDto);

        final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        if (validationResult.isSuccessful()) {
            MethodOutcome methodOutcome = fhirClient.update().resource(patient).execute();
            //Assign fhir Patient resource id.
            Reference patientId = new Reference();
            patientId.setReference("Patient/" + methodOutcome.getId().getIdPart());

            patientDto.getFlags().forEach(flagDto -> {
                if (!duplicateCheckForFlag(flagDto, patientDto.getId())) {
                    Flag flag = convertFlagDtoToFlag(patientId, flagDto);
                    if (flagDto.getLogicalId() != null) {
                        flag.setId(flagDto.getLogicalId());
                        fhirClient.update().resource(flag).execute();
                    } else {
                        fhirClient.create().resource(flag).execute();
                    }
                } else {
                    throw new DuplicateResourceFoundException("Same flag is already present for this patient.");
                }
            });

        } else {
            throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
        }
    }

    @Override
    public PatientDto getPatientById(String patientId) {
        Bundle patientBundle = fhirClient.search().forResource(Patient.class)
                .where(new TokenClientParam("_id").exactly().code(patientId))
                .revInclude(Flag.INCLUDE_PATIENT)
                .returnBundle(Bundle.class)
                .execute();

        if (patientBundle == null || patientBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No patient was found for the given patientID : " + patientId);
        }

        Bundle.BundleEntryComponent patientBundleEntry = patientBundle.getEntry().get(0);
        Patient patient = (Patient) patientBundleEntry.getResource();
        PatientDto patientDto = modelMapper.map(patient, PatientDto.class);
        patientDto.setId(patient.getIdElement().getIdPart());
        patientDto.setBirthDate(patient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        patientDto.setGenderCode(patient.getGender().toCode());

        //Get Flags for the patient
        List<FlagDto> flagDtos = getFlagsForEachPatient(patientBundle.getEntry(), patientBundleEntry.getResource().getIdElement().getIdPart());
        patientDto.setFlags(flagDtos);

        mapExtensionFields(patient, patientDto);

        return patientDto;
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
                        if (patient.getGender() != null)
                            patientDto.setGenderCode(patient.getGender().toCode());
                        mapExtensionFields(patient, patientDto);
                        //Getting flags into the patient dto
                        List<FlagDto> flagDtos = getFlagsForEachPatient(response.getEntry(), patient.getIdElement().getIdPart());
                        patientDto.setFlags(flagDtos);
                        return patientDto;
                    })
                    .collect(toList());
        }
        log.info("Total Patients retrieved from Server #" + patientDtos.size());
        return patientDtos;
    }

    private List<FlagDto> getFlagsForEachPatient(List<Bundle.BundleEntryComponent> patientAndAllReferenceBundle, String patientId) {
        return patientAndAllReferenceBundle.stream().filter(patientWithAllReference -> patientWithAllReference.getResource().getResourceType().equals(ResourceType.Flag))
                .map(flagBundle -> {
                    Flag flag = (Flag) flagBundle.getResource();
                    return flag;
                })
                .filter(flag -> flag.getSubject().getReference().equalsIgnoreCase("Patient/" + patientId))
                .map(flag -> {
                    FlagDto flagDto = modelMapper.map(flag, FlagDto.class);
                    if (flag.getPeriod() != null && !flag.getPeriod().isEmpty()) {
                        PeriodDto periodDto = new PeriodDto();
                        flagDto.setPeriod(periodDto);
                        flagDto.getPeriod().setStart((flag.getPeriod().hasStart()) ? DateUtil.convertDateToLocalDate(flag.getPeriod().getStart()) : null);
                        flagDto.getPeriod().setEnd((flag.getPeriod().hasEnd()) ? DateUtil.convertDateToLocalDate(flag.getPeriod().getEnd()) : null);
                    }

                    ValueSetDto statusOfFlag = FhirDtoUtil.convertCodeToValueSetDto(flag.getStatus().toCode(), lookUpService.getFlagStatus());
                    flagDto.setStatus(statusOfFlag.getCode());
                    flagDto.setStatusDisplay(statusOfFlag.getDisplay());
                    flag.getCategory().getCoding().stream().findAny().ifPresent(coding -> {
                        flagDto.setCategory(coding.getCode());
                        flagDto.setCategoryDisplay(coding.getDisplay());
                    });

                    flagDto.setCode(flag.getCode().getText());
                    flagDto.setSubject(flag.getSubject().getReference());
                    flagDto.setLogicalId(flag.getIdElement().getIdPart());
                    return flagDto;
                }).collect(toList());
    }

    private void setExtensionFields(Patient patient, PatientDto patientDto) {
        List<Extension> extensionList = new ArrayList<>();

        //language
        if (patientDto.getLanguage() != null && !patientDto.getLanguage().isEmpty()) {
            Coding langCoding = getCoding(patientDto.getLanguage(), "", CODING_SYSTEM_LANGUAGE);
            Extension langExtension = createExtension(EXTENSION_URL_LANGUAGE, new CodeableConcept().addCoding(langCoding));
            extensionList.add(langExtension);
        }

        //race
        if (patientDto.getRace() != null && !patientDto.getRace().isEmpty()) {
            Coding raceCoding = getCoding(patientDto.getRace(), "", CODING_SYSTEM_RACE);
            Extension raceExtension = createExtension(EXTENSION_URL_RACE, new CodeableConcept().addCoding(raceCoding));
            extensionList.add(raceExtension);
        }

        //ethnicity
        if (patientDto.getEthnicity() != null && !patientDto.getEthnicity().isEmpty()) {
            Coding ethnicityCoding = getCoding(patientDto.getEthnicity(), "", CODING_SYSTEM_ETHNICITY);
            Extension ethnicityExtension = createExtension(EXTENSION_URL_ETHNICITY, new CodeableConcept().addCoding(ethnicityCoding));
            extensionList.add(ethnicityExtension);
        }

        //us-core-birthsex
        if (patientDto.getBirthSex() != null && !patientDto.getBirthSex().isEmpty()) {
            Coding birthSexCoding = getCoding(patientDto.getBirthSex(), "", CODING_SYSTEM_BIRTHSEX);
            Extension birthSexExtension = createExtension(EXTENSION_URL_BIRTHSEX, new CodeableConcept().addCoding(birthSexCoding));
            extensionList.add(birthSexExtension);
        }

        patient.setExtension(extensionList);
    }


    private void mapExtensionFields(Patient patient, PatientDto patientDto) {
        List<Extension> extensionList = patient.getExtension();

        extensionList.stream().map(FhirUtil::convertExtensionToCoding).filter(Optional::isPresent).map(Optional::get).forEach(coding -> {
            if (coding.getSystem().contains(RACE_CODE)) {
                patientDto.setRace(coding.getCode());
            } else if (coding.getSystem().contains(LANGUAGE_CODE)) {
                patientDto.setLanguage(coding.getCode());
            } else if (coding.getSystem().contains(ETHNICITY_CODE)) {
                patientDto.setEthnicity(coding.getCode());
            } else if (coding.getSystem().contains(GENDER_CODE)) {
                patientDto.setBirthSex(coding.getCode());
            }
        });
    }

    private Flag convertFlagDtoToFlag(Reference patientId, FlagDto flagDto) {
        Flag flag = new Flag();
        //Set Subject
        flag.setSubject(patientId);
        //Set code
        flag.getCode().setText(flagDto.getCode());

        //Set Status
        try {
            flag.setStatus(Flag.FlagStatus.fromCode(flagDto.getStatus()));
        } catch (FHIRException e) {
            throw new BadRequestException("No such fhir status exist.");
        }

        //Set Category
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(modelMapper.map(FhirDtoUtil.convertCodeToValueSetDto(flagDto.getCategory(), lookUpService.getFlagCategory()), Coding.class));
        flag.setCategory(codeableConcept);

        //Set Period
        Period period = new Period();
        period.setStart(java.sql.Date.valueOf(flagDto.getPeriod().getStart()));
        period.setEnd((flagDto.getPeriod().getEnd()!=null)? java.sql.Date.valueOf(flagDto.getPeriod().getEnd()):null);
        flag.setPeriod(period);

        //Set Author
        Reference reference = modelMapper.map(flagDto.getAuthor(), Reference.class);
        flag.setAuthor(reference);

        return flag;
    }

    private boolean duplicateCheckForFlag(FlagDto flagDto, String patientId) {
        IQuery flagBundleForPatientQuery = fhirClient.search().forResource(Flag.class)
                .where(new ReferenceClientParam("subject").hasId(patientId));
        Bundle flagBundleToCoundTotalNumberOfFlag = (Bundle) flagBundleForPatientQuery.returnBundle(Bundle.class).execute();
        int totalFlagForPatient = flagBundleToCoundTotalNumberOfFlag.getTotal();
        Bundle flagBundleForPatient = (Bundle) flagBundleForPatientQuery
                .count(totalFlagForPatient)
                .returnBundle(Bundle.class)
                .execute();
        return flagHasSameCodeAndCategory(flagBundleForPatient, flagDto);
    }

    private boolean flagHasSameCodeAndCategory(Bundle bundle, FlagDto flagDto) {
        List<Flag> duplicateCheckList = new ArrayList<>();
        if (!bundle.isEmpty()) {
            duplicateCheckList = bundle.getEntry().stream()
                    .map(flagResource -> {
                        Flag flag = (Flag) flagResource.getResource();
                        return flag;
                    })
                    .filter(flag -> flag.getCode().getText().equalsIgnoreCase(flagDto.getCode()))
                    .filter(flag -> flag.getCategory().getCoding().get(0).getCode().equalsIgnoreCase(flagDto.getCategory())
                    ).collect(toList());
        }
        //Checking while updating flag
        if (flagDto.getLogicalId() != null) {
            if (duplicateCheckList.isEmpty()) {
                return false;
            } else {
                List<Flag> flags = duplicateCheckList.stream()
                        .filter(flag -> flagDto.getLogicalId().equalsIgnoreCase(flag.getIdElement().getIdPart())
                        ).collect(toList());
                return flags.isEmpty();
            }
        } else {
            //Checking while creating new flag
            return !duplicateCheckList.isEmpty();
        }
    }


}


