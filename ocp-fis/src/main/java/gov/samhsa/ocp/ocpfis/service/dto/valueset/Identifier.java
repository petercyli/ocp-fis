
package gov.samhsa.ocp.ocpfis.service.dto.valueset;

import java.util.HashMap;
import java.util.Map;

public class Identifier {

    private String system;
    private String value;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
