package edu.jlu.controllers;

import edu.jlu.services.XgboostEvaluateService;
import edu.jlu.services.XgboostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class XgboostEvaluateController {

    @Autowired
    private XgboostEvaluateService evalService;

    @Autowired
    private XgboostService xgboostService;   // 新增注入

    @GetMapping("/sleep/evaluation")
    public String evaluationPage() {
        return "/pages/xgboost_evaluation";
    }

    @GetMapping("/api/eval/importance")
    @ResponseBody
    public List<Map<String, Object>> getFeatureImportance(
            @RequestParam String model,
            @RequestParam(defaultValue = "weight") String type) {
        try {
            return evalService.getFeatureImportance(model, type);
        } catch (IOException e) {
            throw new RuntimeException("读取特征重要性文件失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/api/eval/shap")
    @ResponseBody
    public List<Map<String, Object>> getShapData(@RequestParam String model) {
        try {
            return evalService.getShapData(model);
        } catch (IOException e) {
            throw new RuntimeException("读取 SHAP 文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 新增 API：获取模型的分类类别标签列表
     */
    @GetMapping("/api/eval/model-classes")
    @ResponseBody
    public List<String> getModelClasses(@RequestParam String model) {
        Map<String, Object> cfg = xgboostService.getModelOutputConfig(model);
        if (cfg != null && "classification".equals(cfg.get("type"))) {
            Object labels = cfg.get("labels");
            if (labels instanceof List) {
                return (List<String>) labels;
            }
        }
        return Collections.emptyList();
    }
}