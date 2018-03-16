package gov.samhsa.ocp.ocpfis.util;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class PaginationUtil {

    public static Bundle getSearchBundleFirstPage(IQuery query, int count, Optional<Include> include) {
        if (include.isPresent()) {
            return (Bundle) query.count(count)
                    .include(include.get())
                    .returnBundle(Bundle.class)
                    .encodedJson()
                    .execute();
        } else {
            return (Bundle) query.count(count)
                    .returnBundle(Bundle.class)
                    .encodedJson()
                    .execute();
        }
    }

    public static Bundle getSearchBundleAfterFirstPage(IGenericClient fhirClient, FisProperties fisProperties, Bundle SearchBundle, int pageNumber, int pageSize) {
        if (SearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((pageNumber >= 1 ? pageNumber : 1) - 1) * pageSize;

            if (offset >= SearchBundle.getTotal()) {
                throw new ResourceNotFoundException("No resources were found in the FHIR server for the page number: " + pageNumber);
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + SearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + pageSize
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        } else {
            throw new ResourceNotFoundException("No resources were found in the FHIR server for the page number: " + pageNumber);
        }
    }

    public static PageDto<?> applyPaginationForCustomArrayList(List<?> elements,
                                                               int numberOfElementsPerPage,
                                                               Optional<Integer> pageNumber,
                                                               boolean throwExceptionWhenResourceNotFound) {
        boolean firstPage = isFirstPage(pageNumber);
        int currentPage = firstPage ? 1 : pageNumber.get(); // Assuming page number starts with 1

        if (elements == null || elements.isEmpty()) {
            if (throwExceptionWhenResourceNotFound) {
                throw new ResourceNotFoundException("No resources found!");
            } else {
                return new PageDto<>(new ArrayList<>(), numberOfElementsPerPage, 0, 0, 0, 0);
            }
        }

        int totalElements = elements.size();
        double totalPages = Math.ceil((double) totalElements / numberOfElementsPerPage);

        // Check validity of the page number
        if (currentPage > totalPages) {
            throw new ResourceNotFoundException("No resources were found in the FHIR server for the page number: " + currentPage);
        }

        int startIndex = ((currentPage - 1) * numberOfElementsPerPage);
        int endIndex = (currentPage * numberOfElementsPerPage) - 1;
        int lastElementIndex = totalElements - 1;

        // Just to be doubly sure
        if (startIndex > lastElementIndex) {
            throw new ResourceNotFoundException("Something is off about the page number you are requesting! ");
        }

        List<?> currentPageElements;
        if (endIndex > lastElementIndex) {
            currentPageElements = elements.subList(startIndex, ++lastElementIndex);
        } else {
            currentPageElements = elements.subList(startIndex, ++endIndex);
        }
        return new PageDto<>(currentPageElements, numberOfElementsPerPage, totalPages, currentPage, currentPageElements.size(), totalElements);
    }

    public static boolean isFirstPage(Optional<Integer> pageNumber) {
        boolean firstPage = true;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            firstPage = false;
        }
        return firstPage;
    }

    public static int getValidPageSize(FisProperties fisProperties, Optional<Integer> pageSize, String resource) {
        int numberOfResourcesPerPage;

        switch (resource.toUpperCase()) {
            case "ACTIVITYDEFINITION":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getActivityDefinition().getPagination().getMaxSize()).orElse(fisProperties.getActivityDefinition().getPagination().getDefaultSize());
                break;
            case "APPOINTMENT":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getAppointment().getPagination().getMaxSize()).orElse(fisProperties.getAppointment().getPagination().getDefaultSize());
                break;
            case "COMMUNICATION":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getCommunication().getPagination().getMaxSize()).orElse(fisProperties.getCommunication().getPagination().getDefaultSize());
                break;
            case "LOCATION":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getLocation().getPagination().getMaxSize()).orElse(fisProperties.getLocation().getPagination().getDefaultSize());
                break;
            case "CARETEAM":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getCareTeam().getPagination().getMaxSize()).orElse(fisProperties.getCareTeam().getPagination().getDefaultSize());
                break;
            case "HEALTHCARESERVICE":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getHealthcareService().getPagination().getMaxSize()).orElse(fisProperties.getHealthcareService().getPagination().getDefaultSize());
                break;
            case "ORGANIZATION":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getOrganization().getPagination().getMaxSize()).orElse(fisProperties.getOrganization().getPagination().getDefaultSize());
                break;
            case "PATIENT":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getPatient().getPagination().getMaxSize()).orElse(fisProperties.getPatient().getPagination().getDefaultSize());
                break;
            case "PRACTITIONER":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getPractitioner().getPagination().getMaxSize()).orElse(fisProperties.getPractitioner().getPagination().getDefaultSize());
                break;
            case "RELATEDPERSON":
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getRelatedPerson().getPagination().getMaxSize()).orElse(fisProperties.getRelatedPerson().getPagination().getDefaultSize());
                break;
            default:
                //Get location's page size. Need to find a better way for default case
                numberOfResourcesPerPage = pageSize.filter(s -> s > 0 &&
                        s <= fisProperties.getLocation().getPagination().getMaxSize()).orElse(fisProperties.getLocation().getPagination().getDefaultSize());
        }
        return numberOfResourcesPerPage;
    }
}
