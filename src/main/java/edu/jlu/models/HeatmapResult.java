package edu.jlu.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HeatmapResult {
    private String title;
    @JsonProperty("xLabels")
    private List<String> xLabels;
    @JsonProperty("yLabels")
    private List<String> yLabels;
    @JsonProperty("data")
    private List<List<Object>> data;

    public HeatmapResult() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getXLabels() { return xLabels; }
    public void setXLabels(List<String> xLabels) { this.xLabels = xLabels; }
    public List<String> getYLabels() { return yLabels; }
    public void setYLabels(List<String> yLabels) { this.yLabels = yLabels; }
    public List<List<Object>> getData() { return data; }
    public void setData(List<List<Object>> data) { this.data = data; }
}