package gov.samhsa.ocp.ocpfis.data.model.patient;

import gov.samhsa.ocp.ocpfis.data.model.organization.Element;
import gov.samhsa.ocp.ocpfis.data.model.practitioner.TempPractitionerDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempPageDto<T>{
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
    private List<T> elements = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
