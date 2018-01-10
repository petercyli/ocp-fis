package gov.samhsa.ocp.ocpfis.ocp.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import gov.samhsa.ocp.ocpfis.service.PractitionerServiceImpl;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.exception.PractitionerNotFoundException;
import javafx.scene.chart.BubbleChart;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;

import java.awt.color.ICC_ColorSpace;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PractitionerServiceImplTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private IGenericClient fhirClient;

    @InjectMocks
    private PractitionerServiceImpl practitionerService;

    @Test
    public void testGetAllPractitioners() {
        //Arrange
        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery1 = mock(IQuery.class);
        when(iUntypedQuery.forResource(Practitioner.class)).thenReturn(iQuery1);
        IQuery iQuery2 = mock(IQuery.class);
        when(iQuery1.returnBundle(Bundle.class)).thenReturn(iQuery2);
        Bundle bundle = mock(Bundle.class);
        when(iQuery2.execute()).thenReturn(bundle);
        Bundle.BundleEntryComponent bundleEntryComponent = mock(Bundle.BundleEntryComponent.class);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();
        bundleEntryComponents.add(bundleEntryComponent);

        when(bundle.getEntry()).thenReturn(bundleEntryComponents);

        PractitionerDto practitionerDto = mock(PractitionerDto.class);
        List<PractitionerDto> practitionerDtos = new ArrayList<>();
        practitionerDtos.add(practitionerDto);
        when(modelMapper.map(bundleEntryComponent.getResource(), PractitionerDto.class)).thenReturn(practitionerDto);

        //Act
        List<PractitionerDto> practitionerDtoList = practitionerService.getAllPractitioners();

        //Assert
        assertEquals(practitionerDtos, practitionerDtoList);
    }

    @Test
    public void testGetAllPractitioners_Given_NoPractitioner_Then_ThrowsException() {
        //Arrange
        thrown.expect(PractitionerNotFoundException.class);
        thrown.expectMessage("No practitioners were found in the FHIR server");
        IUntypedQuery iUntypedQuery = mock(IUntypedQuery.class);
        when(fhirClient.search()).thenReturn(iUntypedQuery);
        IQuery iQuery1 = mock(IQuery.class);
        when(iUntypedQuery.forResource(Practitioner.class)).thenReturn(iQuery1);
        IQuery iQuery2 = mock(IQuery.class);
        when(iQuery1.returnBundle(Bundle.class)).thenReturn(iQuery2);
        Bundle bundle = mock(Bundle.class);
        when(iQuery2.execute()).thenReturn(bundle);
        List<Bundle.BundleEntryComponent> bundleEntryComponents = new ArrayList<>();

        when(bundle.getEntry()).thenReturn(bundleEntryComponents);

        //Act
        practitionerService.getAllPractitioners();

        //Assert
        //ExpectedException annotated by @rule is thrown;
    }

}
