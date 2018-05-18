
package gov.samhsa.ocp.ocpfis.data.model.practitioner;

import java.util.HashMap;
import java.util.Map;

public class Practitioner {

    private String reference;
    private Object display;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Object getDisplay() {
        return display;
    }

    public void setDisplay(Object display) {
        this.display = display;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
