package edu.jlu.models;

public class OuterDistributionItem {
    private String innerName;
    private String outerName;
    private Integer value;

    public OuterDistributionItem() {}

    public String getInnerName() { return innerName; }
    public void setInnerName(String innerName) { this.innerName = innerName; }
    public String getOuterName() { return outerName; }
    public void setOuterName(String outerName) { this.outerName = outerName; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
}