package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
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

        Bundle allLocationsSearchBundle;

        if (status.isPresent() && status.get().size() > 0) {
            log.info("Searching for ALL locations with the following specific status(es).");
            status.get().forEach(log::info);

            allLocationsSearchBundle = fhirClient.search().forResource(Location.class)
                    .where(new TokenClientParam("status").exactly().codes(status.get()))
                    .count(numberOfLocationsPerPage)
                    .returnBundle(Bundle.class)
                    .execute();

        } else {
            log.info("Searching for location with ALL statuses");
            allLocationsSearchBundle = fhirClient.search().forResource(Location.class)
                    .count(numberOfLocationsPerPage)
                    .returnBundle(Bundle.class)
                    .execute();
        }

        if (allLocationsSearchBundle == null || allLocationsSearchBundle.getEntry().size() < 1) {
            throw new LocationNotFoundException("No locations were found in the FHIR server");
        }

        log.info("FHIR Location(s) bundle retrieved " + allLocationsSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        if (page.isPresent() && allLocationsSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            allLocationsSearchBundle = getLocationSearchBundleByPageAndSize(allLocationsSearchBundle, page, numberOfLocationsPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedLocations = allLocationsSearchBundle.getEntry();

        return retrievedLocations.stream().map(location -> modelMapper.map(location.getResource(), LocationDto.class)).collect(Collectors.toList());
    }

    @Override
    public List<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<List<String>> status, Optional<Integer> page, Optional<Integer> size) {
        int numberOfLocationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpFisProperties.getLocation().getPagination().getMaxSize()).orElse(ocpFisProperties.getLocation().getPagination().getDefaultSize());
        Bundle locationSearchBundle;

        if (status.isPresent() && status.get().size() > 0) {
            log.info("Searching for location with the following specific status(es) for the given OrganizationID:" + organizationResourceId);
            status.get().forEach(log::info);
            locationSearchBundle = fhirClient.search().forResource(Location.class)
                    .where(new ReferenceClientParam("organization").hasId(organizationResourceId))
                    .where(new TokenClientParam("status").exactly().codes(status.get()))
                    .count(numberOfLocationsPerPage)
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            log.info("Searching for location with ALL statuses for the given OrganizationID:" + organizationResourceId);
            locationSearchBundle = fhirClient.search().forResource(Location.class)
                    .where(new ReferenceClientParam("organization").hasId(organizationResourceId))
                    .count(numberOfLocationsPerPage)
                    .returnBundle(Bundle.class)
                    .execute();
        }

        if (locationSearchBundle == null || locationSearchBundle.getEntry().size() < 1) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            throw new LocationNotFoundException("No location found for the given OrganizationID:" + organizationResourceId);
        }

        log.info("FHIR Location(s) bundle retrieved " + locationSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        if (page.isPresent() && locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            locationSearchBundle = getLocationSearchBundleByPageAndSize(locationSearchBundle, page, numberOfLocationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedLocations = locationSearchBundle.getEntry();

        return retrievedLocations.stream().map(location -> modelMapper.map(location.getResource(), LocationDto.class)).collect(Collectors.toList());
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

        return modelMapper.map(retrievedLocation.getResource(), LocationDto.class);
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

        return modelMapper.map(retrievedLocation.getResource(), LocationDto.class);
    }

    private Bundle getLocationSearchBundleByPageAndSize(Bundle locationSearchBundle, Optional<Integer> page, int numberOfLocationsPerPage) {
        if (locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page.filter(p -> p >= 1).orElse(1)) - 1) * numberOfLocationsPerPage;

            if (offset >= locationSearchBundle.getTotal()) {
                throw new LocationNotFoundException("No locations were found in the FHIR server for this page number");
            }

            String pageUrl = ocpFisProperties.getFhir().getPublish().getServerUrl().getResource()
                    + "?_getpages=" + locationSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + numberOfLocationsPerPage
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return locationSearchBundle;
    }
}
