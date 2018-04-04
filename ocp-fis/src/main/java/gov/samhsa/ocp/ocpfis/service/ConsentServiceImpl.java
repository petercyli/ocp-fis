package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ConsentServiceImpl implements ConsentService {
    public static final String INFORMANT_CODE = "INF";
    public static final String INFORMANT_RECIPIENT_CODE = "IRCP";
    public static final String PSEUDO_ORGANIZATION_NAME = "Omnibus Care Plan (SAMHSA)";
    public static final String PSEUDO_ORGANIZATION_TAX_ID = "530196960";

    private final IGenericClient fhirClient;
    private final LookUpService lookUpService;
    private final FisProperties fisProperties;
    private final ModelMapper modelMapper;

    @Autowired
    private FhirValidator fhirValidator;


    @Autowired
    public ConsentServiceImpl(ModelMapper modelMapper,
                              IGenericClient fhirClient,
                              LookUpService lookUpService,
                              FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<ConsentDto> getConsents(Optional<String> patient, Optional<String> fromActor, Optional<String> toActor, Optional<Boolean> generalDesignation, Optional<String> status, Optional<Integer> pageNumber, Optional<Integer> pageSize) {

        int numberOfConsentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Consent.name());
        Bundle firstPageConsentBundle;
        Bundle otherPageConsentBundle;

        // Generate the Query Based on Input Variables
        IQuery iQuery = getConsentIQuery(patient, fromActor, toActor, status);

        //Apply Filters Based on Input Variables

        firstPageConsentBundle = PaginationUtil.getSearchBundleFirstPage(iQuery, numberOfConsentsPerPage, Optional.empty());

        if (firstPageConsentBundle == null || firstPageConsentBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Consents were found in the FHIR server.");
        }

        log.info("FHIR Consent(s) bundle retrieved " + firstPageConsentBundle.getTotal() + " Consent(s) from FHIR server successfully");
        otherPageConsentBundle = firstPageConsentBundle;


        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageConsentBundle.getLink(Bundle.LINK_NEXT) != null) {
            otherPageConsentBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageConsentBundle, pageNumber.get(), numberOfConsentsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedConsents = otherPageConsentBundle.getEntry();

        // Map to DTO
        List<ConsentDto> consentDtosList = retrievedConsents.stream().map(this::convertConsentBundleEntryToConsentDto).collect(Collectors.toList());
        return (PageDto<ConsentDto>) PaginationUtil.applyPaginationForSearchBundle(consentDtosList, otherPageConsentBundle.getTotal(), numberOfConsentsPerPage, pageNumber);

    }

    @Override
    public void createConsent(ConsentDto consentDto) {
        //Create Consent
        Bundle associatedCareTeam = fhirClient.search().forResource(CareTeam.class).where(new ReferenceClientParam("patient").hasId(consentDto.getPatient().getReference()))
                .returnBundle(Bundle.class).execute();
        if (!associatedCareTeam.getEntry().isEmpty()) {
            if (!isDuplicate(consentDto, Optional.empty())) {
                Consent consent = consentDtoToConsent(Optional.empty(), consentDto);

                //Validate
                FhirUtil.validateFhirResource(fhirValidator, consent, Optional.empty(), ResourceType.Consent.name(), "Create Consent");

                fhirClient.create().resource(consent).execute();
            } else {
                throw new DuplicateResourceFoundException("This patient already has a general designation consent.");
            }
        } else {
            throw new ResourceNotFoundException("No care team members for this patient.");
        }
    }

    @Override
    public void updateConsent(String consentId, ConsentDto consentDto) {
        //Update Consent
        if (!isDuplicate(consentDto, Optional.of(consentId))) {
            Consent consent = consentDtoToConsent(Optional.of(consentId), consentDto);
            consent.setId(consentId);

            //Validate
            FhirUtil.validateFhirResource(fhirValidator, consent, Optional.of(consentId), ResourceType.Consent.name(), "Update Consent");

            fhirClient.update().resource(consent).execute();
        } else {
            throw new DuplicateResourceFoundException("This patient already has a general designation consent.");
        }
    }


    private ConsentDto convertConsentBundleEntryToConsentDto(Bundle.BundleEntryComponent fhirConsentDtoModel) {
        ConsentDto consentDto = modelMapper.map(fhirConsentDtoModel.getResource(), ConsentDto.class);
        return consentDto;
    }

    private IQuery getConsentIQuery(Optional<String> patient, Optional<String> fromActor, Optional<String> toActor, Optional<String> status) {
        IQuery iQuery = fhirClient.search().forResource(Consent.class);

        //Get Sub tasks by parent task id
        if (status.isPresent()) {
            iQuery.where(new TokenClientParam("status").exactly().code("active"));
        } else {
            //query the task and sub-task owned by specific practitioner
            if ((fromActor.isPresent() || toActor.isPresent()) && !patient.isPresent()) {
                iQuery.where(new ReferenceClientParam("actor").hasId(fromActor.get()));
            }

            //query the task and sub-task for the specific patient
            if (patient.isPresent() && !fromActor.isPresent() && !toActor.isPresent()) {
                iQuery.where(new ReferenceClientParam("patient").hasId(patient.get()));
            }

            //query the task and sub-task owned by specific practitioner and for the specific patient
            if ((fromActor.isPresent() || toActor.isPresent()) && patient.isPresent()) {
                iQuery.where(new ReferenceClientParam("actor").hasId(fromActor.get()))
                        .where(new ReferenceClientParam("patient").hasId(patient.get()));
            }
        }
        return iQuery;
    }

    private Consent consentDtoToConsent(Optional<String> consentId, ConsentDto consentDto) {
        Consent consent = new Consent();
        if (consentDto.getPeriod() != null) {
            Period period = new Period();
            period.setStart((consentDto.getPeriod().getStart() != null) ? java.sql.Date.valueOf(consentDto.getPeriod().getStart()) : null);
            period.setEnd((consentDto.getPeriod().getEnd() != null) ? java.sql.Date.valueOf(consentDto.getPeriod().getEnd()) : null);
            consent.setPeriod(period);
        }

        consent.setPatient(FhirDtoUtil.mapReferenceDtoToReference(consentDto.getPatient()));

        if (!consentDto.getCategory().isEmpty() && consentDto.getCategory() != null) {
            List<CodeableConcept> categories = consentDto.getCategory().stream()
                    .map(category -> FhirDtoUtil.convertValuesetDtoToCodeableConcept(category))
                    .collect(Collectors.toList());
            consent.setCategory(categories);
        }

        if (consentDto.getDateTime() != null) {
            consent.setDateTime(java.sql.Date.valueOf(consentDto.getDateTime()));
        } else {
            consent.setDateTime(java.sql.Date.valueOf(LocalDate.now()));
        }

        if (!consentDto.getPurpose().isEmpty() && consentDto.getPurpose() != null) {
            List<Coding> purposes = consentDto.getPurpose().stream().map(purpose -> {
                Coding coding = new Coding();
                coding.setDisplay((purpose.getDisplay() != null && !purpose.getDisplay().isEmpty()) ? purpose.getDisplay() : null)
                        .setCode((purpose.getCode() != null && !purpose.getCode().isEmpty()) ? purpose.getCode() : null)
                        .setSystem((purpose.getSystem() != null && !purpose.getSystem().isEmpty()) ? purpose.getSystem() : null);
                return coding;
            }).collect(Collectors.toList());

            consent.setPurpose(purposes);
        }

        if (consentDto.getStatus() != null) {
            if (consentDto.getStatus().getCode() != null) {
                try {
                    consent.setStatus(Consent.ConsentState.fromCode(consentDto.getStatus().getCode()));
                } catch (FHIRException e) {
                    throw new ResourceNotFoundException("Invalid consent status found.");
                }
            }
        }

        //Setting identifier
        if (!consentId.isPresent()) {
            Identifier identifier = new Identifier();
            identifier.setValue(UUID.randomUUID().toString());
            identifier.setSystem(fisProperties.getConsent().getIdentifierSystem());
            consent.setIdentifier(identifier);
        } else if (consentDto.getIdentifier() != null) {
            Identifier identifier = new Identifier();
            identifier.setValue(consentDto.getIdentifier().getValue());
            identifier.setSystem(consentDto.getIdentifier().getSystem());
            consent.setIdentifier(identifier);
        }


        List<Consent.ConsentActorComponent> actors = new ArrayList<>();

        //Getting psuedo organization
        Bundle organizationBundle = getPseudoOrganization();

        organizationBundle.getEntry().stream().findAny().ifPresent(entry -> {
            Organization organization = (Organization) entry.getResource();
            ReferenceDto referenceDto = new ReferenceDto();
            referenceDto.setReference("Organization/" + organization.getIdElement().getIdPart());
            referenceDto.setDisplay(PSEUDO_ORGANIZATION_NAME);
            consent.setOrganization(Arrays.asList(FhirDtoUtil.mapReferenceDtoToReference(referenceDto)));

            if (consentDto.isGeneralDesignation()) {
                Consent.ConsentActorComponent fromActor = new Consent.ConsentActorComponent();
                fromActor.setReference(FhirDtoUtil.mapReferenceDtoToReference(referenceDto))
                        .setRole(FhirDtoUtil.convertValuesetDtoToCodeableConcept(FhirDtoUtil.convertCodeToValueSetDto(INFORMANT_CODE, lookUpService.getSecurityRole())));
                actors.add(fromActor);
            }
        });

        if (consentDto.isGeneralDesignation()) {
            //Adding To careTeams
            Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                    .where(new ReferenceClientParam("subject").hasId(consentDto.getPatient().getReference()))
                    .returnBundle(Bundle.class).execute();

            careTeamBundle.getEntry().forEach(careTeamEntry -> {
                CareTeam careTeam = (CareTeam) careTeamEntry.getResource();
                Consent.ConsentActorComponent toActor = convertCareTeamToActor(careTeam, FhirDtoUtil.convertCodeToValueSetDto(INFORMANT_RECIPIENT_CODE, lookUpService.getSecurityRole()));
                actors.add(toActor);
            });
            consent.setActor(actors);
        }

        return consent;

    }

    private Consent.ConsentActorComponent convertCareTeamToActor(CareTeam careTeam, ValueSetDto securityRoleValueSet) {
        Consent.ConsentActorComponent actor = new Consent.ConsentActorComponent();
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference("CareTeam/" + careTeam.getIdElement().getIdPart());
        referenceDto.setDisplay(careTeam.getName());
        actor.setReference(FhirDtoUtil.mapReferenceDtoToReference(referenceDto));
        actor.setRole(FhirDtoUtil.convertValuesetDtoToCodeableConcept(securityRoleValueSet));
        return actor;
    }

    private boolean isDuplicate(ConsentDto consentDto, Optional<String> consentId) {
        //Duplicate Check For General Designation
        if (consentDto.isGeneralDesignation()) {
            Bundle consentBundle = fhirClient.search().forResource(Consent.class).where(new ReferenceClientParam("patient").hasId(consentDto.getPatient().getReference()))
                    .returnBundle(Bundle.class).execute();
            boolean checkFromBundle = consentBundle.getEntry().stream().anyMatch(consentBundleEntry -> {
                Consent consent = (Consent) consentBundleEntry.getResource();
                List<String> fromActor = getReferenceOfCareTeam(consent, INFORMANT_CODE);

                String pseudoOrgRef = getPseudoOrganization().getEntry().stream().findFirst().map(pseudoOrg -> {
                    Organization organization = (Organization) pseudoOrg.getResource();
                    return organization.getIdElement().getIdPart();
                }).get();
                System.out.println("the org is " + pseudoOrgRef);
                if ((fromActor.size() == 1)) {
                    System.out.println(fromActor.stream().findFirst().get());
                    if (fromActor.stream().findFirst().get().equalsIgnoreCase("Organization/" + pseudoOrgRef)) {
                        if (consentId.isPresent()) {
                            return !(consentId.get().equalsIgnoreCase(consent.getIdElement().getIdPart()));
                        } else {
                            return true;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            });

            return checkFromBundle;
        }
        return false;
    }

    private List<String> getReferenceOfCareTeam(Consent consent, String code) {
        return consent.getActor().stream().filter(actor -> actor.getRole().getCoding().stream()
                .anyMatch(role -> role.getCode().equalsIgnoreCase(code)))
                .map(actor -> actor.getReference().getReference())
                .collect(Collectors.toList());
    }

    private Bundle getPseudoOrganization() {
        return fhirClient.search().forResource(Organization.class)
                .where(new TokenClientParam("identifier").exactly().code(PSEUDO_ORGANIZATION_TAX_ID))
                .where(new StringClientParam("name").matches().value(PSEUDO_ORGANIZATION_NAME))
                .returnBundle(Bundle.class)
                .execute();
    }

}
