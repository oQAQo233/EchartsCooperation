package edu.jlu.controllers;

import edu.jlu.models.SleepHealthRecord;
import edu.jlu.services.SleepHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sleep")
public class SleepHealthController {

    @Autowired
    private SleepHealthService sleepHealthService;

    @GetMapping
    public String sleepPage(Model model) {
        return "redirect:/sleep/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "pages/dashboard";
    }

    @GetMapping("/records")
    public String recordsPage(Model model) {
        model.addAttribute("records", sleepHealthService.getAllRecords());
        model.addAttribute("count", sleepHealthService.getRecordCount());
        return "pages/records";
    }

    @GetMapping("/api/records")
    @ResponseBody
    public List<SleepHealthRecord> getAllRecords() {
        return sleepHealthService.getAllRecords();
    }

    @GetMapping("/api/records/{personId}")
    @ResponseBody
    public SleepHealthRecord getRecord(@PathVariable Integer personId) {
        return sleepHealthService.getRecordById(personId);
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStats() {
        return sleepHealthService.getDashboardStats();
    }

    @GetMapping("/api/chart/age-distribution")
    @ResponseBody
    public List<Map<String, Object>> getAgeDistribution() {
        return sleepHealthService.getAgeDistribution();
    }

    @GetMapping("/api/chart/sleep-disorder-risk")
    @ResponseBody
    public List<Map<String, Object>> getSleepDisorderRisk() {
        return sleepHealthService.getSleepDisorderRiskDistribution();
    }

    @GetMapping("/api/chart/sleep-duration-by-occupation")
    @ResponseBody
    public List<Map<String, Object>> getSleepDurationByOccupation() {
        return sleepHealthService.getSleepDurationByOccupation();
    }

    @GetMapping("/api/chart/sleep-quality-by-chronotype")
    @ResponseBody
    public List<Map<String, Object>> getSleepQualityByChronotype() {
        return sleepHealthService.getSleepQualityByChronotype();
    }

    @GetMapping("/api/chart/sleep-duration-vs-quality")
    @ResponseBody
    public List<Map<String, Object>> getSleepDurationVsQuality() {
        return sleepHealthService.getSleepDurationVsQuality();
    }

    @GetMapping("/api/chart/stress-distribution")
    @ResponseBody
    public List<Map<String, Object>> getStressDistribution() {
        return sleepHealthService.getStressScoreDistribution();
    }
}
