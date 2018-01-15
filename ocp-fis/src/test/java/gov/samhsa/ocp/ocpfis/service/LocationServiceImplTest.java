package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.exception.LocationNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class LocationServiceImplTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private IGenericClient fhirClient;
    @Mock
    private FisProperties fisProperties;

    @InjectMocks
    private LocationServiceImpl locationServiceImpl;

    @Test
    public void testGetAllLocations() {
        //Arrange
        int size = 10;

        FisProperties.Location location = mock(FisProperties.Location.class);
        when(fisProperties.getLocation()).thenReturn(location);
        FisProperties.Location.Pagination pagination = mock(FisProperties.Location.Pagination.class);
        when(location.getPagination()).thenReturn(pagination);
        when(pagination.getDefaultSize()).thenReturn(20);

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Location.class)).thenReturn(iQuery);
        IQuery count = mock(IQuery.class);
        when(iQuery.count(20)).thenReturn(count);
        IQuery bundle = mock(IQuery.class);
        when(count.returnBundle(Bundle.class)).thenReturn(bundle);
        Bundle locationSearchBundle = mock(Bundle.class);
        when(bundle.execute()).thenReturn(locationSearchBundle);
        when(locationSearchBundle.getTotal()).thenReturn(1);

        Bundle.BundleEntryComponent bundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        bundleEntryComponents.add(bundleEntryComponent);
        when(locationSearchBundle.getEntry()).thenReturn(bundleEntryComponents);

        Resource resource = mock(Resource.class);
        when(bundleEntryComponent.getResource()).thenReturn(resource);
        LocationDto locationDto = mock(LocationDto.class);
        List<LocationDto> locationDtos = new ArrayList<>();
        locationDtos.add(locationDto);
        when(modelMapper.map(resource, LocationDto.class)).thenReturn(locationDto);
        //Act
       // List<LocationDto> locationDtos1 = locationServiceImpl.getAllLocations(Optional.empty(), Optional.empty(), Optional.of(size));

        //Assert
       // assertEquals(locationDtos, locationDtos1);
    }

    @Test
    public void testGetAllLocations_No_Locations_Found_Then_ThrowsException() {
        //Arrange
        int size = 10;

        FisProperties.Location location = mock(FisProperties.Location.class);
        when(fisProperties.getLocation()).thenReturn(location);
        FisProperties.Location.Pagination pagination = mock(FisProperties.Location.Pagination.class);
        when(location.getPagination()).thenReturn(pagination);
        when(pagination.getDefaultSize()).thenReturn(20);

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Location.class)).thenReturn(iQuery);
        IQuery count = mock(IQuery.class);
        when(iQuery.count(20)).thenReturn(count);
        IQuery bundle = mock(IQuery.class);
        when(count.returnBundle(Bundle.class)).thenReturn(bundle);
        Bundle locationSearchBundle = mock(Bundle.class);
        when(bundle.execute()).thenReturn(locationSearchBundle);
        when(locationSearchBundle.getTotal()).thenReturn(0);

        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        when(locationSearchBundle.getEntry()).thenReturn(bundleEntryComponents);

        thrown.expect(LocationNotFoundException.class);
        thrown.expectMessage("No locations were found in the FHIR server");

        //Act
        //locationServiceImpl.getAllLocations(Optional.empty(), Optional.empty(), Optional.of(size));
    }

    @Test
    public void getLocationsByOrganization() {
        //Arrange
        int size = 10;
        String organizationResourceId = "123";

        FisProperties.Location location = mock(FisProperties.Location.class);
        when(fisProperties.getLocation()).thenReturn(location);
        FisProperties.Location.Pagination pagination = mock(FisProperties.Location.Pagination.class);
        when(location.getPagination()).thenReturn(pagination);
        when(pagination.getDefaultSize()).thenReturn(size);

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Location.class)).thenReturn(iQuery);

        ReferenceClientParam referenceClientParam = mock(ReferenceClientParam.class);
        ICriterion criteria = mock(ICriterion.class);
        when(referenceClientParam.hasId(organizationResourceId)).thenReturn(criteria);
        IQuery iQuery1 = mock(IQuery.class);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery1);

        IQuery count = mock(IQuery.class);
        when(iQuery1.count(size)).thenReturn(count);
        IQuery bundle = mock(IQuery.class);
        when(count.returnBundle(Bundle.class)).thenReturn(bundle);
        Bundle locationSearchBundle = mock(Bundle.class);
        when(bundle.execute()).thenReturn(locationSearchBundle);
        when(locationSearchBundle.getTotal()).thenReturn(1);

        Bundle.BundleEntryComponent bundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        bundleEntryComponents.add(bundleEntryComponent);
        when(locationSearchBundle.getEntry()).thenReturn(bundleEntryComponents);

        Resource resource = mock(Resource.class);
        when(bundleEntryComponent.getResource()).thenReturn(resource);
        LocationDto locationDto = mock(LocationDto.class);
        List<LocationDto> locationDtos = new ArrayList<>();
        locationDtos.add(locationDto);
        when(modelMapper.map(resource, LocationDto.class)).thenReturn(locationDto);

        //Act
        //List<LocationDto> locationDtos1 = locationServiceImpl.getLocationsByOrganization(organizationResourceId, Optional.empty(), Optional.empty(), Optional.of(size));

        //Assert
        //assertEquals(locationDtos, locationDtos1);
    }

    @Test
    public void getLocationsByOrganization_No_Locations_Found_Then_ThrowsException() {
        //Arrange
        int size = 10;
        String organizationResourceId = "123";

        FisProperties.Location location = mock(FisProperties.Location.class);
        when(fisProperties.getLocation()).thenReturn(location);
        FisProperties.Location.Pagination pagination = mock(FisProperties.Location.Pagination.class);
        when(location.getPagination()).thenReturn(pagination);
        when(pagination.getDefaultSize()).thenReturn(size);

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Location.class)).thenReturn(iQuery);

        ReferenceClientParam referenceClientParam = mock(ReferenceClientParam.class);
        ICriterion criteria = mock(ICriterion.class);
        when(referenceClientParam.hasId(organizationResourceId)).thenReturn(criteria);
        IQuery iQuery1 = mock(IQuery.class);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery1);

        IQuery count = mock(IQuery.class);
        when(iQuery1.count(size)).thenReturn(count);
        IQuery bundle = mock(IQuery.class);
        when(count.returnBundle(Bundle.class)).thenReturn(bundle);
        Bundle locationSearchBundle = mock(Bundle.class);
        when(bundle.execute()).thenReturn(locationSearchBundle);

        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        when(locationSearchBundle.getEntry()).thenReturn(bundleEntryComponents);

        thrown.expect(LocationNotFoundException.class);
        thrown.expectMessage("No location found for the given OrganizationID:" + organizationResourceId);

        //Act
        //locationServiceImpl.getLocationsByOrganization(organizationResourceId, Optional.empty(), Optional.empty(), Optional.of(size));
    }

    @Test
    public void getLocation() {
        //Arrange
        String locationId = "123";

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Location.class)).thenReturn(iQuery);

        IQuery iQuery1 = mock(IQuery.class);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery1);

        IQuery bundle = mock(IQuery.class);
        when(iQuery1.returnBundle(Bundle.class)).thenReturn(bundle);
        Bundle locationSearchBundle = mock(Bundle.class);
        when(bundle.execute()).thenReturn(locationSearchBundle);
        when(locationSearchBundle.getTotal()).thenReturn(1);

        Bundle.BundleEntryComponent bundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        bundleEntryComponents.add(bundleEntryComponent);
        when(locationSearchBundle.getEntry()).thenReturn(bundleEntryComponents);

        Resource resource = mock(Resource.class);
        when(bundleEntryComponent.getResource()).thenReturn(resource);
        LocationDto locationDto = mock(LocationDto.class);
        when(modelMapper.map(resource, LocationDto.class)).thenReturn(locationDto);

        //Act
        LocationDto locationDto1 = locationServiceImpl.getLocation(locationId);

        //Assert
        assertEquals(locationDto, locationDto1);
    }

    @Test
    public void getLocation_No_Location_Found_Then_ThrowsException() {

        //Arrange
        String locationId = "123";

        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery = mock(IQuery.class);
        when(iUntypedQuery.forResource(Location.class)).thenReturn(iQuery);

        IQuery iQuery1 = mock(IQuery.class);
        when(iQuery.where(any(ICriterion.class))).thenReturn(iQuery1);

        IQuery bundle = mock(IQuery.class);
        when(iQuery1.returnBundle(Bundle.class)).thenReturn(bundle);
        Bundle locationSearchBundle = mock(Bundle.class);
        when(bundle.execute()).thenReturn(locationSearchBundle);
        when(locationSearchBundle.getTotal()).thenReturn(1);

        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        when(locationSearchBundle.getEntry()).thenReturn(bundleEntryComponents);

        thrown.expect(LocationNotFoundException.class);
        thrown.expectMessage("No location was found for the given LocationID:" + locationId);

        //Act
        locationServiceImpl.getLocation(locationId);
    }

}
