
package gov.samhsa.ocp.ocpfis.data.model.organization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element {

    private Boolean active;
    private List<Address> addresses = null;
    private List<Telecom> telecoms = null;
    private String name;
    private String logicalId;
    private Object resourceURL;
    private List<Identifier> identifiers = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<Telecom> getTelecoms() {
        return telecoms;
    }

    public void setTelecoms(List<Telecom> telecoms) {
        this.telecoms = telecoms;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogicalId() {
        return logicalId;
    }

    public void setLogicalId(String logicalId) {
        this.logicalId = logicalId;
    }

    public Object getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(Object resourceURL) {
        this.resourceURL = resourceURL;
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
