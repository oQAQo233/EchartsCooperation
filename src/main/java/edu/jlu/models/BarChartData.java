package edu.jlu.models;

public class BarChartData {
    private java.util.List<String> categories;
    private java.util.List<Integer> counts;

    public BarChartData() {}

    public java.util.List<String> getCategories() { return categories; }
    public void setCategories(java.util.List<String> categories) { this.categories = categories; }
    public java.util.List<Integer> getCounts() { return counts; }
    public void setCounts(java.util.List<Integer> counts) { this.counts = counts; }
}