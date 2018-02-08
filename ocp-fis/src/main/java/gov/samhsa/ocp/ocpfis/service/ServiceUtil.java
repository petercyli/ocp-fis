package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;

@Slf4j
public final class ServiceUtil {
    private static IGenericClient fhirClient;
    private static FisProperties fisProperties;

    public ServiceUtil(IGenericClient fhirClient, FisProperties fisProperties) {
        ServiceUtil.fhirClient = fhirClient;
        ServiceUtil.fisProperties = fisProperties;
    }

    public static Bundle getSearchBundleAfterFirstPage(Bundle SearchBundle, int pageNumber, int pageSize) {
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
}
