
package gov.samhsa.ocp.ocpfis.data.model.patientlist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WrapperPatientDto {

    private Integer size;
    private Double totalNumberOfPages;
    private Integer currentPage;
    private Integer currentPageSize;
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;
    private Boolean firstPage;
    private Boolean lastPage;
    private Integer totalElements;
    private Boolean hasElements;
    private List<Element> elements = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Double getTotalNumberOfPages() {
        return totalNumberOfPages;
    }

    public void setTotalNumberOfPages(Double totalNumberOfPages) {
        this.totalNumberOfPages = totalNumberOfPages;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getCurrentPageSize() {
        return currentPageSize;
    }

    public void setCurrentPageSize(Integer currentPageSize) {
        this.currentPageSize = currentPageSize;
    }

    public Boolean getHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(Boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public Boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(Boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }

    public Boolean getFirstPage() {
        return firstPage;
    }

    public void setFirstPage(Boolean firstPage) {
        this.firstPage = firstPage;
    }

    public Boolean getLastPage() {
        return lastPage;
    }

    public void setLastPage(Boolean lastPage) {
        this.lastPage = lastPage;
    }

    public Integer getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Integer totalElements) {
        this.totalElements = totalElements;
    }

    public Boolean getHasElements() {
        return hasElements;
    }

    public void setHasElements(Boolean hasElements) {
        this.hasElements = hasElements;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
