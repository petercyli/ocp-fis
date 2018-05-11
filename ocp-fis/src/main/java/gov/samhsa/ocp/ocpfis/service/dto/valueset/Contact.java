
package gov.samhsa.ocp.ocpfis.service.dto.valueset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Contact {

    private List<Telecom> telecom = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public List<Telecom> getTelecom() {
        return telecom;
    }

    public void setTelecom(List<Telecom> telecom) {
        this.telecom = telecom;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
