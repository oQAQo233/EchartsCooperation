package edu.jlu.controllers;

import edu.jlu.services.DistributionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/sleep")
public class DistributionController {

    @Autowired
    private DistributionService distributionService;

    @GetMapping("/distribution")
    public String distributionPage() {
        return "pages/distribution";
    }

    @GetMapping("/api/chart/distribution")
    @ResponseBody
    public Map<String, Object> getDistribution(
            @RequestParam("inner") String inner,
            @RequestParam("outer") String outer) {
        return distributionService.getDistributionData(inner, outer);
    }
}
