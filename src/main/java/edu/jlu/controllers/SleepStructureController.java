package edu.jlu.controllers;

import edu.jlu.services.SleepStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/sleep")
public class SleepStructureController {

    @Autowired
    private SleepStructureService sleepStructureService;

    @GetMapping("/sleep-structure")
    public String sleepStructurePage(Model model) {
        Map<String, Object> ageRange = sleepStructureService.getAgeRange();
        model.addAttribute("minAge", ageRange.get("minAge"));
        model.addAttribute("maxAge", ageRange.get("maxAge"));
        return "pages/sleep-structure";
    }

    @GetMapping("/sleep-structure/api/chart-data")
    @ResponseBody
    public Map<String, Object> getChartData(@RequestParam Integer minAge, @RequestParam Integer maxAge) {
        return sleepStructureService.getChartData(minAge, maxAge);
    }
}