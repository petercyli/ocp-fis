package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.domain.StructureDefinitionEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.FlagDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.CoverageToCoverageDtoMap;
import gov.samhsa.ocp.ocpfis.service.mapping.EpisodeOfCareToEpisodeOfCareDtoMapper;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.FhirResourceUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Flag;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ca.uhn.fhir.rest.api.Constants.PARAM_LASTUPDATED;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class PatientServiceImpl implements PatientService {

    public static final String TO_DO = "To-Do";
    public static final int EPISODE_OF_CARE_END_PERIOD = 1;

    private final IGenericClient fhirClient;
    private final IParser iParser;
    private final ModelMapper modelMapper;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;
    private final LookUpService lookUpService;
    private final CoverageServiceImpl coverageService;

    @Autowired
    public PatientServiceImpl(IGenericClient fhirClient, IParser iParser, ModelMapper modelMapper, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService, CoverageServiceImpl coverageService) {
        this.fhirClient = fhirClient;
        this.iParser = iParser;
        this.modelMapper = modelMapper;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService = lookUpService;
        this.coverageService = coverageService;
    }

    @Override
    public List<PatientDto> getPatients() {
        log.debug("Patients Query to FHIR Server: START");
        Bundle response = fhirClient.search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .sort().descending(PARAM_LASTUPDATED)
                .encodedJson()
                .execute();
        log.debug("Patients Query to FHIR Server: END");
        return convertBundleToPatientDtos(response, Boolean.FALSE);
    }

    @Override
    public PageDto<PatientDto> getPatientsByValue(Optional<String> searchKey, Optional<String> value, Optional<String> organization, Optional<Boolean> assigned, Optional<String> careTeamPractitioner, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {
        int numberOfPatientsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Patient.name());

        IQuery PatientSearchQuery = fhirClient.search().forResource(Patient.class).sort().descending(PARAM_LASTUPDATED);

        if (showInactive.isPresent()) {
            if (!showInactive.get()) {
                // show only active patients
                PatientSearchQuery.where(new TokenClientParam("active").exactly().code(Boolean.TRUE.toString()));
            }
        }

        if (organization.isPresent() && careTeamPractitioner.isPresent()) {
            if (!patientsAssociatedWithPractitioner(organization.get(), careTeamPractitioner.get()).isEmpty()) {
                PatientSearchQuery.where(new TokenClientParam("_id").exactly().codes(patientsAssociatedWithPractitioner(organization.get(), careTeamPractitioner.get())));
            } else {
                log.info("No Patients were found for given organization.");
                return new PageDto<>(new ArrayList<>(), numberOfPatientsPerPage, 0, 0, 0, 0);
            }
        } else if (organization.isPresent()) {
            if (!patientsInOrganization(organization.get()).isEmpty()) {
                PatientSearchQuery.where(new TokenClientParam("_id").exactly().codes(patientsInOrganization(organization.get())));
            } else {
                log.info("No Patients were found for given organization.");
                return new PageDto<>(new ArrayList<>(), numberOfPatientsPerPage, 0, 0, 0, 0);
            }
        }


        searchKey.ifPresent(key -> {
            if (key.equalsIgnoreCase(SearchKeyEnum.CommonSearchKey.NAME.name())) {
                value.ifPresent(s -> PatientSearchQuery.where(new RichStringClientParam("name").contains().value(s.trim())));
            } else if (key.equalsIgnoreCase(SearchKeyEnum.CommonSearchKey.IDENTIFIER.name())) {
                value.ifPresent(s -> PatientSearchQuery.where(new TokenClientParam("identifier").exactly().code(s.trim())));
            } else {
                throw new BadRequestException("Invalid Type Values");
            }
        });

        Bundle firstPagePatientSearchBundle;
        Bundle otherPagePatientSearchBundle;
        boolean firstPage = true;
        log.debug("Patients Search Query to FHIR Server: START");
        firstPagePatientSearchBundle = (Bundle) PatientSearchQuery
                .count(numberOfPatientsPerPage)
                .revInclude(Flag.INCLUDE_PATIENT)
                .revInclude(EpisodeOfCare.INCLUDE_PATIENT)
                .revInclude(Coverage.INCLUDE_BENEFICIARY)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Search Query to FHIR Server: END");

        List<PatientDto> patientDtos = convertAllBundleToSinglePatientDtoList(firstPagePatientSearchBundle, numberOfPatientsPerPage);

        if (assigned.isPresent()) {
            if (assigned.get()) {
                patientDtos = patientDtos.stream().filter(pdto -> {
                    Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                            .where(new ReferenceClientParam("subject").hasId(pdto.getId())).returnBundle(Bundle.class).execute();
                    return !careTeamBundle.getEntry().stream().map(ct -> (CareTeam) ct.getResource()).flatMap(ct -> ct.getParticipant().stream()
                            .map(par -> par.getRole().getCoding().stream().findFirst().get().getCode()))
                            .filter(r -> lookUpService.getParticipantRoles().stream().map(role -> role.getCode().trim()).collect(toList()).contains(r.trim()))
                            .collect(toList())
                            .isEmpty();
                }).collect(toList());
            }
        }

        if (showAll.isPresent() && showAll.get()) {
            return (PageDto<PatientDto>) PaginationUtil.applyPaginationForCustomArrayList(patientDtos, patientDtos.size(), Optional.of(1), false);
        }

        return (PageDto<PatientDto>) PaginationUtil.applyPaginationForCustomArrayList(patientDtos, numberOfPatientsPerPage, page, false);
    }


    @Override
    public PageDto<PatientDto> getPatientsByPractitioner(Optional<String> practitioner, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfPatientsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Patient.name());
        List<PatientDto> patients = this.getPatientsByPractitioner(practitioner, searchKey, searchValue);

        return (PageDto<PatientDto>) PaginationUtil.applyPaginationForCustomArrayList(patients, numberOfPatientsPerPage, pageNumber, false);
    }

    @Override
    public List<PatientDto> getPatientsByPractitioner(Optional<String> practitioner, Optional<String> searchKey, Optional<String> searchValue) {
        List<PatientDto> patients = new ArrayList<>();

        IQuery practitionerQuery = fhirClient.search().forResource(CareTeam.class);

        practitioner.ifPresent(practitionerId -> practitionerQuery.where(new ReferenceClientParam("participant").hasId(practitionerId)));

        Bundle bundle = (Bundle) practitionerQuery
                .include(CareTeam.INCLUDE_PATIENT)
                .sort().ascending(CareTeam.RES_ID)
                .returnBundle(Bundle.class)
                .execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> components = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);
            if (components != null && !components.isEmpty()) {
                patients = components.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.Patient))
                        .map(it -> (Patient) it.getResource())
                        .filter(it -> filterBySearchKey(it, searchKey, searchValue))
                        .map(patient -> mapPatientToPatientDto(patient, bundle.getEntry()))
                        .distinct()
                        .collect(toList());
            }
        }
        return patients;
    }

    private boolean filterBySearchKey(Patient patient, Optional<String> searchKey, Optional<String> searchValue) {
        //returning everything if searchKey is not present, change to false later.
        if (searchKey.isPresent() && searchValue.isPresent()) {
            if (searchKey.get().equalsIgnoreCase(SearchKeyEnum.CommonSearchKey.NAME.name())) {
                return FhirResourceUtil.checkPatientName(patient, searchValue.get());
            } else if (searchKey.get().equalsIgnoreCase(SearchKeyEnum.CommonSearchKey.IDENTIFIER.name())) {
                return FhirResourceUtil.checkPatientId(patient, searchValue.get());
            }
        }
        return true;
    }

    private PatientDto mapPatientToPatientDto(Patient patient, List<Bundle.BundleEntryComponent> response) {
        PatientDto patientDto = modelMapper.map(patient, PatientDto.class);
        patientDto.setId(patient.getIdElement().getIdPart());
        patientDto.setMrn(patientDto.getIdentifier().stream().filter(iden -> iden.getSystem().equalsIgnoreCase(fisProperties.getPatient().getMrn().getCodeSystem())).findFirst().map(IdentifierDto::getValue));
        patientDto.setIdentifier(patientDto.getIdentifier().stream().filter(iden -> !iden.getSystem().equalsIgnoreCase(fisProperties.getPatient().getMrn().getCodeSystem())).collect(toList()));
        Bundle bundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(Task.class).where(new ReferenceClientParam("patient").hasId(patient.getIdElement().getIdPart())))
                .returnBundle(Bundle.class).encodedJson().execute();
        List<String> types = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties).stream().map(at -> {
            Task task = (Task) at.getResource();
            try {
                return task.getDefinitionReference().getDisplay();
            } catch (FHIRException e) {
                return "";
            }
        }).distinct().collect(toList());

        if (types.isEmpty()) {
            patientDto.setActivityTypes(Optional.empty());
        } else {
            patientDto.setActivityTypes(Optional.of(types));
        }

        if (patient.getGender() != null)
            patientDto.setGenderCode(patient.getGender().toCode());
        mapExtensionFields(patient, patientDto);
        //Getting flags into the patient dto
        List<FlagDto> flagDtos = getFlagsForEachPatient(response, patient.getIdElement().getIdPart());
        patientDto.setFlags(Optional.ofNullable(flagDtos));

        List<CoverageDto> coverageDtos = getConveragesForEachPatient(response, patient.getIdElement().getIdPart());
        patientDto.setCoverages(Optional.ofNullable(coverageDtos));

        List<EpisodeOfCareDto> episodeOfCareDtos = getEocsForEachPatient(response, patient.getIdElement().getIdPart());
        patientDto.setEpisodeOfCares(episodeOfCareDtos);
        return patientDto;
    }

    @Override
    public void createPatient(PatientDto patientDto) {

        if (!checkDuplicatePatientOfSameOrganization(patientDto)) {
            if (checkDuplicateInFhir(patientDto)) {
                patientDto.getIdentifier().add(setUniqueIdentifierForPatient(patientsWithMatchedDuplicateCheckParameters(patientDto).stream()
                        .map(pat -> (Patient) pat.getResource()).findAny().get().getIdentifier().stream()
                        .filter(iden -> iden.getSystem().equalsIgnoreCase(fisProperties.getPatient().getMrn().getCodeSystem()))
                        .map(Identifier::getValue).findAny().orElse(generateRandomMrn())));
            } else {
                patientDto.getIdentifier().add(setUniqueIdentifierForPatient(generateRandomMrn()));
            }

            final Patient patient = modelMapper.map(patientDto, Patient.class);
            patient.setManagingOrganization(FhirDtoUtil.mapReferenceDtoToReference(orgReference(patientDto.getOrganizationId())));
            patient.setActive(Boolean.TRUE);
            patient.setGender(FhirResourceUtil.getPatientGender(patientDto.getGenderCode()));
            patient.setBirthDate(java.sql.Date.valueOf(patientDto.getBirthDate()));

            setExtensionFields(patient, patientDto);

            //Set Profile Meta Data
            FhirProfileUtil.setPatientProfileMetaData(fhirClient, patient);
            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, patient, Optional.empty(), ResourceType.Patient.name(), "Create Patient");
            //Create
            MethodOutcome methodOutcome = FhirOperationUtil.createFhirResource(fhirClient, patient, ResourceType.Patient.name());

            //Assign fhir Patient resource id.
            Reference patientId = new Reference();
            patientId.setReference("Patient/" + methodOutcome.getId().getIdPart());

            //Create flag for the patient
            patientDto.getFlags().ifPresent(flags -> flags.forEach(flagDto -> {
                Flag flag = convertFlagDtoToFlag(patientId, flagDto);
                //Set Profile Meta Data
                FhirProfileUtil.setFlagProfileMetaData(fhirClient, flag);
                //Validate
                FhirOperationUtil.validateFhirResource(fhirValidator, flag, Optional.empty(), ResourceType.Flag.name(), "Create Flag(When creating Patient)");
                //Create
                FhirOperationUtil.createFhirResource(fhirClient, flag, ResourceType.Flag.name());
            }));

            //Create To-Do task
            Task task = FhirResourceUtil.createToDoTask(methodOutcome.getId().getIdPart(), patientDto.getPractitionerId().orElse(fisProperties.getDefaultPractitioner()), patientDto.getOrganizationId().orElse(fisProperties.getDefaultOrganization()), fhirClient, fisProperties);
            task.setDefinition(FhirDtoUtil.mapReferenceDtoToReference(FhirResourceUtil.getRelatedActivityDefinition(patientDto.getOrganizationId().orElse(fisProperties.getDefaultOrganization()), TO_DO, fhirClient, fisProperties)));
            //Set Profile Meta Data
            FhirProfileUtil.setTaskProfileMetaData(fhirClient, task);
            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, task, Optional.empty(), ResourceType.Task.name(), "Create Task(When creating Patient)");
            //Create
            FhirOperationUtil.createFhirResource(fhirClient, task, ResourceType.Task.name());

            //Create EpisodeOfCare
            if (patientDto.getEpisodeOfCares() != null && !patientDto.getEpisodeOfCares().isEmpty()) {
                patientDto.getEpisodeOfCares().forEach(eoc -> {
                    ReferenceDto patientReference = new ReferenceDto();
                    patientReference.setReference(ResourceType.Patient + "/" + methodOutcome.getId().getIdPart());
                    patientDto.getName().stream().findAny().ifPresent(name -> patientReference.setDisplay(name.getFirstName() + " " + name.getLastName()));
                    eoc.setPatient(patientReference);
                    eoc.setManagingOrganization(orgReference(patientDto.getOrganizationId()));
                    if (eoc.getCareManager() == null) {
                        eoc.setCareManager(pracReference(patientDto.getPractitionerId()));
                    }
                    EpisodeOfCare episodeOfCare = convertEpisodeOfCareDtoToEpisodeOfCare(eoc);
                    //Set Profile Meta Data
                    FhirProfileUtil.setEpisodeOfCareProfileMetaData(fhirClient, episodeOfCare);
                    //Validate
                    FhirOperationUtil.validateFhirResource(fhirValidator, episodeOfCare, Optional.empty(), ResourceType.EpisodeOfCare.name(), "Create EpisodeOfCare(When creating Patient)");
                    //Create
                    FhirOperationUtil.createFhirResource(fhirClient, episodeOfCare, ResourceType.EpisodeOfCare.name());

                });
            }

        } else {
            log.info("Patient already exists with the given identifier system and value");
            throw new DuplicateResourceFoundException("Patient already exists with the given identifier system and value");
        }
    }

    @Override
    public void updatePatient(PatientDto patientDto) {
        if (!isDuplicateWhileUpdate(patientDto)) {
            //Add mpi to the identifiers
            List<IdentifierDto> identifierDtos = patientDto.getIdentifier();
            Patient patientToGetMpi = fhirClient.read().resource(Patient.class).withId(patientDto.getId()).execute();
            Optional<String> mrn = patientToGetMpi.getIdentifier().stream()
                    .filter(p -> p.getSystem().equalsIgnoreCase(fisProperties.getPatient().getMrn().getCodeSystem()))
                    .findFirst().map(Identifier::getValue);

            mrn.ifPresent(m -> identifierDtos.add(setUniqueIdentifierForPatient(m)));

            patientDto.setIdentifier(identifierDtos);
            final Patient patient = modelMapper.map(patientDto, Patient.class);
            patient.setId(new IdType(patientDto.getId()));
            patient.setGender(FhirResourceUtil.getPatientGender(patientDto.getGenderCode()));
            patient.setBirthDate(java.sql.Date.valueOf(patientDto.getBirthDate()));

            setExtensionFields(patient, patientDto);


            //Set Profile Meta Data
            FhirProfileUtil.setPatientProfileMetaData(fhirClient, patient);
            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, patient, Optional.of(patientDto.getId()), ResourceType.Patient.name(), "Update Patient");
            //Update
            MethodOutcome methodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, patient, ResourceType.Patient.name());

            Reference patientId = new Reference();
            patientId.setReference("Patient/" + methodOutcome.getId().getIdPart());

            patientDto.getFlags().ifPresent(flags -> flags.forEach(flagDto -> {
                if (!duplicateCheckForFlag(flagDto, patientDto.getId())) {
                    Flag flag = convertFlagDtoToFlag(patientId, flagDto);
                    //Set Profile Meta Data
                    FhirProfileUtil.setFlagProfileMetaData(fhirClient, flag);

                    if (flagDto.getLogicalId() != null) {
                        flag.setId(flagDto.getLogicalId());
                        //Validate
                        FhirOperationUtil.validateFhirResource(fhirValidator, flag, Optional.empty(), ResourceType.Flag.name(), "Update Flag(When updating Patient)");
                        //Update
                        FhirOperationUtil.updateFhirResource(fhirClient, flag, ResourceType.Flag.name());
                    } else {
                        //Validate
                        FhirOperationUtil.validateFhirResource(fhirValidator, flag, Optional.empty(), ResourceType.Flag.name(), "Create Flag(When updating Patient)");
                        //Create
                        FhirOperationUtil.createFhirResource(fhirClient, flag, ResourceType.Flag.name());
                    }
                } else {
                    throw new DuplicateResourceFoundException("Same flag is already present for this patient.");
                }
            }));

            // if flags are not present
            if (!patientDto.getFlags().isPresent()) {
                // TODO:: update the existing flags with enteredinerror status or remove them

            }

            //Update the episode of care
            if (patientDto.getEpisodeOfCares() != null && !patientDto.getEpisodeOfCares().isEmpty()) {
                patientDto.getEpisodeOfCares().forEach(eoc -> {
                    ReferenceDto patientReference = new ReferenceDto();
                    patientReference.setReference(ResourceType.Patient + "/" + patientDto.getId());
                    patientDto.getName().stream().findAny().ifPresent(name -> patientReference.setDisplay(name.getFirstName() + " " + name.getLastName()));
                    eoc.setPatient(patientReference);
                    eoc.setManagingOrganization(orgReference(patientDto.getOrganizationId()));
                    if (eoc.getCareManager() == null) {
                        eoc.setCareManager(pracReference(patientDto.getPractitionerId()));
                    }
                    EpisodeOfCare episodeOfCare = convertEpisodeOfCareDtoToEpisodeOfCare(eoc);
                    //Set Profile Meta Data
                    FhirProfileUtil.setEpisodeOfCareProfileMetaData(fhirClient, episodeOfCare);
                    if (eoc.getId() != null) {
                        episodeOfCare.setId(eoc.getId());
                        //Validate
                        FhirOperationUtil.validateFhirResource(fhirValidator, episodeOfCare, Optional.of(eoc.getId()), ResourceType.EpisodeOfCare.name(), "Update EpisodeOfCare(When updating Patient)");
                        //Update
                        FhirOperationUtil.updateFhirResource(fhirClient, episodeOfCare, ResourceType.EpisodeOfCare.name());
                    } else {
                        //Validate
                        FhirOperationUtil.validateFhirResource(fhirValidator, episodeOfCare, Optional.empty(), ResourceType.EpisodeOfCare.name(), "Create EpisodeOfCare(When updating Patient)");
                        //Create
                        FhirOperationUtil.createFhirResource(fhirClient, episodeOfCare, ResourceType.EpisodeOfCare.name());
                    }
                });
            }

            //Update the coverage
            patientDto.getCoverages().ifPresent(coverages -> coverages.forEach(coverageDto -> {
                if (coverageDto.getLogicalId() != null) {
                    coverageService.updateCoverage(coverageDto.getLogicalId(), coverageDto);
                } else {
                    coverageService.createCoverage(coverageDto);
                }
            }));

        } else {
            throw new DuplicateResourceFoundException("Patient already exists with the given identifier system and value.");
        }
    }

    @Override
    public PatientDto getPatientById(String patientId) {
        Bundle patientBundle = fhirClient.search().forResource(Patient.class)
                .where(new TokenClientParam("_id").exactly().code(patientId))
                .revInclude(Flag.INCLUDE_PATIENT)
                .revInclude(EpisodeOfCare.INCLUDE_PATIENT)
                .revInclude(Coverage.INCLUDE_BENEFICIARY)
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
        patientDto.setMrn(patientDto.getIdentifier().stream().filter(iden -> iden.getSystem().equalsIgnoreCase(fisProperties.getPatient().getMrn().getCodeSystem())).findFirst().map(IdentifierDto::getValue));
        patientDto.setIdentifier(patientDto.getIdentifier().stream().filter(iden -> !iden.getSystem().equalsIgnoreCase(fisProperties.getPatient().getMrn().getCodeSystem())).collect(toList()));
        if (patient.hasManagingOrganization()) {
            patientDto.setOrganizationId(Optional.ofNullable(patient.getManagingOrganization().getReference().split("/")[1]));
        }
        //Get Flags for the patient
        List<FlagDto> flagDtos = getFlagsForEachPatient(patientBundle.getEntry(), patientBundleEntry.getResource().getIdElement().getIdPart());
        patientDto.setFlags(Optional.ofNullable(flagDtos));

        List<EpisodeOfCareDto> eocDtos = getEocsForEachPatient(patientBundle.getEntry(), patientBundleEntry.getResource().getIdElement().getIdPart());
        patientDto.setEpisodeOfCares(eocDtos);

        List<CoverageDto> coverageDtos = getConveragesForEachPatient(patientBundle.getEntry(), patientBundleEntry.getResource().getIdElement().getIdPart());
        patientDto.setCoverages(Optional.ofNullable(coverageDtos));

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
                    .map(patient -> mapPatientToPatientDto(patient, response.getEntry()))
                    .collect(toList());
        }
        log.info("Total Patients retrieved from Server #" + patientDtos.size());
        return patientDtos;
    }

    private List<FlagDto> getFlagsForEachPatient(List<Bundle.BundleEntryComponent> patientAndAllReferenceBundle, String patientId) {
        return patientAndAllReferenceBundle.stream().filter(patientWithAllReference -> patientWithAllReference.getResource().getResourceType().equals(ResourceType.Flag))
                .map(flagBundle -> (Flag) flagBundle.getResource())
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

    private List<EpisodeOfCareDto> getEocsForEachPatient(List<Bundle.BundleEntryComponent> patientAndAllReferenceBundle, String patientId) {
        return patientAndAllReferenceBundle.stream().filter(patientWithAllReference -> patientWithAllReference.getResource().getResourceType().equals(ResourceType.EpisodeOfCare))
                .map(eocBundle -> (EpisodeOfCare) eocBundle.getResource())
                .filter(eoc -> eoc.getPatient().getReference().equalsIgnoreCase("Patient/" + patientId))
                .map(eoc -> EpisodeOfCareToEpisodeOfCareDtoMapper.map(eoc, lookUpService)).collect(toList());
    }

    private List<CoverageDto> getConveragesForEachPatient(List<Bundle.BundleEntryComponent> patientAndAllReferenceBundle, String patientId) {
        return patientAndAllReferenceBundle.stream().filter(patientWithAllReference -> patientWithAllReference.getResource().getResourceType().equals(ResourceType.Coverage))
                .map(coverageBundle -> (Coverage) coverageBundle.getResource())
                .filter(coverage -> coverage.getBeneficiary().getReference().equalsIgnoreCase("Patient/" + patientId))
                .map(CoverageToCoverageDtoMap::map)
                .collect(toList());
    }

    private void setExtensionFields(Patient patient, PatientDto patientDto) {
        List<Extension> extensionList = new ArrayList<>();

        //language
        if (patientDto.getLanguage() != null && !patientDto.getLanguage().isEmpty()) {
            Coding langCoding = FhirResourceUtil.getCoding(patientDto.getLanguage(), "", StructureDefinitionEnum.LANGUAGES.getUrl());
            Extension langExtension = FhirResourceUtil.createExtension(StructureDefinitionEnum.US_CORE_SIMPLE_LANGUAGE.getUrl(), new CodeableConcept().addCoding(langCoding));
            extensionList.add(langExtension);
        }

        //race
        if (patientDto.getRace() != null && !patientDto.getRace().isEmpty()) {
            Coding raceCoding = FhirResourceUtil.getCoding(patientDto.getRace(), "", StructureDefinitionEnum.RACE.getUrl());
            Extension raceExtension = FhirResourceUtil.createExtension(StructureDefinitionEnum.US_CORE_RACE.getUrl(), new CodeableConcept().addCoding(raceCoding));
            extensionList.add(raceExtension);
        }

        //ethnicity
        if (patientDto.getEthnicity() != null && !patientDto.getEthnicity().isEmpty()) {
            Coding ethnicityCoding = FhirResourceUtil.getCoding(patientDto.getEthnicity(), "", StructureDefinitionEnum.ETHNICITY.getUrl());
            Extension ethnicityExtension = FhirResourceUtil.createExtension(StructureDefinitionEnum.US_CORE_ETHNICITY.getUrl(), new CodeableConcept().addCoding(ethnicityCoding));
            extensionList.add(ethnicityExtension);
        }

        //us-core-birthsex
        if (patientDto.getBirthSex() != null && !patientDto.getBirthSex().isEmpty()) {
            Coding birthSexCoding = FhirResourceUtil.getCoding(patientDto.getBirthSex(), "", StructureDefinitionEnum.ADMINISTRATIVE_GENDER.getUrl());
            Extension birthSexExtension = FhirResourceUtil.createExtension(StructureDefinitionEnum.US_CORE_BIRTHSEX.getUrl(), new CodeableConcept().addCoding(birthSexCoding));
            extensionList.add(birthSexExtension);
        }

        patient.setExtension(extensionList);
    }

    private void mapExtensionFields(Patient patient, PatientDto patientDto) {
        List<Extension> extensionList = patient.getExtension();

        extensionList.stream().map(extension -> (CodeableConcept) extension.getValue())
                .forEach(codeableConcept -> codeableConcept.getCoding().stream().findFirst().ifPresent(coding -> {
                    if (coding.getSystem().contains(StructureDefinitionEnum.RACE.getUrl())) {
                        patientDto.setRace(FhirDtoUtil.convertCodeToValueSetDto(coding.getCode(), lookUpService.getUSCoreRace()).getCode());
                    } else if (coding.getSystem().contains(StructureDefinitionEnum.LANGUAGES.getUrl())) {
                        patientDto.setLanguage(FhirDtoUtil.convertCodeToValueSetDto(coding.getCode(), lookUpService.getLanguages()).getCode());
                    } else if (coding.getSystem().contains(StructureDefinitionEnum.ETHNICITY.getUrl())) {
                        patientDto.setEthnicity(FhirDtoUtil.convertCodeToValueSetDto(coding.getCode(), lookUpService.getUSCoreEthnicity()).getCode());
                    } else if (coding.getSystem().contains(StructureDefinitionEnum.ADMINISTRATIVE_GENDER.getUrl())) {
                        patientDto.setBirthSex(FhirDtoUtil.convertCodeToValueSetDto(coding.getCode(), lookUpService.getUSCoreBirthSex()).getCode());
                    }
                }));
    }

    private EpisodeOfCare convertEpisodeOfCareDtoToEpisodeOfCare(EpisodeOfCareDto episodeOfCareDto) {
        EpisodeOfCare episodeOfCare = new EpisodeOfCare();
        try {
            episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.fromCode(episodeOfCareDto.getStatus()));
        } catch (FHIRException e) {
            throw new BadRequestException("No such fhir status or type exist.");
        }
        episodeOfCare.setType(Collections.singletonList(FhirDtoUtil.convertValuesetDtoToCodeableConcept(FhirDtoUtil.convertCodeToValueSetDto(episodeOfCareDto.getType(), lookUpService.getEocType()))));
        episodeOfCare.setPatient(FhirDtoUtil.mapReferenceDtoToReference(episodeOfCareDto.getPatient()));
        episodeOfCare.setManagingOrganization(FhirDtoUtil.mapReferenceDtoToReference(episodeOfCareDto.getManagingOrganization()));
        Period period = new Period();
        try {
            period.setStart(DateUtil.convertStringToDate(episodeOfCareDto.getStartDate()));
            period.setEnd(DateUtil.convertStringToDate(episodeOfCareDto.getEndDate()));
        } catch (ParseException e) {
            throw new BadRequestException("The start and end date of episode of care has wrong syntax.");
        }
        episodeOfCare.setPeriod(period);
        episodeOfCare.setCareManager(FhirDtoUtil.mapReferenceDtoToReference(episodeOfCareDto.getCareManager()));
        return episodeOfCare;
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
        period.setEnd((flagDto.getPeriod().getEnd() != null) ? java.sql.Date.valueOf(flagDto.getPeriod().getEnd()) : null);
        flag.setPeriod(period);

        //Set Author
        if (flagDto.getAuthor() != null && FhirOperationUtil.isStringNotNullAndNotEmpty(flagDto.getAuthor().getReference())) {
            Reference reference = modelMapper.map(flagDto.getAuthor(), Reference.class);
            flag.setAuthor(reference);
        }
        return flag;
    }

    private boolean duplicateCheckForFlag(FlagDto flagDto, String patientId) {
        Bundle flagBundleForPatient = getFlagsByPatient(patientId);
        return flagHasSameCodeAndCategory(flagBundleForPatient, flagDto);
    }

    private Bundle getFlagsByPatient(String patientId) {
        IQuery flagBundleForPatientQuery = fhirClient.search().forResource(Flag.class)
                .where(new ReferenceClientParam("subject").hasId(patientId));
        Bundle flagBundleToCoundTotalNumberOfFlag = (Bundle) flagBundleForPatientQuery.returnBundle(Bundle.class).execute();
        int totalFlagForPatient = flagBundleToCoundTotalNumberOfFlag.getTotal();
        return (Bundle) flagBundleForPatientQuery
                .count(totalFlagForPatient)
                .returnBundle(Bundle.class)
                .execute();
    }

    private boolean flagHasSameCodeAndCategory(Bundle bundle, FlagDto flagDto) {
        List<Flag> duplicateCheckList = new ArrayList<>();
        if (!bundle.isEmpty()) {
            duplicateCheckList = bundle.getEntry().stream()
                    .map(flagResource -> (Flag) flagResource.getResource())
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

    private List<String> patientsInOrganization(String org) {
        Bundle bundleFromPatient = fhirClient.search().forResource(Patient.class)
                .where(new ReferenceClientParam("organization").hasId(org))
                .returnBundle(Bundle.class)
                .sort().descending(PARAM_LASTUPDATED)
                .execute();

        List<String> getPatientIdFromPatient = FhirOperationUtil.getAllBundleComponentsAsList(bundleFromPatient, Optional.empty(), fhirClient, fisProperties)
                .stream().map(pat -> {
                    Patient patient = (Patient) pat.getResource();
                    return patient.getIdElement().getIdPart();
                }).distinct().collect(toList());

        //TODO:Remove the bundle after next data purge.
        Bundle bundle = fhirClient.search().forResource(EpisodeOfCare.class)
                .where(new ReferenceClientParam("organization").hasId(org))
                .returnBundle(Bundle.class)
                .sort().descending(PARAM_LASTUPDATED)
                .execute();
        List<String> getPatientFromEoc = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties).stream().map(eoc -> {
            EpisodeOfCare episodeOfCare = (EpisodeOfCare) eoc.getResource();
            return (episodeOfCare.hasPatient()) ? (episodeOfCare.getPatient().getReference().split("/")[1]) : null;
        }).distinct().collect(toList());

        return Stream.of(getPatientIdFromPatient, getPatientFromEoc).flatMap(Collection::stream).distinct().collect(toList());
    }

    private List<String> patientsAssociatedWithPractitioner(String org, String prac) {
        //List of patient in the organization
        List<String> patientsInOrganization = patientsInOrganization(org);

        //List of organizations to which practitioner is associated with
        List<String> organizationsPractitionerIsAssociatedWith = organizationsOfPractitioner(prac);

        //List of patient with the practitioner's organization in the care team.
        List<String> patientsRelatedWithOrganizationOfPractitionerOnCareTeam = getPatientsByParticipantsInCareTeam(organizationsPractitionerIsAssociatedWith);

        //List of patient with the practitioner in the care team.
        List<String> patientsRealtedWithPractitionerOnCareTeam = getPatientsByParticipantsInCareTeam(Arrays.asList(prac));

        return Stream.of(patientsInOrganization, patientsRelatedWithOrganizationOfPractitionerOnCareTeam, patientsRealtedWithPractitionerOnCareTeam).flatMap(Collection::stream).distinct().collect(toList());
    }

    private List<PatientDto> convertAllBundleToSinglePatientDtoList(Bundle firstPagePatientSearchBundle, int numberOBundlePerPage) {
        List<Bundle.BundleEntryComponent> bundleEntryComponentList = FhirOperationUtil.getAllBundleComponentsAsList(firstPagePatientSearchBundle, Optional.of(numberOBundlePerPage), fhirClient, fisProperties);
        return bundleEntryComponentList.stream()
                .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Patient))
                .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource())
                .map(patient -> mapPatientToPatientDto(patient, bundleEntryComponentList))
                .collect(toList());
    }

    private boolean isDuplicateWhileUpdate(PatientDto patientDto) {
        final Patient patient = fhirClient.read().resource(Patient.class).withId(patientDto.getId()).execute();
        List<Bundle.BundleEntryComponent> bundleEntryComponents = patientsWithMatchedDuplicateCheckParameters(patientDto);
        if (bundleEntryComponents.isEmpty()) {
            return false;
        } else {
            return bundleEntryComponents.stream().noneMatch(resource -> {
                Patient pRes = (Patient) resource.getResource();
                return pRes.getIdElement().getIdPart().equalsIgnoreCase(patient.getIdElement().getIdPart());
            });
        }
    }

    private ReferenceDto orgReference(Optional<String> organizationId) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setDisplay(fhirClient.read().resource(Organization.class).withId(organizationId.get()).execute().getName());
        referenceDto.setReference("Organization/" + organizationId.get());
        return referenceDto;
    }

    private ReferenceDto pracReference(Optional<String> practitionerId) {
        ReferenceDto referenceDto = new ReferenceDto();
        fhirClient.read().resource(Practitioner.class).withId(practitionerId.get()).execute().getName().stream().findAny()
                .ifPresent(name -> referenceDto.setDisplay(name.getGiven().stream().findFirst().get().toString() + " " + name.getFamily()));
        referenceDto.setReference("Practitioner/" + practitionerId.get());
        return referenceDto;
    }


    private boolean checkDuplicateInFhir(PatientDto patientDto) {
        return !patientsWithMatchedDuplicateCheckParameters(patientDto).isEmpty();
    }

    private boolean checkDuplicatePatientOfSameOrganization(PatientDto patientDto) {
        List<Bundle.BundleEntryComponent> patientWithDuplicateParameters = patientsWithMatchedDuplicateCheckParameters(patientDto);
        if (!patientWithDuplicateParameters.isEmpty()) {
            return !patientsWithMatchedDuplicateCheckParameters(patientDto).stream().filter(pat -> {
                Patient patient = (Patient) pat.getResource();
                return (patient.hasManagingOrganization()) && patient.getManagingOrganization().getReference().split("/")[1].equalsIgnoreCase(patientDto.getOrganizationId().get());
            }).collect(toList()).isEmpty();
        }
        return false;
    }

    private IdentifierDto setUniqueIdentifierForPatient(String value) {
        return IdentifierDto.builder().system(fisProperties.getPatient().getMrn().getCodeSystem())
                .systemDisplay(fisProperties.getPatient().getMrn().getDisplayName())
                .value(value).display(value).build();
    }

    private List<Bundle.BundleEntryComponent> patientsWithMatchedDuplicateCheckParameters(PatientDto patientDto) {
        String system = patientDto.getIdentifier().get(0).getSystem();
        String value = patientDto.getIdentifier().get(0).getValue();
        log.info("Searching patients with identifier system : " + system + " and value : " + value);
        Bundle patientBundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(Patient.class)).returnBundle(Bundle.class).execute();
        return FhirOperationUtil.getAllBundleComponentsAsList(patientBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(patient -> {
            Patient p = (Patient) patient.getResource();
            return p.getIdentifier().stream().anyMatch(identifier -> checkIdentifier(system, value, identifier)) &&
                    checkFirstName(p, patientDto)
                    && checkBirthdate(p, patientDto) && checkGender(p, patientDto);
        }).collect(toList());
    }

    private boolean checkIdentifier(String system, String value, Identifier identifier) {
        return identifier.getSystem().equalsIgnoreCase(system) && identifier.getValue().replaceAll(" ", "")
                .replaceAll("-", "").trim()
                .equalsIgnoreCase(value.replaceAll(" ", "").replaceAll("-", "").trim());
    }

    private boolean checkFirstName(Patient p, PatientDto patientDto) {
        return p.getName().stream().findAny().get().getGiven().stream().findAny().get().toString().equalsIgnoreCase(patientDto.getName().stream().findAny().get().getFirstName());
    }

    private boolean checkBirthdate(Patient p, PatientDto patientDto) {
        return modelMapper.map(p, PatientDto.class).getBirthDate().toString().equalsIgnoreCase(patientDto.getBirthDate().toString());
    }

    private boolean checkGender(Patient p, PatientDto patientDto) {
        return p.getGender().toCode().equalsIgnoreCase(patientDto.getGenderCode());
    }

    private String generateRandomMrn() {
        StringBuilder localIdIdBuilder = new StringBuilder();
        if (null != fisProperties.getPatient().getMrn().getPrefix()) {
            localIdIdBuilder.append(fisProperties.getPatient().getMrn().getPrefix());
            localIdIdBuilder.append("-");
        }
        localIdIdBuilder.append(RandomStringUtils
                .randomAlphanumeric((fisProperties.getPatient().getMrn().getLength())));
        return localIdIdBuilder.toString().toUpperCase();
    }


    private List<String> getPatientsByParticipantsInCareTeam(List<String> participants) {
        List<String> patients = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("participant").hasAnyOfIds(participants))
                .returnBundle(Bundle.class)
                .execute();

        if (bundle != null) {
            patients = bundle.getEntry().stream()
                    .map(it -> (CareTeam) it.getResource())
                    .map(it -> it.getSubject().getReference().split("/")[1])
                    .collect(toList());

        }

        return patients;
    }

    private List<String> organizationsOfPractitioner(String practitioner) {
        List<String> org = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("practitioner").hasId(practitioner))
                .returnBundle(Bundle.class)
                .execute();

        if (bundle != null) {
            org = bundle.getEntry().stream()
                    .map(it -> (PractitionerRole) it.getResource())
                    .map(pr -> pr.getOrganization().getReference().split("/")[1])
                    .collect(toList());
        }

        return org;
    }


}


