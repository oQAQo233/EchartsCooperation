package edu.jlu.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OuterDistributionItem {
    @JsonProperty("inner_name")
    private String innerName;

    @JsonProperty("outer_name")
    private String outerName;

    private Integer value;

    public OuterDistributionItem() {}

    @JsonProperty("inner_name")
    public String getInnerName() { return innerName; }

    @JsonProperty("inner_name")
    public void setInnerName(String innerName) { this.innerName = innerName; }

    @JsonProperty("outer_name")
    public String getOuterName() { return outerName; }

    @JsonProperty("outer_name")
    public void setOuterName(String outerName) { this.outerName = outerName; }

    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
}