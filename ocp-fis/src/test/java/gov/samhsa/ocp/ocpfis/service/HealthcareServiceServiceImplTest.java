package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.HealthcareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameLogicalIdIdentifiersDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.Test;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PaginationUtil.class, IdType.class})
public class HealthcareServiceServiceImplTest {

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private IGenericClient fhirClient;

    @Mock
    private FhirValidator fhirValidator;

    @Mock
    private FisProperties fisProperties;

    @InjectMocks
    private HealthcareServiceServiceImpl healthcareServiceService;

    @Test
    public void testGetAllHealthcareServicesByLocation() {
        //Arrange
        final List<String> statusList = new ArrayList<>();
        final String status = "active";
        statusList.add(status);

        final String organizationResourceId = "1";
        final String locationId = "2";
        final String searchKey = "name";
        final String searchValue = "value";
        final int page = 1;
        final int size = 5;
        final String hcsId = "234";
        final String locationReference = "Location/2";
        final String locationName = "locationName";
        final int totalBundle = 1;

        PowerMockito.mockStatic(PaginationUtil.class);
        PowerMockito.when(PaginationUtil.getValidPageSize(fisProperties, java.util.Optional.ofNullable(size), org.hl7.fhir.dstu3.model.ResourceType.HealthcareService.name())).thenReturn(size);

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);

        IQuery healthcareQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(HealthcareService.class)).thenReturn(healthcareQuery);
        IQuery healthcareWithOrganizationIdQuery = mock(IQuery.class);
        when(healthcareQuery.where(any(ICriterion.class))).thenReturn(healthcareWithOrganizationIdQuery);
        IQuery healthcareWithOrgAndLocationId = mock(IQuery.class);
        when(healthcareWithOrganizationIdQuery.where(any(ICriterion.class))).thenReturn(healthcareWithOrgAndLocationId);

        IQuery healthcareWithPageNumberQuery = mock(IQuery.class);
        when(healthcareWithOrgAndLocationId.count(size)).thenReturn(healthcareWithPageNumberQuery);
        IQuery healthcareWithLocationQuery = mock(IQuery.class);
        when(healthcareWithPageNumberQuery.include(HealthcareService.INCLUDE_LOCATION)).thenReturn(healthcareWithLocationQuery);
        IQuery healthcareBundleQuery = mock(IQuery.class);
        when(healthcareWithLocationQuery.returnBundle(Bundle.class)).thenReturn(healthcareBundleQuery);
        IClientExecutable iClientExecutable = mock(IClientExecutable.class);
        when(healthcareBundleQuery.encodedJson()).thenReturn(iClientExecutable);
        Bundle healthcareBundle = mock(Bundle.class);
        when(iClientExecutable.execute()).thenReturn(healthcareBundle);

        Bundle.BundleEntryComponent hcsbundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        Bundle.BundleEntryComponent locationEntryComponent = mock(Bundle.BundleEntryComponent.class);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        bundleEntryComponents.add(hcsbundleEntryComponent);
        bundleEntryComponents.add(locationEntryComponent);
        when(healthcareBundle.getEntry()).thenReturn(bundleEntryComponents);
        when(healthcareBundle.getTotal()).thenReturn(totalBundle);

        HealthcareService healthcareServiceResource = mock(HealthcareService.class);
        when(hcsbundleEntryComponent.getResource()).thenReturn(healthcareServiceResource);
        when(healthcareServiceResource.getResourceType()).thenReturn(org.hl7.fhir.dstu3.model.ResourceType.HealthcareService);
        List<HealthcareServiceDto> healthcareServiceDtos = new ArrayList<>();
        HealthcareServiceDto healthcareServiceDto = mock(HealthcareServiceDto.class);
        healthcareServiceDtos.add(healthcareServiceDto);

        when(modelMapper.map(healthcareServiceResource, HealthcareServiceDto.class)).thenReturn(healthcareServiceDto);
        IdType idType = PowerMockito.mock(IdType.class);
        when(healthcareServiceResource.getIdElement()).thenReturn(idType);
        PowerMockito.when(idType.getIdPart()).thenReturn(hcsId);

        Reference location = mock(Reference.class);
        List<Reference> locations = new ArrayList<>();
        locations.add(location);
        when(healthcareServiceResource.getLocation()).thenReturn(locations);
        when(location.getReference()).thenReturn(locationReference);

        Location locationPresent = mock(Location.class);
        when(locationEntryComponent.getResource()).thenReturn(locationPresent);
        when(locationPresent.getResourceType()).thenReturn(org.hl7.fhir.dstu3.model.ResourceType.Location);

        NameLogicalIdIdentifiersDto locationDto = mock(NameLogicalIdIdentifiersDto.class);
        when(modelMapper.map(locationPresent, NameLogicalIdIdentifiersDto.class)).thenReturn(locationDto);
        IdType locationIdType = PowerMockito.mock(IdType.class);
        when(locationPresent.getIdElement()).thenReturn(locationIdType);
        PowerMockito.when(locationIdType.getIdPart()).thenReturn(locationId);
        when(locationPresent.getName()).thenReturn(locationName);

        //Act
        PageDto<HealthcareServiceDto> getAllHealthCareServices = healthcareServiceService.getAllHealthcareServicesByLocation(organizationResourceId, locationId, Optional.ofNullable(statusList), Optional.ofNullable(searchKey), Optional.ofNullable(searchValue), Optional.ofNullable(page), Optional.ofNullable(size));

        //Assert
        assertEquals(healthcareServiceDtos, getAllHealthCareServices.getElements());
        assertEquals(1, getAllHealthCareServices.getTotalNumberOfPages(), 1e-15);
        assertEquals(false, getAllHealthCareServices.isHasNextPage());
    }

}
