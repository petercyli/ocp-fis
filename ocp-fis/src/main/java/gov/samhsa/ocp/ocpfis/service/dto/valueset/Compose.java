
package gov.samhsa.ocp.ocpfis.service.dto.valueset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Compose {

    private List<Include> include = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public List<Include> getInclude() {
        return include;
    }

    public void setInclude(List<Include> include) {
        this.include = include;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
