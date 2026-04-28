package edu.jlu.controllers;

import edu.jlu.services.XgboostEvaluateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class XgboostEvaluateController {

    @Autowired
    private XgboostEvaluateService evalService;

    /**
     * 返回模型评估可视化页面（Thymeleaf模板）
     * 访问路径：/sleep/evaluation
     */
    @GetMapping("/sleep/evaluation")
    public String evaluationPage() {
        return "/pages/xgboost_evaluation";   // 对应 templates/xgboost_evaluation.html
    }

    /**
     * 获取特征重要性
     * GET /api/eval/importance?model=model1&type=weight
     */
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

    /**
     * 获取 SHAP 原始数据
     * GET /api/eval/shap?model=model1
     */
    @GetMapping("/api/eval/shap")
    @ResponseBody
    public List<Map<String, Object>> getShapData(@RequestParam String model) {
        try {
            return evalService.getShapData(model);
        } catch (IOException e) {
            throw new RuntimeException("读取 SHAP 文件失败: " + e.getMessage(), e);
        }
    }
}