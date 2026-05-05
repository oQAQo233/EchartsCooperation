package edu.jlu.controllers;

import edu.jlu.models.BedtimeBehaviorImpact;
import edu.jlu.services.MedicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/sleep")
public class MedicalController {

    @Autowired
    private MedicalService medicalService;

    @GetMapping("/medical-behavior")
    public String medicalBehaviorPage() {
        return "pages/medical-behavior";
    }

    @GetMapping("/api/chart/behavior")
    @ResponseBody
    public List<BedtimeBehaviorImpact> getBehaviorImpactData() {
        return medicalService.getBedtimeBehaviorImpact();
    }
}
