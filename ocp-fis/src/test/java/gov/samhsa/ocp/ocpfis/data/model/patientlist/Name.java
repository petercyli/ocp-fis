
package gov.samhsa.ocp.ocpfis.data.model.patientlist;

import java.util.HashMap;
import java.util.Map;

public class Name {

    private String firstName;
    private String lastName;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
