
package gov.samhsa.ocp.ocpfis.service.dto.structuredefinition;

import java.util.HashMap;
import java.util.Map;

public class Meta {

    private String versionId;
    private String lastUpdated;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
