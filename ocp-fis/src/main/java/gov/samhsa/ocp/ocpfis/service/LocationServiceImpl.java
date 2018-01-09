package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.OcpProperties;
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

    private final OcpProperties ocpProperties;

    @Autowired
    public LocationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, OcpProperties ocpProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.ocpProperties = ocpProperties;
    }

    /**
     * Gets all available locations in the configured FHIR server
     *
     * @return
     */
    @Override
    public List<LocationDto> getAllLocations(Optional<Integer> page, Optional<Integer> size) {

        int numberOfLocationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpProperties.getLocation().getPagination().getMaxSize()).orElse(ocpProperties.getLocation().getPagination().getDefaultSize());

        Bundle allLocationsSearchBundle = fhirClient.search().forResource(Location.class)
                .count(numberOfLocationsPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (allLocationsSearchBundle == null || allLocationsSearchBundle.getEntry().size() < 1) {
            throw new LocationNotFoundException("No locations were found in the FHIR server");
        }

        log.info("FHIR Location(s) bundle retrieved " + allLocationsSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        if (page.isPresent() && allLocationsSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            allLocationsSearchBundle = getLocationSearchBundleByPage(allLocationsSearchBundle, page, numberOfLocationsPerPage);
        }
        List<Bundle.BundleEntryComponent> retrievedLocations = allLocationsSearchBundle.getEntry();

        return retrievedLocations.stream().map(location -> modelMapper.map(location.getResource(), LocationDto.class)).collect(Collectors.toList());
    }

    /**
     * Gets all locations(all levels) that are managed under a given Organization Id
     *
     * @param organizationResourceId
     * @return
     */
    @Override
    public List<LocationDto> getLocationsByOrganization(String organizationResourceId, Optional<Integer> page, Optional<Integer> size) {
        int numberOfLocationsPerPage = size.filter(s -> s > 0 &&
                s <= ocpProperties.getLocation().getPagination().getMaxSize()).orElse(ocpProperties.getLocation().getPagination().getDefaultSize());

        Bundle locationSearchBundle = fhirClient.search().forResource(Location.class)
                .where(new ReferenceClientParam("organization").hasId(organizationResourceId))
                .returnBundle(Bundle.class)
                .execute();

        if (locationSearchBundle == null || locationSearchBundle.getEntry().size() < 1) {
            log.info("No location found for the given OrganizationID:" + organizationResourceId);
            throw new LocationNotFoundException("No location found for the given OrganizationID:" + organizationResourceId);
        }

        log.info("FHIR Location(s) bundle retrieved " + locationSearchBundle.getTotal() + " location(s) from FHIR server successfully");

        if (page.isPresent() && locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            locationSearchBundle = getLocationSearchBundleByPage(locationSearchBundle, page, numberOfLocationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedLocations = locationSearchBundle.getEntry();

        return retrievedLocations.stream().map(location -> modelMapper.map(location.getResource(), LocationDto.class)).collect(Collectors.toList());
    }

    /**
     * Get Location By Id
     *
     * @param locationId
     * @return
     */
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

    /**
     * Gets level 1 child location for a given Location Id
     *
     * @param locationId
     * @return
     */
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

    private Bundle getLocationSearchBundleByPage(Bundle locationSearchBundle, Optional<Integer> page, int numberOfLocationsPerPage) {
        if (page.isPresent() && locationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page.filter(p -> p >= 1).orElse(1)) - 1) * numberOfLocationsPerPage;

            if (offset >= locationSearchBundle.getTotal()) {
                throw new LocationNotFoundException("No locations were found in the FHIR server for this page number");
            }

            String pageUrl = ocpProperties.getFhir().getPublish().getServerUrl().getResource()
                    + "?_getpages=" + locationSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + numberOfLocationsPerPage
                    + "&_bundletype=searchset";

            // Load the required page
            Bundle requestedPageLocationsSearchBundle = fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();

            return requestedPageLocationsSearchBundle;
        }
        return locationSearchBundle;
    }
}
