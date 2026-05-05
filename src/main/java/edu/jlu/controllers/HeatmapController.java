package edu.jlu.controllers;

import edu.jlu.services.HeatmapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/sleep")
public class HeatmapController {

    @Autowired
    private HeatmapService heatmapService;

    @GetMapping("/heatmap")
    public String heatmapPage() {
        return "pages/heatmap";
    }

    /**
     * 兼容保留：原来的默认热力图接口（睡眠时长 × 睡眠质量）
     */
    @GetMapping("/api/chart/heatmap/sleep-duration-vs-quality")
    @ResponseBody
    public Map<String, Object> sleepDurationVsQualityHeatmap() {
        return heatmapService.getSleepDurationVsQualityHeatmap();
    }

    /**
     * 新增：可细化分桶的 range 热力图接口（连续分桶）
     *
     * 示例：
     * /sleep/api/chart/heatmap/range?xField=sleep_duration_hrs&yField=sleep_quality_score
     * &xStart=3&xEnd=11&xStep=0.5&yStart=1&yEnd=11&yStep=1
     */
    @GetMapping("/api/chart/heatmap/range")
    @ResponseBody
    public Map<String, Object> rangeHeatmap(
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