package gov.samhsa.ocp.ocpfis.data.model.practitioner;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TempPractitionerDto {
    private boolean active;
    private List<AddressDto> addresses = null;
    private List<TelecomDto> telecoms = null;
    private List<NameDto> name;
    private String logicalId;
    private List<IdentifierDto> identifiers = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<AddressDto> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AddressDto> addresses) {
        this.addresses = addresses;
    }

    public List<TelecomDto> getTelecoms() {
        return telecoms;
    }

    public void setTelecoms(List<TelecomDto> telecoms) {
        this.telecoms = telecoms;
    }

    public List<NameDto> getName() {
        return name;
    }

    public void setName(List<NameDto> name) {
        this.name = name;
    }

    public String getLogicalId() {
        return logicalId;
    }

    public void setLogicalId(String logicalId) {
        this.logicalId = logicalId;
    }

    public List<IdentifierDto> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<IdentifierDto> identifiers) {
        this.identifiers = identifiers;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
