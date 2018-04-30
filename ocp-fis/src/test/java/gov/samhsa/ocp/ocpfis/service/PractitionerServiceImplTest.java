package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PaginationUtil.class, IdType.class})
public class PractitionerServiceImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private IGenericClient fhirClient;

    @Mock
    private FhirValidator fhirValidator;

    @Mock
    private FisProperties fisProperties;

    @InjectMocks
    private PractitionerServiceImpl practitionerService;


    @Test
    @Ignore
    public void testGetAllPractitioners() {
        //Arrange
        final String practitioner = "Practitioner";
        final int numberOfPractitionersPerPage = 25;
        final String id = "id";
        final String referenceCode = "Practitioner/" + id;
        final String roleId = "roleId";

        PowerMockito.mockStatic(PaginationUtil.class);
        when(PaginationUtil.getValidPageSize(fisProperties, Optional.empty(), practitioner)).thenReturn(numberOfPractitionersPerPage);
        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Practitioner.class)).thenReturn(iQuery);
        IQuery iQueryWithCount = mock(IQuery.class);
        when(iQuery.count(numberOfPractitionersPerPage)).thenReturn(iQueryWithCount);
        IQuery pracitionerWithRoleQuery = mock(IQuery.class);
        when(iQueryWithCount.revInclude(PractitionerRole.INCLUDE_PRACTITIONER)).thenReturn(pracitionerWithRoleQuery);
        IQuery bundleQuery = mock(IQuery.class);
        when(pracitionerWithRoleQuery.returnBundle(Bundle.class)).thenReturn(bundleQuery);
        Bundle bundle = mock(Bundle.class);
        when(bundleQuery.execute()).thenReturn(bundle);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        Bundle.BundleEntryComponent bundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        Bundle.BundleEntryComponent roleBundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        bundleEntryComponents.add(bundleEntryComponent);
        bundleEntryComponents.add(roleBundleEntryComponent);
        when(bundle.getEntry()).thenReturn(bundleEntryComponents);
        Resource resource = mock(Resource.class);
        when(bundleEntryComponent.getResource()).thenReturn(resource);
        when(resource.getResourceType()).thenReturn(ResourceType.Practitioner);

        PractitionerDto practitionerDto = mock(PractitionerDto.class);
        when(modelMapper.map(resource, PractitionerDto.class)).thenReturn(practitionerDto);

        IdType idType = PowerMockito.mock(IdType.class);
        when(resource.getIdElement()).thenReturn(idType);
        when(idType.getIdPart()).thenReturn(id);
        Resource roleResource = mock(Resource.class);
        PractitionerRole practitionerRole = mock(PractitionerRole.class);
        when(roleBundleEntryComponent.getResource()).thenReturn(roleResource).thenReturn(practitionerRole);
        when(practitionerRole.getResourceType()).thenReturn(ResourceType.PractitionerRole);
        when(roleResource.getResourceType()).thenReturn(ResourceType.PractitionerRole);

        Reference reference = mock(Reference.class);
        when(practitionerRole.getPractitioner()).thenReturn(reference);

        when(reference.getReference()).thenReturn(referenceCode);
        PractitionerRoleDto practitionerRoleDto = mock(PractitionerRoleDto.class);
        when(modelMapper.map(practitionerRole, PractitionerRoleDto.class)).thenReturn(practitionerRoleDto);
        IdType roleIdType = PowerMockito.mock(IdType.class);
        when(practitionerRole.getIdElement()).thenReturn(roleIdType);

        when(roleIdType.getIdPart()).thenReturn(roleId);

        //Act
        PageDto<PractitionerDto> allPractitioners = practitionerService.getAllPractitioners(Optional.of(false), Optional.empty(), Optional.empty());

        //Assert
        assertEquals(false, allPractitioners.isHasNextPage());
        assertEquals(1, allPractitioners.getElements().size());
    }

}
