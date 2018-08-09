package gov.samhsa.ocp.ocpfis.util;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.constants.ActivityDefinitionConstants;
import gov.samhsa.ocp.ocpfis.constants.IdentifierConstants;
import gov.samhsa.ocp.ocpfis.domain.KnownIdentifierSystemEnum;
import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.AbstractCareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.IdentifierSystemNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.dstu3.model.Timing;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointSystem;
import org.hl7.fhir.dstu3.model.codesystems.DefinitionTopic;
import org.hl7.fhir.dstu3.model.codesystems.TaskPerformerType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class FhirResourceUtil {

    public static Enumerations.AdministrativeGender getPatientGender(String codeString) {
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

    public static Coding getCoding(String code, String display, String system) {
        Coding coding = new Coding();
        if (isStringNotNullAndNotEmpty(code)) {
            coding.setCode(code);
        }

        if (isStringNotNullAndNotEmpty(display)) {
            coding.setDisplay(display);
        }

        if (isStringNotNullAndNotEmpty(system)) {
            coding.setSystem(system);
        }
        return coding;
    }

    public static boolean checkPatientName(Patient patient, String searchValue) {
        return patient.getName()
                .stream()
                .anyMatch(humanName -> humanName.getGiven().stream().anyMatch(name -> name.toString().equalsIgnoreCase(searchValue)) || humanName.getFamily().equalsIgnoreCase(searchValue));
    }

    public static boolean checkPatientId(Patient patient, String searchValue) {
        return patient.getIdentifier()
                .stream()
                .anyMatch(identifier -> identifier.getValue().equalsIgnoreCase(searchValue));

    }

    public static boolean isStringNotNullAndNotEmpty(String givenString) {
        return givenString != null && !givenString.trim().isEmpty();
    }

    public static boolean isStringNullOrEmpty(String givenString) {
        return givenString == null || givenString.trim().isEmpty();
    }

    public static String getRoleFromCodeableConcept(CodeableConcept codeableConcept) {
        Optional<Coding> codingRoleCode = codeableConcept.getCoding().stream().findFirst();
        return codingRoleCode.isPresent() ? codingRoleCode.get().getCode() : "";
    }

    public static Extension createExtension(String url, Type t) {
        Extension ext = new Extension();
        ext.setUrl(url);
        ext.setValue(t);
        return ext;
    }

    public static ActivityDefinition createToDoActivityDefinition(String referenceOrganization, FisProperties fisProperties, LookUpService lookUpService, IGenericClient fhirClient) {
        ActivityDefinition activityDefinition = new ActivityDefinition();
        activityDefinition.setVersion(fisProperties.getActivityDefinition().getVersion());
        activityDefinition.setName(ActivityDefinitionConstants.TO_DO);
        activityDefinition.setTitle(ActivityDefinitionConstants.TO_DO);

        activityDefinition.setStatus(Enumerations.PublicationStatus.ACTIVE);

        activityDefinition.setKind(ActivityDefinition.ActivityDefinitionKind.ACTIVITYDEFINITION);
        CodeableConcept topic = new CodeableConcept();
        topic.addCoding().setCode(DefinitionTopic.TREATMENT.toCode()).setDisplay(DefinitionTopic.TREATMENT.getDisplay())
                .setSystem(DefinitionTopic.TREATMENT.getSystem());
        activityDefinition.setTopic(Collections.singletonList(topic));

        activityDefinition.setDate(java.sql.Date.valueOf(LocalDate.now()));
        activityDefinition.setPublisher("Organization/" + referenceOrganization);
        activityDefinition.setDescription(ActivityDefinitionConstants.TO_DO);

        Period period = new Period();
        period.setStart(java.sql.Date.valueOf(LocalDate.now()));
        period.setEnd(java.sql.Date.valueOf(LocalDate.now().plusYears(fisProperties.getDefaultEndPeriod())));
        activityDefinition.setEffectivePeriod(period);

        Timing timing = new Timing();
        timing.getRepeat().setDurationMax(fisProperties.getDefaultMaxDuration());
        timing.getRepeat().setDuration(fisProperties.getDefaultMaxDuration());
        timing.getRepeat().setDurationUnit(Timing.UnitsOfTime.D);
        timing.getRepeat().setFrequency(ActivityDefinitionConstants.ACTIVITY_DEFINITION_FREQUENCY);
        activityDefinition.setTiming(timing);

        CodeableConcept participantRole = new CodeableConcept();
        ValueSetDto participantRoleValueSet = FhirDtoUtil.convertCodeToValueSetDto("O", lookUpService.getActionParticipantRole());
        participantRole.addCoding().setCode(participantRoleValueSet.getCode())
                .setDisplay(participantRoleValueSet.getDisplay())
                .setSystem(participantRoleValueSet.getSystem());
        activityDefinition.addParticipant()
                .setRole(participantRole)
                .setType(ActivityDefinition.ActivityParticipantType.PRACTITIONER);

        RelatedArtifact relatedArtifact = new RelatedArtifact();
        relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DOCUMENTATION);
        relatedArtifact.setDisplay("To-Do List");
        activityDefinition.setRelatedArtifact(Collections.singletonList(relatedArtifact));

        return activityDefinition;
    }

    public static Task createToDoTask(String patientId, String practitionerId, String organizationId, IGenericClient fhirClient, FisProperties fisProperties) {
        Task task = new Task();

        task.setStatus(Task.TaskStatus.READY);
        task.setPriority(Task.TaskPriority.ASAP);
        task.setIntent(Task.TaskIntent.PROPOSAL);

        //Start and end date
        task.getExecutionPeriod().setStart(java.sql.Date.valueOf(LocalDate.now()));
        task.getExecutionPeriod().setEnd(java.sql.Date.valueOf(LocalDate.now().plusYears(fisProperties.getDefaultEndPeriod())));

        //Performer Type
        CodeableConcept performerType = new CodeableConcept();
        performerType.addCoding().setDisplay(TaskPerformerType.REQUESTER.getDisplay())
                .setCode(TaskPerformerType.REQUESTER.toCode())
                .setSystem(TaskPerformerType.REQUESTER.getSystem());
        task.setPerformerType(Collections.singletonList(performerType));

        Reference patient = new Reference();
        patient.setReference("Patient/" + patientId);
        task.setFor(patient);
        task.setDescription(ActivityDefinitionConstants.TO_DO);

        Reference reference = new Reference();
        reference.setReference("Practitioner/" + practitionerId);

        Task.TaskRequesterComponent taskRequesterComponent = new Task.TaskRequesterComponent();
        Reference organizationReference = new Reference();
        organizationReference.setReference("Organization/" + organizationId);
        taskRequesterComponent.setAgent(reference).setOnBehalfOf(organizationReference);

        task.setRequester(taskRequesterComponent);

        return task;
    }

    public static ReferenceDto getRelatedActivityDefinition(String organizationId, String definitionDisplay, IGenericClient fhirClient, FisProperties fisProperties) {
        Bundle bundle = fhirClient.search().forResource(ActivityDefinition.class)
                .where(new StringClientParam("publisher").matches().value("Organization/" + organizationId))
                .where(new StringClientParam("description").matches().value(definitionDisplay))
                .returnBundle(Bundle.class).execute();
        ReferenceDto referenceDto = new ReferenceDto();
        bundle.getEntry().stream().findAny().ifPresent(ad -> {
            ActivityDefinition activityDefinition = (ActivityDefinition) ad.getResource();
            referenceDto.setDisplay((activityDefinition.hasDescription()) ? activityDefinition.getDescription() : null);
            referenceDto.setReference("ActivityDefinition/" + activityDefinition.getIdElement().getIdPart());
        });
        return referenceDto;
    }

    public static List<AbstractCareTeamDto> getOrganizationActors(Optional<String> patientId, Optional<String> name, Optional<String> organizationId, Optional<List<String>> careTeams, IGenericClient fhirClient, FisProperties fisProperties) {
        Bundle organizationBundle = fhirClient.search().forResource(Organization.class)
                .where(new TokenClientParam("_id").exactly().codes(participantIds(organizationId, patientId, careTeams, fhirClient)))
                .where(new RichStringClientParam("name").matches().value(name.orElse("")))
                .returnBundle(Bundle.class)
                .elementsSubset("id", "resourceType", "name", "identifier", "telecom", "address")
                .execute();
        List<Bundle.BundleEntryComponent> organizationBundleEntryList = FhirOperationUtil.getAllBundleComponentsAsList(organizationBundle, Optional.empty(), fhirClient, fisProperties);

        return organizationBundleEntryList.stream().map(org -> {
            AbstractCareTeamDto abstractCareTeamDto = new AbstractCareTeamDto();
            Organization organization = (Organization) org.getResource();
            abstractCareTeamDto.setId(organization.getIdElement().getIdPart());
            abstractCareTeamDto.setDisplay(organization.getName());
            abstractCareTeamDto.setCareTeamType(AbstractCareTeamDto.CareTeamType.ORGANIZATION);
            List<IdentifierDto> identifierDtos = organization.getIdentifier().stream()
                    .map(FhirResourceUtil::entireCovertIdentifierToIdentifierDto)
                    .collect(Collectors.toList());
            abstractCareTeamDto.setIdentifiers(identifierDtos);

            organization.getAddress().stream().findAny().ifPresent(address -> {
                AddressDto addressDto = convertAddressToAddressDto(address);
                abstractCareTeamDto.setAddress(addressDto);
            });

            if (organization.hasTelecom()) {
                organization.getTelecom().stream()
                        .filter(telecom -> {
                            if (telecom.hasSystem())
                                return telecom.getSystem().getDisplay().equalsIgnoreCase(ContactPointSystem.PHONE.toString());
                            return false;
                        })
                        .findAny().ifPresent(phone -> abstractCareTeamDto.setPhoneNumber(Optional.ofNullable(phone.getValue())));

                organization.getTelecom().stream().filter(telecom -> {
                    if (telecom.hasSystem())
                        return telecom.getSystem().getDisplay().equalsIgnoreCase(ContactPointSystem.EMAIL.toString());
                    return false;
                })
                        .findAny().ifPresent(email -> abstractCareTeamDto.setEmail(Optional.ofNullable(email.getValue())));
            }
            return abstractCareTeamDto;
        }).distinct().collect(Collectors.toList());
    }

    public static List<AbstractCareTeamDto> getPractitionerActors(Optional<String> patientId, Optional<String> name, Optional<String> practitionerId, Optional<List<String>> careTeams, IGenericClient fhirClient, FisProperties fisProperties) {
        Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().codes(participantIds(practitionerId, patientId, careTeams, fhirClient)))
                .where(new RichStringClientParam("name").matches().value(name.orElse("")))
                .returnBundle(Bundle.class)
                .elementsSubset("id", "resourceType", "name", "identifier", "telecom", "address")
                .execute();

        List<Bundle.BundleEntryComponent> practitionerBundleEntryList = FhirOperationUtil.getAllBundleComponentsAsList(practitionerBundle, Optional.empty(), fhirClient, fisProperties);

        return practitionerBundleEntryList.stream().map(pr -> {
            AbstractCareTeamDto abstractCareTeamDto = new AbstractCareTeamDto();
            Practitioner practitioner = (Practitioner) pr.getResource();
            abstractCareTeamDto.setId(practitioner.getIdElement().getIdPart());
            practitioner.getName().stream().findAny().ifPresent(humanName -> abstractCareTeamDto.setDisplay(humanName.getGiven().stream().findAny().get() + " " + humanName.getFamily()));

            abstractCareTeamDto.setCareTeamType(AbstractCareTeamDto.CareTeamType.PRACTITIONER);
            List<IdentifierDto> identifierDtos = practitioner.getIdentifier().stream()
                    .map(FhirResourceUtil::entireCovertIdentifierToIdentifierDto)
                    .collect(Collectors.toList());
            abstractCareTeamDto.setIdentifiers(identifierDtos);

            practitioner.getAddress().stream().findAny().ifPresent(address -> {
                        AddressDto addressDto = convertAddressToAddressDto(address);
                        abstractCareTeamDto.setAddress(addressDto);
                    }
            );

            if (practitioner.hasTelecom()) {
                practitioner.getTelecom().stream()
                        .filter(telecom -> {
                            if (telecom.hasSystem())
                                return telecom.getSystem().getDisplay().equalsIgnoreCase(ContactPointSystem.PHONE.toString());
                            return false;
                        })
                        .findAny().ifPresent(phone -> abstractCareTeamDto.setPhoneNumber(Optional.ofNullable(phone.getValue())));

                practitioner.getTelecom().stream().filter(telecom -> {
                    if (telecom.hasSystem())
                        return telecom.getSystem().getDisplay().equalsIgnoreCase(ContactPointSystem.EMAIL.toString());
                    return false;
                })
                        .findAny().ifPresent(email -> abstractCareTeamDto.setEmail(Optional.ofNullable(email.getValue())));
            }
            return abstractCareTeamDto;

        }).distinct().collect(Collectors.toList());
    }

    public static List<AbstractCareTeamDto> getRelatedPersonActors(Optional<String> patientId, Optional<String> name, Optional<String> relatedPersonId, Optional<List<String>> careTeams, IGenericClient fhirClient, FisProperties fisProperties) {

        Bundle relatedBundle = fhirClient.search().forResource(RelatedPerson.class)
                .where(new TokenClientParam("_id").exactly().codes(participantIds(relatedPersonId, patientId, careTeams, fhirClient)))
                .where(new RichStringClientParam("name").matches().value(name.orElse("")))
                .returnBundle(Bundle.class)
                .elementsSubset("id", "resourceType", "name", "identifier", "telecom", "address")
                .execute();

        List<Bundle.BundleEntryComponent> relatedPersonBundleEntryList = FhirOperationUtil.getAllBundleComponentsAsList(relatedBundle, Optional.empty(), fhirClient, fisProperties);

        return relatedPersonBundleEntryList.stream().map(rp -> {
            AbstractCareTeamDto abstractCareTeamDto = new AbstractCareTeamDto();
            RelatedPerson relatedPerson = (RelatedPerson) rp.getResource();
            abstractCareTeamDto.setId(relatedPerson.getIdElement().getIdPart());

            abstractCareTeamDto.setCareTeamType(AbstractCareTeamDto.CareTeamType.RELATEDPERSON);
            relatedPerson.getName().stream().findAny().ifPresent(humanName -> abstractCareTeamDto.setDisplay(humanName.getGiven().stream().findAny().get() + " " + humanName.getFamily()));

            List<IdentifierDto> identifierDtos = relatedPerson.getIdentifier().stream()
                    .map(FhirResourceUtil::entireCovertIdentifierToIdentifierDto)
                    .collect(Collectors.toList());
            abstractCareTeamDto.setIdentifiers(identifierDtos);

            relatedPerson.getAddress().stream().findAny().ifPresent(address -> {
                        AddressDto addressDto = convertAddressToAddressDto(address);
                        abstractCareTeamDto.setAddress(addressDto);
                    }
            );

            if (relatedPerson.hasTelecom()) {
                relatedPerson.getTelecom().stream()
                        .filter(telecom -> {
                            if (telecom.hasSystem())
                                return telecom.getSystem().getDisplay().equalsIgnoreCase(ContactPointSystem.PHONE.toString());
                            return false;
                        })
                        .findAny().ifPresent(phone -> abstractCareTeamDto.setPhoneNumber(Optional.ofNullable(phone.getValue())));

                relatedPerson.getTelecom().stream().filter(telecom -> {
                    if (telecom.hasSystem())
                        return telecom.getSystem().getDisplay().equalsIgnoreCase(ContactPointSystem.EMAIL.toString());
                    return false;
                })
                        .findAny().ifPresent(email -> abstractCareTeamDto.setEmail(Optional.ofNullable(email.getValue())));
            }
            return abstractCareTeamDto;
        }).distinct().collect(Collectors.toList());
    }

    public static List<String> getCareTeamParticipantIdsFromPatient(Optional<String> patientId, IGenericClient fhirClient) {
        List<String> participantIds = new ArrayList<>();
        if (patientId.isPresent()) {
            Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class).where(new ReferenceClientParam("patient").hasId(patientId.get()))
                    .returnBundle(Bundle.class)
                    .elementsSubset("participant")
                    .execute();
            participantIds = careTeamBundle.getEntry().stream().flatMap(careTeam -> {
                CareTeam ct = (CareTeam) careTeam.getResource();
                return ct.getParticipant().stream().map(par -> {
                    String references = par.getMember().getReference().split("/")[1];
                    return references;
                });
            }).collect(Collectors.toList());
        }
        return participantIds;
    }

    public static AddressDto convertAddressToAddressDto(Address address) {
        AddressDto addressDto = new AddressDto();
        if (address.hasLine()) {
            if (address.hasLine()) {
                address.getLine().stream().findAny().ifPresent(line -> addressDto.setLine1(line.getValue()));
            }
        }

        if (address.hasCity())
            addressDto.setCity(address.getCity());
        if (address.hasCountry())
            addressDto.setCountryCode(address.getCountry());
        if (address.hasPostalCode())
            addressDto.setPostalCode(address.getPostalCode());
        if (address.hasState())
            addressDto.setStateCode(address.getState());
        return addressDto;
    }

    public static IdentifierDto entireCovertIdentifierToIdentifierDto(Identifier identifier) {
        IdentifierDto identifierDto = new IdentifierDto();

        if (identifier != null) {
            String systemOid = identifier.getSystem() != null ? identifier.getSystem() : "";
            String systemDisplay;

            try {
                if (systemOid.startsWith(IdentifierConstants.URN_OID_TEXT) || systemOid.startsWith(IdentifierConstants.HTTP_TEXT)) {
                    systemDisplay = KnownIdentifierSystemEnum.fromUri(systemOid).getDisplay();
                } else if (systemOid.startsWith(IdentifierConstants.OID_NUMBER_STARTING_WITH)) {
                    systemDisplay = KnownIdentifierSystemEnum.fromOid(systemOid).getDisplay();
                } else
                    systemDisplay = systemOid;
            } catch (IdentifierSystemNotFoundException e) {
                systemDisplay = systemOid;
            }

            identifierDto = IdentifierDto.builder()
                    .system(systemOid)
                    .oid(systemOid.startsWith(IdentifierConstants.URN_OID_TEXT)
                            ? systemOid.replace(IdentifierConstants.URN_OID_TEXT, "")
                            : "")
                    .systemDisplay(systemDisplay)
                    .value(identifier.getValue())
                    .display(identifier.getValue())
                    .build();
        }
        return identifierDto;
    }

    public static List<String> participantIds(Optional<String> participantId, Optional<String> patientId, Optional<List<String>> careTeams, IGenericClient fhirClient) {
        List<String> participantIds;
        if (participantId.isPresent()) {
            participantIds = Collections.singletonList(participantId.get());
        } else if (careTeams.isPresent()) {
            participantIds = getParticipantIdFromCareTeam(careTeams, fhirClient);
        } else {
            participantIds = getCareTeamParticipantIdsFromPatient(patientId, fhirClient);
        }
        return participantIds;
    }

    public static List<String> getParticipantIdFromCareTeam(Optional<List<String>> careTeams, IGenericClient fhirClient) {
        Bundle bundle = fhirClient.search().forResource(CareTeam.class).where(new TokenClientParam("_id").exactly().codes(careTeams.get())).returnBundle(Bundle.class).execute();

        return bundle.getEntry().stream().map(ct -> (CareTeam) ct.getResource()).flatMap(ct -> ct.getParticipant().stream().map(par -> par.getMember().getReference().split("/")[1])).distinct().collect(Collectors.toList());

    }
}

