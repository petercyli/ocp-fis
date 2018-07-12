
package gov.samhsa.ocp.ocpfis.service.dto.structuredefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Slicing {

    private List<Discriminator> discriminator = null;
    private String rules;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public List<Discriminator> getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(List<Discriminator> discriminator) {
        this.discriminator = discriminator;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
