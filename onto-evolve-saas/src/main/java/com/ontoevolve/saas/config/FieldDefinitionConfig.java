package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldDefinitionConfig {

    public enum FieldType { STRING, NUMBER, BOOLEAN, DATE, ENUM }

    private String fieldName;
    private FieldType fieldType;
    private String label;
    private boolean required;
    private List<String> enumValues;
    private int sortOrder;

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public List<String> getEnumValues() { return enumValues; }
    public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
