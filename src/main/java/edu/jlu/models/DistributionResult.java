package edu.jlu.models;

import java.util.List;

public class DistributionResult {
    private List<InnerDistributionItem> inner;
    private List<OuterDistributionItem> outer;

    public DistributionResult() {}

    public List<InnerDistributionItem> getInner() { return inner; }
    public void setInner(List<InnerDistributionItem> inner) { this.inner = inner; }
    public List<OuterDistributionItem> getOuter() { return outer; }
    public void setOuter(List<OuterDistributionItem> outer) { this.outer = outer; }
}