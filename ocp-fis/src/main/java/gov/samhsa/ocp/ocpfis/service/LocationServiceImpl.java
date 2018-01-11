package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.OcpFisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.exception.LocationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Location;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final OcpFisProperties ocpFisProperties;

    @Autowired
    public LocationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, OcpFisProperties ocpFisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.ocpFisProperties = ocpFisProperties;
    }

    @Override
    public List<LocationDto> getAllLocations(Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size) {

        int numberOfLocationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getLocation().getPagination().getMaxSize()).orElse(ocpFisProperties.getLocation().getPagination().getDefaultSize());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class);

        if (status.isPresent() && status.get().size() > 0) {
            log.info("Searching for ALL locations with the following specific status(es).");
            status.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(status.get()));
        } else {
            log.info("Searching for locations with ALL statuses");
        }
        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = (Bundle) locationsSearchQuery.count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().size() < 1) {
            throw new LocationNotFoundException("No locations were found in the FHIR server");
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");
        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (page.isPresent() && page.get() > 1 && firstPageLocationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            otherPageLocationSearchBundle = getLocationSearchBundleAfterFirstPage(firstPageLocationSearchBundle, page.get(), numberOfLocationsPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedLocations = otherPageLocationSearchBundle.getEntry();

        return retrievedLocations.stream().map(this::convertLocationBundleEntryToLocationDto).collect(Collectors.toList());
    }

    @Override
    public List<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size) {
        int numberOfLocationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getLocation().getPagination().getMaxSize()).orElse(ocpFisProperties.getLocation().getPagination().getDefaultSize());

        Bundle firstPageLocationSearchBundle;
        Bundle otherPageLocationSearchBundle;

        IQuery locationsSearchQuery = fhirClient.search().forResource(Location.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId));

        if (status.isPresent() && status.get().size() > 0) {
            log.info("Searching for location with the following specific status(es) for the given OrganizationID:" + organizationResourceId);
            status.get().forEach(log::info);
            locationsSearchQuery.where(new TokenClientParam("status").exactly().codes(status.get()));
        } else {
            log.info("Searching for locations with ALL statuses for the given OrganizationID:" + organizationResourceId);
        }

        //The following bundle only contains Page 1 of the resultSet
        firstPageLocationSearchBundle = (Bundle) locationsSearchQuery.count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageLocationSearchBundle == null || firstPageLocationSearchBundle.getEntry().size() < 1) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            throw new LocationNotFoundException("No location found for the given OrganizationID:" + organizationResourceId);
        }

        log.info("FHIR Location(s) bundle retrieved " + firstPageLocationSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        otherPageLocationSearchBundle = firstPageLocationSearchBundle;
        if (page.isPresent() && page.get() > 1 && otherPageLocationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            otherPageLocationSearchBundle = getLocationSearchBundleAfterFirstPage(otherPageLocationSearchBundle, page.get(), numberOfLocationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedLocations = otherPageLocationSearchBundle.getEntry();

        return retrievedLocations.stream().map(this::convertLocationBundleEntryToLocationDto).collect(Collectors.toList());
    }

    @Override
    public LocationDto getLocation(String locationId) {
        Bundle locationBundle = fhirClient.search().forResource(Location.class)
                .where(new TokenClientParam("_id").exactly().code(locationId))
                .returnBundle(Bundle.class)
                .execute();

        if (locationBundle == null || locationBundle.getEntry().size() < 1) {
            log.info("No location was found for the given LocationID:" + locationId);
            throw new LocationNotFoundException("No location was found for the given LocationID:" + locationId);
        }

        log.info("FHIR Location bundle retrieved from FHIR server successfully");

        Bundle.BundleEntryComponent retrievedLocation = locationBundle.getEntry().get(0);

        return convertLocationBundleEntryToLocationDto(retrievedLocation);
    }

    @Override
    public LocationDto getChildLocation(String locationId) {
        Bundle childLocationBundle = fhirClient.search().forResource(Location.class)
                .where(new ReferenceClientParam("partof").hasId(locationId))
                .returnBundle(Bundle.class)
                .execute();

        if (childLocationBundle == null || childLocationBundle.getEntry().size() < 1) {
            log.info("No child location found for the given LocationID:" + locationId);
            throw new LocationNotFoundException("No child location found for the given LocationID:" + locationId);
        }

        log.info("FHIR Location bundle retrieved from FHIR server successfully");

        Bundle.BundleEntryComponent retrievedLocation = childLocationBundle.getEntry().get(0);

        return convertLocationBundleEntryToLocationDto(retrievedLocation);
    }

    private Bundle getLocationSearchBundleAfterFirstPage(Bundle locationSearchBundle, int page, int size) {
        if (locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page >= 1 ? page : 1) - 1) * size;

            if (offset >= locationSearchBundle.getTotal()) {
                throw new LocationNotFoundException("No locations were found in the FHIR server for this page number");
            }

            String pageUrl = ocpFisProperties.getFhir().getPublish().getServerUrl().getResource()
                    + "?_getpages=" + locationSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + size
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return locationSearchBundle;
    }

    private LocationDto convertLocationBundleEntryToLocationDto(Bundle.BundleEntryComponent fhirLocationModel) {
        LocationDto tempLocationDto = modelMapper.map(fhirLocationModel.getResource(), LocationDto.class);
        tempLocationDto.setLogicalId(fhirLocationModel.getResource().getIdElement().getIdPart());
        return tempLocationDto;
    }
}
