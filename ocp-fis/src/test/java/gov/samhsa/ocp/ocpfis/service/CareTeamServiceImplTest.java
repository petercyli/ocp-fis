package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PaginationUtil.class, IdType.class})
public class CareTeamServiceImplTest {
    public static final String STATUS_ACTIVE = "active";

    @Mock
    private IGenericClient fhirClient;

    @Mock
    private FhirValidator fhirValidator;

    @Mock
    private FisProperties fisProperties;

    @Mock
    private LookUpService lookUpService;

    @InjectMocks
    public CareTeamServiceImpl careTeamService;

    @Ignore
    @Test
    public void testGetCareTeams() {
        //Arrange
        final List<String> statusList = new ArrayList<>();
        final String status = "active";
        statusList.add(status);

        final String searchType = "patientId";
        final String searchValue = "123";
        final int page = 1;
        final int size = 1;

        final String careTeamId = "345";
        final String careTeamName = "name";
        final String statusCode = "active";
        final String statusDisplay = "Active";

        final String categoryCode = "category";
        final String categoryDisplay = "Category";
        final String subjectRef = "Patient/102";
        final String subId = "102";

        final String subFamilyName = "familySubject";
        final String subGivenName = "givenSubject";

        final String reasonCode = "reason";
        final String reasonDisplay = "Reason";

        final String roleCode = "role";
        final String roleDisplay = "Role";

        final String participantRef = "RelatedPerson/234";
        final String participantId = "234";

        final String participantGiven = "participantGiven";
        final String participantFamily = "participantFamily";

        PowerMockito.mockStatic(PaginationUtil.class);
        when(PaginationUtil.getValidPageSize(fisProperties, Optional.ofNullable(size), ResourceType.CareTeam.name())).thenReturn(size);

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);

        IQuery careTeamQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(CareTeam.class)).thenReturn(careTeamQuery);
        IQuery careTeamWithParticipantQuery = mock(IQuery.class);
        when(careTeamQuery.include(CareTeam.INCLUDE_PARTICIPANT)).thenReturn(careTeamWithParticipantQuery);
        IQuery careTeamWithParticipantAndSubjectQuery = mock(IQuery.class);
        when(careTeamWithParticipantQuery.include(CareTeam.INCLUDE_SUBJECT)).thenReturn(careTeamWithParticipantAndSubjectQuery);
        IQuery careTeamWithSizeQuery = mock(IQuery.class);
        when(careTeamWithParticipantAndSubjectQuery.count(size)).thenReturn(careTeamWithSizeQuery);
        IQuery careTeamBundleQuery = mock(IQuery.class);
        when(careTeamWithSizeQuery.returnBundle(Bundle.class)).thenReturn(careTeamBundleQuery);

        Bundle careTeamBundle = mock(Bundle.class);
        when(careTeamBundleQuery.execute()).thenReturn(careTeamBundle);

        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        Bundle.BundleEntryComponent careTeamBundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        Bundle.BundleEntryComponent subjectBundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        Bundle.BundleEntryComponent participantBundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        bundleEntryComponents.add(careTeamBundleEntryComponent);
        bundleEntryComponents.add(subjectBundleEntryComponent);
        bundleEntryComponents.add(participantBundleEntryComponent);
        when(careTeamBundle.getEntry()).thenReturn(bundleEntryComponents);

        CareTeam careTeamResource = mock(CareTeam.class);
        when(careTeamBundleEntryComponent.getResource()).thenReturn(careTeamResource);
        when(careTeamResource.getResourceType()).thenReturn(ResourceType.CareTeam);
        IdType idType = PowerMockito.mock(IdType.class);
        Mockito.when(careTeamResource.getIdElement()).thenReturn(idType);
        PowerMockito.when(idType.getIdPart()).thenReturn(careTeamId);

        when(careTeamResource.getName()).thenReturn(careTeamName);
        when(careTeamResource.getStatus()).thenReturn(CareTeam.CareTeamStatus.ACTIVE);

        LookUpService lookUpService = mock(LookUpService.class);
        List<ValueSetDto> valueSetDtos = new ArrayList<>();
        ValueSetDto valueSetDto = mock(ValueSetDto.class);
        valueSetDtos.add(valueSetDto);
        when(lookUpService.getCareTeamStatuses()).thenReturn(valueSetDtos);
        when(valueSetDto.getCode()).thenReturn(statusCode);
        when(valueSetDto.getDisplay()).thenReturn(statusDisplay);

        List<CodeableConcept> categories = new ArrayList<>();
        CodeableConcept category = mock(CodeableConcept.class);
        categories.add(category);
        when(careTeamResource.getCategory()).thenReturn(categories);
        List<Coding> codings = new ArrayList<>();
        Coding coding = mock(Coding.class);
        codings.add(coding);
        when(category.getCoding()).thenReturn(codings);
        when(coding.getCode()).thenReturn(categoryCode);
        List<ValueSetDto> categoryValueSets = new ArrayList<>();
        ValueSetDto categoryValueSet = mock(ValueSetDto.class);
        categoryValueSets.add(categoryValueSet);
        when(lookUpService.getCareTeamCategories()).thenReturn(categoryValueSets);
        when(categoryValueSet.getCode()).thenReturn(categoryCode);
        when(categoryValueSet.getDisplay()).thenReturn(categoryDisplay);
        Reference referenceSubject = mock(Reference.class);
        when(careTeamResource.getSubject()).thenReturn(referenceSubject);
        when(referenceSubject.getReference()).thenReturn(subjectRef);
        Patient patient = mock(Patient.class);
        when(subjectBundleEntryComponent.getResource()).thenReturn(patient);
        when(patient.getResourceType()).thenReturn(ResourceType.Patient);
        IdType subjectIdType = PowerMockito.mock(IdType.class);
        when(patient.getIdElement()).thenReturn(subjectIdType);
        PowerMockito.when(subjectIdType.getIdPart()).thenReturn(subId);

        List<HumanName> names = new ArrayList<>();
        HumanName humanName = mock(HumanName.class);
        names.add(humanName);
        when(patient.getName()).thenReturn(names);
        when(humanName.getFamily()).thenReturn(subFamilyName);
        List<StringType> givenNames = new ArrayList<>();
        StringType givenName = mock(StringType.class);
        givenNames.add(givenName);
        when(humanName.getGiven()).thenReturn(givenNames);
        when(givenName.toString()).thenReturn(subGivenName);

        List<CodeableConcept> reasonCodes = new ArrayList<>();
        CodeableConcept reasonCodeable = mock(CodeableConcept.class);
        reasonCodes.add(reasonCodeable);
        when(careTeamResource.getReasonCode()).thenReturn(reasonCodes);
        List<Coding> reasonCodings = new ArrayList<>();
        Coding reasonCoding = mock(Coding.class);
        reasonCodings.add(reasonCoding);
        when(reasonCodeable.getCoding()).thenReturn(reasonCodings);
        when(reasonCoding.getCode()).thenReturn(reasonCode);
        ValueSetDto reasonValueSetDto = mock(ValueSetDto.class);
        List<ValueSetDto> reasonValueSetDtos = new ArrayList<>();
        reasonValueSetDtos.add(reasonValueSetDto);
        when(lookUpService.getCareTeamReasons()).thenReturn(reasonValueSetDtos);
        when(reasonValueSetDto.getCode()).thenReturn(reasonCode);
        when(reasonValueSetDto.getDisplay()).thenReturn(reasonDisplay);

        List<CareTeam.CareTeamParticipantComponent> careTeamParticipantComponents = new ArrayList<>();
        CareTeam.CareTeamParticipantComponent careTeamParticipantComponent = mock(CareTeam.CareTeamParticipantComponent.class);
        careTeamParticipantComponents.add(careTeamParticipantComponent);
        when(careTeamResource.getParticipant()).thenReturn(careTeamParticipantComponents);
        CodeableConcept roleConcept = mock(CodeableConcept.class);
        when(careTeamParticipantComponent.getRole()).thenReturn(roleConcept);

        List<Coding> roleCodings = new ArrayList<>();
        Coding roleCoding = mock(Coding.class);
        roleCodings.add(roleCoding);
        when(roleConcept.getCoding()).thenReturn(roleCodings);
        when(roleCoding.getCode()).thenReturn(roleCode);

        List<ValueSetDto> roleValueSetDtos = new ArrayList<>();
        ValueSetDto roleValueSetDto = mock(ValueSetDto.class);
        roleValueSetDtos.add(roleValueSetDto);
        when(lookUpService.getParticipantRoles()).thenReturn(roleValueSetDtos);
        when(roleValueSetDto.getCode()).thenReturn(roleCode);
        when(roleValueSetDto.getDisplay()).thenReturn(roleDisplay);

        Period period = mock(Period.class);
        when(careTeamParticipantComponent.getPeriod()).thenReturn(period);
        Date startDate = Date.from(Instant.now());
        when(period.getStart()).thenReturn(startDate);
        Date endDate = Date.from(Instant.now());
        when(period.getEnd()).thenReturn(endDate);

        Reference participantReference = mock(Reference.class);
        when(careTeamParticipantComponent.getMember()).thenReturn(participantReference);
        when(participantReference.getReference()).thenReturn(participantRef);
        RelatedPerson relatedPerson = mock(RelatedPerson.class);
        when(participantBundleEntryComponent.getResource()).thenReturn(relatedPerson);
        when(relatedPerson.getResourceType()).thenReturn(ResourceType.RelatedPerson);
        IdType participantIdType = PowerMockito.mock(IdType.class);
        when(relatedPerson.getIdElement()).thenReturn(participantIdType);
        PowerMockito.when(participantIdType.getIdPart()).thenReturn(participantId);
        List<HumanName> participantNames = new ArrayList<>();
        HumanName participantHumanName = mock(HumanName.class);
        participantNames.add(participantHumanName);
        when(relatedPerson.getName()).thenReturn(participantNames);

        List<StringType> participantGivenNames = new ArrayList<>();
        StringType participantGivenName = mock(StringType.class);
        participantGivenNames.add(participantGivenName);
        when(participantHumanName.getGiven()).thenReturn(participantGivenNames);
        when(participantGivenName.toString()).thenReturn(participantGiven);

        when(participantHumanName.getFamily()).thenReturn(participantFamily);

        //Act
        PageDto<CareTeamDto> careTeams = careTeamService.getCareTeams(Optional.ofNullable(statusList), searchType, searchValue, Optional.ofNullable(page), Optional.ofNullable(size));

        //Assert
        assertEquals(1, careTeams.getElements().size());
        assertEquals(1, careTeams.getCurrentPageSize());
        verify(relatedPerson).fhirType();
    }
}
