package edu.jlu.models;

public class BedtimeBehaviorImpact {
    private String groupName;
    private Integer peopleCount;
    private Double avgScreen;
    private Double avgLatency;
    private Double avgCaffeine;
    private Double avgAlcohol;

    public BedtimeBehaviorImpact() {}

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public Double getAvgScreen() { return avgScreen; }
    public void setAvgScreen(Double avgScreen) { this.avgScreen = avgScreen; }
    public Double getAvgLatency() { return avgLatency; }
    public void setAvgLatency(Double avgLatency) { this.avgLatency = avgLatency; }
    public Double getAvgCaffeine() { return avgCaffeine; }
    public void setAvgCaffeine(Double avgCaffeine) { this.avgCaffeine = avgCaffeine; }
    public Double getAvgAlcohol() { return avgAlcohol; }
    public void setAvgAlcohol(Double avgAlcohol) { this.avgAlcohol = avgAlcohol; }
}