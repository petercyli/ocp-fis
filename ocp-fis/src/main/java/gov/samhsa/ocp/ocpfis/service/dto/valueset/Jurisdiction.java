
package gov.samhsa.ocp.ocpfis.service.dto.valueset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jurisdiction {

    private List<Coding> coding = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public List<Coding> getCoding() {
        return coding;
    }

    public void setCoding(List<Coding> coding) {
        this.coding = coding;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
