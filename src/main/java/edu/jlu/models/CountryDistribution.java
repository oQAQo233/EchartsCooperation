package edu.jlu.models;

public class CountryDistribution {
    private String name;
    private Integer value;
    private Double avgDuration;
    private Double avgQuality;

    public CountryDistribution() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    public Double getAvgDuration() { return avgDuration; }
    public void setAvgDuration(Double avgDuration) { this.avgDuration = avgDuration; }
    public Double getAvgQuality() { return avgQuality; }
    public void setAvgQuality(Double avgQuality) { this.avgQuality = avgQuality; }
}