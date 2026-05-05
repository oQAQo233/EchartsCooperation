package edu.jlu.controllers;

import edu.jlu.models.HeatmapResult;
import edu.jlu.services.HeatmapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/sleep")
public class HeatmapController {

    @Autowired
    private HeatmapService heatmapService;

    @GetMapping("/heatmap")
    public String heatmapPage() {
        return "pages/heatmap";
    }

    @GetMapping("/api/chart/heatmap/sleep-duration-vs-quality")
    @ResponseBody
    public HeatmapResult sleepDurationVsQualityHeatmap() {
        return heatmapService.getSleepDurationVsQualityHeatmap();
    }

    @GetMapping("/api/chart/heatmap/range")
    @ResponseBody
    public HeatmapResult rangeHeatmap(
            @RequestParam String xField,
            @RequestParam String yField,
            @RequestParam double xStart,
            @RequestParam double xEnd,
            @RequestParam double xStep,
            @RequestParam double yStart,
            @RequestParam double yEnd,
            @RequestParam double yStep,
            @RequestParam(required = false) String title
    ) {
        return heatmapService.getRangeHeatmap(
                xField, yField,
                xStart, xEnd, xStep,
                yStart, yEnd, yStep,
                title
        );
    }
}
