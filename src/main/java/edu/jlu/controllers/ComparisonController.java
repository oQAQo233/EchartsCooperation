package edu.jlu.controllers;

import edu.jlu.services.ComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/sleep")
public class ComparisonController {

    @Autowired
    private ComparisonService comparisonService;

    @GetMapping("/comparison")
    public String comparisonPage() {
        return "pages/comparison";
    }

    @GetMapping("/api/chart/comparison")
    @ResponseBody
    public Map<String, Object> getComparisonData(
            @RequestParam("dimension") String dimension) {
        try {
            return comparisonService.getComparisonData(dimension);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "数据加载失败: " + e.getMessage());
            return error;
        }
    }
}