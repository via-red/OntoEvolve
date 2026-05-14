package com.ontoevolve.saas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackDimensionConfig {

    private String name;
    private String label;
    private String description;
    private double min = 0;
    private double max = 1;
    private boolean higherIsBetter = true;
    private int sortOrder;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }

    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }

    public boolean isHigherIsBetter() { return higherIsBetter; }
    public void setHigherIsBetter(boolean higherIsBetter) { this.higherIsBetter = higherIsBetter; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
