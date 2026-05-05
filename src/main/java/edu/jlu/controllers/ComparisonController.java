package edu.jlu.controllers;

import edu.jlu.models.ComparisonResult;
import edu.jlu.services.ComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public ComparisonResult getComparisonData(
            @RequestParam("dimension") String dimension) {
        return comparisonService.getComparisonData(dimension);
    }
}
