package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.GeneralConsentRelatedFieldDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.PreconditionFailedException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.service.pdf.ConsentPdfGenerator;
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
import org.hl7.fhir.dstu3.model.codesystems.V3ActReason;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
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
    private final ConsentPdfGenerator consentPdfGenerator;


    @Autowired
    private FhirValidator fhirValidator;


    @Autowired
    public ConsentServiceImpl(ModelMapper modelMapper,
                              IGenericClient fhirClient,
                              LookUpService lookUpService,
                              FisProperties fisProperties,
                              ConsentPdfGenerator consentPdfGenerator) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.consentPdfGenerator = consentPdfGenerator;
    }

    @Override
    public PageDto<ConsentDto> getConsents(Optional<String> patient, Optional<String> practitioner, Optional<String> status, Optional<Boolean> generalDesignation, Optional<Integer> pageNumber, Optional<Integer> pageSize) {

        int numberOfConsentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Consent.name());
        Bundle firstPageConsentBundle;
        Bundle otherPageConsentBundle;

        // Generate the Query Based on Input Variables
        IQuery iQuery = getConsentIQuery(patient, practitioner, status, generalDesignation);

        // Disable caching to get latest data
        iQuery = setNoCacheControlDirective(iQuery);

        //Apply Filters Based on Input Variables

        firstPageConsentBundle = PaginationUtil.getSearchBundleFirstPage(iQuery, numberOfConsentsPerPage, Optional.empty());

        if (firstPageConsentBundle == null || firstPageConsentBundle.getEntry().isEmpty()) {
            return new PageDto<>(new ArrayList<>(), numberOfConsentsPerPage, 0, 0, 0, 0);
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
    public ConsentDto getConsentsById(String consentId) {
        log.info("Searching for consentId: " + consentId);
        Bundle consentBundle = fhirClient.search().forResource(Consent.class)
                .where(new TokenClientParam("_id").exactly().code(consentId.trim()))
                .returnBundle(Bundle.class)
                .execute();

        if (consentBundle == null || consentBundle.getEntry().isEmpty()) {
            log.info("No consent was found for the given consentId:" + consentId);
            throw new ResourceNotFoundException("No consent was found for the given consent ID:" + consentId);
        }

        log.info("FHIR consent bundle retrieved from FHIR server successfully for consent ID:" + consentId);

        Bundle.BundleEntryComponent retrievedConsent = consentBundle.getEntry().get(0);
        return convertConsentBundleEntryToConsentDto(retrievedConsent);
    }

    @Override
    public GeneralConsentRelatedFieldDto getGeneralConsentRelatedFields(String patient) {
        GeneralConsentRelatedFieldDto generalConsentRelatedFieldDto = new GeneralConsentRelatedFieldDto();

        //Adding To careTeams
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("subject").hasId(patient))
                .returnBundle(Bundle.class).execute();

        if (!careTeamBundle.getEntry().isEmpty()) {
            List<ReferenceDto> toActors = careTeamBundle.getEntry().stream().map(careTeamEntry -> {
                CareTeam careTeam = (CareTeam) careTeamEntry.getResource();
                return convertCareTeamToReferenceDto(careTeam);
            }).collect(Collectors.toList());

            generalConsentRelatedFieldDto.setToActors(toActors);

            //Adding from careTeams
            Bundle organizationBundle = getPseudoOrganization();

            organizationBundle.getEntry().stream().findAny().ifPresent(entry -> {
                Organization organization = (Organization) entry.getResource();
                ReferenceDto referenceDto = new ReferenceDto();
                referenceDto.setReference("Organization/" + organization.getIdElement().getIdPart());
                referenceDto.setDisplay(PSEUDO_ORGANIZATION_NAME);
                generalConsentRelatedFieldDto.setFromActors(Arrays.asList(referenceDto));
            });

            generalConsentRelatedFieldDto.setPurposeOfUse(FhirDtoUtil.convertCodeToValueSetDto(V3ActReason.TREAT.toCode(), lookUpService.getPurposeOfUse()));

        } else {
            throw new ResourceNotFoundException("No care teams are present.");
        }
        return generalConsentRelatedFieldDto;
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
            throw new PreconditionFailedException("No care team members for this patient.");
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
        consentDto.getFromActor().forEach(member -> {
            if (member.getDisplay().equalsIgnoreCase("Omnibus Care Plan (SAMHSA)"))
                consentDto.setGeneralDesignation(true);
        });
        return consentDto;
    }

    private IQuery getConsentIQuery(Optional<String> patient, Optional<String> practitioner, Optional<String> status, Optional<Boolean> generalDesignation) {
        IQuery iQuery = fhirClient.search().forResource(Consent.class);

        //Query the status.
        if (status.isPresent()) {
            iQuery.where(new TokenClientParam("status").exactly().code("active"));
        } else {
            //query with practitioner.
            practitioner.ifPresent(pr->{
                if(!getCareTeamIdsFromPractitioner(pr).isEmpty()) {
                    iQuery.where(new ReferenceClientParam("actor").hasAnyOfIds(getCareTeamIdsFromPractitioner(pr)));
                }else{
                    throw new ResourceNotFoundException("Care Team Member cannot be found for the practitioner");
                }
            });

            //query with patient.
            patient.ifPresent(pt-> iQuery.where(new ReferenceClientParam("patient").hasId(pt)));

            //Query with general designation.
            generalDesignation.ifPresent(gd -> {
                if (gd.booleanValue()) {
                    String pseudoOrgId = getPseudoOrganization().getEntry().stream().findFirst().map(pseudoOrgEntry -> {
                        Organization organization = (Organization) pseudoOrgEntry.getResource();
                        String id = organization.getIdElement().getIdPart();
                        return id;
                    }).get();
                    iQuery.where(new ReferenceClientParam("actor").hasId(pseudoOrgId));
                }
            });

            if (!practitioner.isPresent() && !patient.isPresent()) {
                throw new ResourceNotFoundException("Practitioner or Patient is required to find Consents");
            }
        }
        return iQuery;
    }

    @Override
    public void attestConsent(String consentId) {
        Consent consent = fhirClient.read().resource(Consent.class).withId(consentId.trim()).execute();
        consent.setStatus(Consent.ConsentState.ACTIVE);

        fhirClient.update().resource(consent).execute();
    }


    @Override
    public void saveConsent(ConsentDto consentDto) {

        try {
            consentPdfGenerator.generateConsentPdf(consentDto);
        } catch (IOException e) {

        }
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
            if (consentDto.getStatus() != null) {
                try {
                    consent.setStatus(Consent.ConsentState.fromCode(consentDto.getStatus()));
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

    private ReferenceDto convertCareTeamToReferenceDto(CareTeam careTeam) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference(careTeam.getIdElement().getIdPart());
        referenceDto.setDisplay(careTeam.getName());
        return referenceDto;
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
                if ((fromActor.size() == 1)) {
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

    private List<String> getCareTeamIdsFromPractitioner(String practitioner) {
        IQuery careTeamQuery = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("participant").hasId(practitioner));

        Bundle careTeamBundle = (Bundle) careTeamQuery.returnBundle(Bundle.class).execute();

        List<String> careTeamIds = careTeamBundle.getEntry().stream().map(careTeamBundleEntry -> {
            CareTeam careTeam = (CareTeam) careTeamBundleEntry.getResource();
            return careTeam.getIdElement().getIdPart();
        }).collect(Collectors.toList());

        return careTeamIds;
    }

    private IQuery setNoCacheControlDirective(IQuery searchQuery) {
        final CacheControlDirective cacheControlDirective = new CacheControlDirective();
        cacheControlDirective.setNoCache(true);
        searchQuery.cacheControl(cacheControlDirective);
        return searchQuery;
    }

}
