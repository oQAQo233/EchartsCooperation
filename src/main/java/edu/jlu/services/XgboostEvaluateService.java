package edu.jlu.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class XgboostEvaluateService {

    private static final Logger log = LoggerFactory.getLogger(XgboostEvaluateService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;

    @Autowired
    private XgboostService xgboostService;

    public XgboostEvaluateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 读取特征重要性 JSON，并附加中文名
     */
    public List<Map<String, Object>> getFeatureImportance(String modelName, String type) throws IOException {
        String fileName = modelName + "_" + type + ".json";
        Resource resource = resourceLoader.getResource("classpath:evals/" + fileName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("特征重要性文件不存在: " + fileName);
        }
        List<Map<String, Object>> rawData;
        try (InputStream is = resource.getInputStream()) {
            rawData = mapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
        }

        // 构建特征 key -> name 映射
        Map<String, String> nameMap = buildFeatureNameMap();
        for (Map<String, Object> item : rawData) {
            String key = (String) item.get("feature");
            if (nameMap.containsKey(key)) {
                item.put("feature_name", nameMap.get(key));
            }
        }
        return rawData;
    }

    /**
     * 读取 SHAP JSON，并进行反归一化和标签映射
     */
    public List<Map<String, Object>> getShapData(String modelName) throws IOException {
        String fileName = modelName + "_shap.json";
        Resource resource = resourceLoader.getResource("classpath:evals/" + fileName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("SHAP 文件不存在: " + fileName);
        }
        List<Map<String, Object>> rawData;
        try (InputStream is = resource.getInputStream()) {
            rawData = mapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
        }

        // 加载预处理参数 (scaler)
        XgboostService.PreprocessParams scaler;
        try {
            scaler = xgboostService.loadPreprocessParams(modelName);
        } catch (IOException e) {
            log.error("无法加载模型 {} 的归一化参数", modelName, e);
            throw new RuntimeException("无法加载模型 " + modelName + " 的归一化参数: " + e.getMessage(), e);
        }

        // 防御性处理：确保 numericColumns 不为 null
        List<String> numericColumns = scaler.numericColumns != null ? scaler.numericColumns : Collections.emptyList();
        double[] mean = scaler.mean;
        double[] std = scaler.std;

        // 特征元数据映射 (key -> meta)
        List<Map<String, Object>> allFeatures = xgboostService.getAllFeatures();
        Map<String, Map<String, Object>> featureMeta = new HashMap<>();
        for (Map<String, Object> feat : allFeatures) {
            featureMeta.put((String) feat.get("key"), feat);
        }

        // 模型输出配置（用于 class 标签）
        Map<String, Object> outputCfg = xgboostService.getModelOutputConfig(modelName);
        if (outputCfg == null) {
            log.warn("模型 {} 在 OUTPUT_CONFIG 中不存在，class 标签将无法生成", modelName);
        }
        String outputType = outputCfg != null ? (String) outputCfg.get("type") : null;
        List<String> classLabels = null;
        if ("classification".equals(outputType) && outputCfg != null && outputCfg.containsKey("labels")) {
            classLabels = (List<String>) outputCfg.get("labels");
        }

        // 转换每一条 SHAP 记录
        for (Map<String, Object> record : rawData) {
            String featureKey = (String) record.get("feature");
            Map<String, Object> meta = featureMeta.get(featureKey);

            // 附加特征中文名
            if (meta != null) {
                record.put("feature_name", meta.get("name"));
            } else {
                record.put("feature_name", featureKey); // fallback
            }

            Object rawValue = record.get("feature_value");
            if (rawValue != null && meta != null) {
                String featureType = (String) meta.get("type");
                if ("numeric".equals(featureType) && numericColumns.contains(featureKey)) {
                    // 数值型：反归一化
                    try {
                        int idx = numericColumns.indexOf(featureKey);
                        double value = Double.parseDouble(rawValue.toString());
                        double original = value * std[idx] + mean[idx];
                        record.put("feature_value", original);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        log.warn("特征 {} 反归一化失败，保留原值", featureKey, e);
                    }
                } else if ("categorical".equals(featureType) && meta.containsKey("options")) {
                    // 分类型：必须保证 feature_value 变成数值，同时保留标签
                    List<Map<String, Object>> options = (List<Map<String, Object>>) meta.get("options");
                    boolean converted = false;

                    // 尝试将 rawValue 作为数字解析（可能是整数或数字字符串）
                    try {
                        int code = Integer.parseInt(rawValue.toString());
                        // 如果成功，说明已经是数字，直接作为 feature_value
                        record.put("feature_value", code);
                        // 查找对应标签
                        for (Map<String, Object> opt : options) {
                            if ((int) opt.get("value") == code) {
                                record.put("feature_value_label", opt.get("label"));
                                break;
                            }
                        }
                        converted = true;
                    } catch (NumberFormatException ignored) {
                        // 不是数字，可能是标签字符串
                    }

                    if (!converted) {
                        // rawValue 是标签字符串，在 options 中按标签匹配
                        String strVal = rawValue.toString().trim();
                        for (Map<String, Object> opt : options) {
                            if (strVal.equals(opt.get("label"))) {
                                int code = (int) opt.get("value");
                                record.put("feature_value", code);
                                record.put("feature_value_label", opt.get("label"));
                                converted = true;
                                break;
                            }
                        }
                    }

                    if (!converted) {
                        log.warn("分类特征 {} 无法将 feature_value '{}' 转换为数值或标签", featureKey, rawValue);
                    }
                }
            }

            // 处理 class 字段
            if (record.containsKey("class") && classLabels != null) {
                try {
                    int classId = Integer.parseInt(record.get("class").toString());
                    if (classId >= 0 && classId < classLabels.size()) {
                        record.put("class_label", classLabels.get(classId));
                    }
                } catch (NumberFormatException e) {
                    log.warn("class 字段无法转换为整数: {}", record.get("class"));
                }
            }
        }
        return rawData;
    }

    /**
     * 从 XgboostService 获取特征中文名映射
     */
    private Map<String, String> buildFeatureNameMap() {
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> feat : xgboostService.getAllFeatures()) {
            map.put((String) feat.get("key"), (String) feat.get("name"));
        }
        return map;
    }
}