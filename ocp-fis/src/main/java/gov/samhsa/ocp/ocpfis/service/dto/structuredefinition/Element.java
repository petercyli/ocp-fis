
package gov.samhsa.ocp.ocpfis.service.dto.structuredefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element {

    private String id;
    private String path;
    private String _short;
    private String definition;
    private List<String> alias = null;
    private Boolean mustSupport;
    private Boolean isModifier;
    private String requirements;
    private Integer min;
    private String comment;
    private List<Example> example = null;
    private String max;
    private Slicing slicing;
    private String sliceName;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getShort() {
        return _short;
    }

    public void setShort(String _short) {
        this._short = _short;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
    }

    public Boolean getMustSupport() {
        return mustSupport;
    }

    public void setMustSupport(Boolean mustSupport) {
        this.mustSupport = mustSupport;
    }

    public Boolean getIsModifier() {
        return isModifier;
    }

    public void setIsModifier(Boolean isModifier) {
        this.isModifier = isModifier;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Example> getExample() {
        return example;
    }

    public void setExample(List<Example> example) {
        this.example = example;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public Slicing getSlicing() {
        return slicing;
    }

    public void setSlicing(Slicing slicing) {
        this.slicing = slicing;
    }

    public String getSliceName() {
        return sliceName;
    }

    public void setSliceName(String sliceName) {
        this.sliceName = sliceName;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
