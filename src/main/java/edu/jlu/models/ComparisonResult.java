package edu.jlu.models;

import java.util.List;
import java.util.Map;

public class ComparisonResult {
    private BarChartData barChart;
    private Map<String, List<Double>> metricAverages;
    private Map<String, Double> metricMaxValues;
    private List<String> categories;
    private List<String> metricLabels;

    public ComparisonResult() {}

    public BarChartData getBarChart() { return barChart; }
    public void setBarChart(BarChartData barChart) { this.barChart = barChart; }
    public Map<String, List<Double>> getMetricAverages() { return metricAverages; }
    public void setMetricAverages(Map<String, List<Double>> metricAverages) { this.metricAverages = metricAverages; }
    public Map<String, Double> getMetricMaxValues() { return metricMaxValues; }
    public void setMetricMaxValues(Map<String, Double> metricMaxValues) { this.metricMaxValues = metricMaxValues; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public List<String> getMetricLabels() { return metricLabels; }
    public void setMetricLabels(List<String> metricLabels) { this.metricLabels = metricLabels; }
}