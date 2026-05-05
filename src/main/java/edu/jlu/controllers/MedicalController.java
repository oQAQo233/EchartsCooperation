package edu.jlu.controllers;

import edu.jlu.services.MedicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sleep")
public class MedicalController {

    // 1. 注入你刚才写好的 Service
    @Autowired
    private MedicalService medicalService;

    @GetMapping("/medical-behavior")
    public String medicalBehaviorPage() {
        return "pages/medical-behavior";
    }
    @GetMapping("/api/chart/behavior")
    @ResponseBody
    public List<Map<String, Object>> getBehaviorImpactData() {
        return medicalService.getBedtimeBehaviorImpact();
    }

}
