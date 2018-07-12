
package gov.samhsa.ocp.ocpfis.service.dto.structuredefinition;

import java.util.HashMap;
import java.util.Map;

public class Telecom {

    private String system;
    private String value;
    private Period period;
    private String use;
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

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
