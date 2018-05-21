
package gov.samhsa.ocp.ocpfis.service.dto.valueset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueSetDto {

    private String resourceType;
    private String id;
    private Text text;
    private List<Extension> extension = null;
    private String url;
    private List<Identifier> identifier = null;
    private String version;
    private String name;
    private String status;
    private Boolean experimental;
    private String date;
    private String publisher;
    private List<Contact> contact = null;
    private String description;
    private String copyright;
    private Compose compose;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private List<Jurisdiction> jurisdiction = new ArrayList<>();

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public List<Extension> getExtension() {
        return extension;
    }

    public void setExtension(List<Extension> extension) {
        this.extension = extension;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Identifier> getIdentifier() {
        return identifier;
    }

    public void setIdentifier(List<Identifier> identifier) {
        this.identifier = identifier;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getExperimental() {
        return experimental;
    }

    public void setExperimental(Boolean experimental) {
        this.experimental = experimental;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public List<Contact> getContact() {
        return contact;
    }

    public void setContact(List<Contact> contact) {
        this.contact = contact;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public Compose getCompose() {
        return compose;
    }

    public void setCompose(Compose compose) {
        this.compose = compose;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public List<Jurisdiction> getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(List<Jurisdiction> jurisdiction) {
        this.jurisdiction = jurisdiction;
    }
}
