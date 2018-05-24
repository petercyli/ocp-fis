
package gov.samhsa.ocp.ocpfis.data.model.practitioner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element {

    private String logicalId;
    private List<Identifier> identifiers = null;
    private Boolean active;
    private List<Name> name = null;
    private List<Telecom> telecoms = null;
    private List<Address> addresses = null;
    private List<PractitionerRole> practitionerRoles = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getLogicalId() {
        return logicalId;
    }

    public void setLogicalId(String logicalId) {
        this.logicalId = logicalId;
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<Name> getName() {
        return name;
    }

    public void setName(List<Name> name) {
        this.name = name;
    }

    public List<Telecom> getTelecoms() {
        return telecoms;
    }

    public void setTelecoms(List<Telecom> telecoms) {
        this.telecoms = telecoms;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<PractitionerRole> getPractitionerRoles() {
        return practitionerRoles;
    }

    public void setPractitionerRoles(List<PractitionerRole> practitionerRoles) {
        this.practitionerRoles = practitionerRoles;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
