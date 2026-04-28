package edu.jlu.services;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service // 声明服务类，交给 Spring 容器管理
public class XgboostService {

    private static final Map<String, Map<String, Object>> OUTPUT_CONFIG = new LinkedHashMap<>();
    // 静态常量，存储每个目标变量（即模型要预测的列）的配置。
    // 外层 Map 的 key 是目标变量名（字符串），value 是它的元信息（名称、类型等）。
    // 使用 LinkedHashMap 保持插入顺序，展示时有固定序。

    static { // 定义模型（目标变量）及其输出配置
        Map<String, Object> cfgCog = new HashMap<>();
        cfgCog.put("name", "认知表现评分预测");
        cfgCog.put("type", "regression");
        OUTPUT_CONFIG.put("cognitive_performance_score", cfgCog);

        Map<String, Object> cfgRisk = new HashMap<>();
        cfgRisk.put("name", "睡眠障碍风险评估");
        cfgRisk.put("type", "regression");
        cfgRisk.put("labels", Arrays.asList("Healthy", "Mild", "Moderate", "Severe"));
        OUTPUT_CONFIG.put("sleep_disorder_risk", cfgRisk);

        Map<String, Object> cfgMental = new HashMap<>();
        cfgMental.put("name", "心理健康状况预测");
        cfgMental.put("type", "classification");
        cfgMental.put("labels", Arrays.asList("Healthy", "Anxiety", "Depression", "Both"));
        OUTPUT_CONFIG.put("mental_health_condition", cfgMental);

        Map<String, Object> cfgDur = new HashMap<>();
        cfgDur.put("name", "总睡眠时长预测");
        cfgDur.put("type", "regression");
        OUTPUT_CONFIG.put("sleep_duration_hrs", cfgDur);

        Map<String, Object> cfgLat = new HashMap<>();
        cfgLat.put("name", "入睡潜伏期预测");
        cfgLat.put("type", "regression");
        OUTPUT_CONFIG.put("sleep_latency_mins", cfgLat);

        Map<String, Object> cfgRested = new HashMap<>();
        cfgRested.put("name", "是否休息充分预测");
        cfgRested.put("type", "classification");
        cfgRested.put("labels", Arrays.asList("未恢复", "休息好了"));
        OUTPUT_CONFIG.put("felt_rested", cfgRested);

        Map<String, Object> cfgOcc = new HashMap<>();
        cfgOcc.put("name", "职业预测");
        cfgOcc.put("type", "classification");
        OUTPUT_CONFIG.put("occupation", cfgOcc);
    }

    private static final Map<String, List<String>> MASK_FEATURE_LIST = new LinkedHashMap<>();
    // 每个模型需要屏蔽（mask）的特征列表。当选择一个模型进行预测时，这些特征在输入表单中不应出现

    static { // 配置 Mask 特征
        MASK_FEATURE_LIST.put("cognitive_performance_score",
                Arrays.asList("sleep_disorder_risk", "felt_rested", "sleep_quality_score"));
        MASK_FEATURE_LIST.put("sleep_disorder_risk",
                Arrays.asList("cognitive_performance_score", "felt_rested", "sleep_quality_score"));
        MASK_FEATURE_LIST.put("mental_health_condition",
                Arrays.asList("cognitive_performance_score", "sleep_disorder_risk"));
        MASK_FEATURE_LIST.put("sleep_duration_hrs",
                Arrays.asList("cognitive_performance_score", "sleep_disorder_risk", "rem_percentage", "deep_sleep_percentage", "sleep_latency_mins"));
        MASK_FEATURE_LIST.put("sleep_latency_mins",
                Arrays.asList("cognitive_performance_score", "sleep_disorder_risk", "rem_percentage", "deep_sleep_percentage", "sleep_duration_hrs"));
        MASK_FEATURE_LIST.put("felt_rested",
                Arrays.asList("cognitive_performance_score", "sleep_disorder_risk"));
        MASK_FEATURE_LIST.put("occupation",
                Arrays.asList());
    }

    private static final List<Map<String, Object>> ALL_FEATURES;
    // 存储全部特征元数据的列表，每个特征用一个 Map 表示（key, name, type, options 等）

    static {
        ALL_FEATURES = new ArrayList<>();
        // 个人身体指标/特质(7)
        addFeature("age", "年龄", "numeric", null);
        addFeature("gender", "性别", "categorical", Arrays.asList(
                option("男", 0), option("女", 1), option("其他", 2)));
        addFeature("chronotype", "睡眠类型", "categorical", Arrays.asList(
                option("Morning", 0), option("Evening", 1), option("Neutral", 2)));
        addFeature("mental_health_condition", "心理健康状况", "categorical", Arrays.asList(
                option("Healthy", 0), option("Anxiety", 1), option("Depression", 2), option("Both", 3)));
        addFeature("stress_score", "心理压力评分", "numeric", null);
        addFeature("heart_rate_resting_bpm", "安静心率", "numeric", null);
        addFeature("bmi", "BMI", "numeric", null);

        // 环境因素(6)
        addFeature("occupation", "职业", "categorical", Arrays.asList(
                option("Student", 0), option("Manager", 1), option("Doctor", 2),
                option("Teacher", 3), option("Nurse", 4), option("Sales", 5),
                option("Lawyer", 6), option("Software Engineer", 7), option("Driver", 8),
                option("Freelancer", 9), option("Retired", 10), option("Homemaker", 11)));
        addFeature("shift_work", "是否轮班工作", "categorical", Arrays.asList(
                option("否", 0), option("是", 1)));
        addFeature("country", "国家", "categorical", Arrays.asList(
                option("Canada", 0), option("USA", 1), option("UK", 2),
                option("Japan", 3), option("Brazil", 4), option("Italy", 5),
                option("India", 6), option("Germany", 7), option("South Korea", 8),
                option("Australia", 9), option("France", 10), option("Sweden", 11),
                option("Netherlands", 12), option("Spain", 13), option("Mexico", 14)));
        addFeature("season", "季节", "categorical", Arrays.asList(
                option("Spring", 0), option("Summer", 1), option("Autumn", 2), option("Winter", 3)));
        addFeature("room_temperature_celsius", "卧室温度", "numeric", null);
        addFeature("day_type", "日期类型", "categorical", Arrays.asList(
                option("Weekday", 0), option("Weekend", 1)));

        // 行为因素(8)
        addFeature("caffeine_mg_before_bed", "睡前咖啡因摄入量", "numeric", null);
        addFeature("alcohol_units_before_bed", "睡前酒精摄入量", "numeric", null);
        addFeature("screen_time_before_bed_mins", "睡前屏幕使用时间", "numeric", null);
        addFeature("exercise_day", "当天是否运动", "categorical", Arrays.asList(
                option("否", 0), option("是", 1)));
        addFeature("steps_that_day", "当天行走步数", "numeric", null);
        addFeature("nap_duration_mins", "日间小睡时长", "numeric", null);
        addFeature("work_hours_that_day", "当天工作/学习小时数", "numeric", null);
        addFeature("sleep_aid_used", "是否使用助眠药物", "categorical", Arrays.asList(
                option("否", 0), option("是", 1)));

        // 睡眠相关指标(8)
        addFeature("sleep_duration_hrs", "总睡眠时长", "numeric", null);
        addFeature("sleep_quality_score", "睡眠质量主观评分", "numeric", null);
        addFeature("rem_percentage", "REM占比", "numeric", null);
        addFeature("deep_sleep_percentage", "深度睡眠占比", "numeric", null);
        addFeature("sleep_latency_mins", "入睡潜伏期", "numeric", null);
        addFeature("wake_episodes_per_night", "夜间醒来次数", "numeric", null);
        addFeature("weekend_sleep_diff_hrs", "周末/工作日睡眠时长差", "numeric", null);
        addFeature("felt_rested", "感觉是否休息充分", "categorical", Arrays.asList(
                option("未恢复", 0), option("休息好了", 1)));

        // 明显目标列(2)
        addFeature("cognitive_performance_score", "认知能力评分", "numeric", null);
        addFeature("sleep_disorder_risk", "睡眠情况异常风险", "numeric", null);

    }

    private static Map<String, Object> option(String label, int value) {
        // 静态方法（在静态初始化中使用）：创建一个表示分类选项的 Map，包含显示标签 label 和对应的编码值 value
        Map<String, Object> map = new HashMap<>();
        map.put("label", label);
        map.put("value", value);
        return map;
    }

    private static void addFeature(String key, String name, String type, List<Map<String, Object>> options) {
        // 静态方法（在静态初始化中使用）：添加特征到全特征池
        Map<String, Object> feat = new HashMap<>();
        feat.put("key", key);
        feat.put("name", name);
        feat.put("type", type);
        if (options != null) {
            feat.put("options", options);
        }
        ALL_FEATURES.add(feat);
    }

    private final Map<String, Booster> boosterCache = new HashMap<>();
    // 实例级别的缓存，存储已加载的 Booster 模型对象，避免重复从文件加载，提高预测速度

    /**
     * 获取缓存的Booster，若无则从 classpath:models/{modelId}.json 加载
     */
    private Booster getCachedBooster(String modelId) throws IOException, XGBoostError {
        if (boosterCache.containsKey(modelId)) {
            return boosterCache.get(modelId);
        }
        ClassPathResource resource = new ClassPathResource("models/" + modelId + ".json");
        try (InputStream inputStream = resource.getInputStream()) {
            Booster booster = XGBoost.loadModel(inputStream);
            boosterCache.put(modelId, booster);
            return booster;
        }
    }

    public static class PreprocessParams {
        List<String> numericColumns; // 数值列名列表，顺序与 mean/std 对应
        double[] mean;
        double[] std;
        List<String> featureOrder; // 所有特征列的顺序（与训练时一致）
    } // 内部类：存储模型的归一化参数和特征顺序

    /**
     * 加载预处理参数文件 (models/{modelId}_scaler)
     */
    public PreprocessParams loadPreprocessParams(String modelId) throws IOException {
        String resourcePath = "models/" + modelId + "_scaler";
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            // 读取 JSON 为 Map
            Map<String, Object> config = mapper.readValue(is, Map.class);

            PreprocessParams params = new PreprocessParams();
            params.numericColumns = (List<String>) config.get("numeric_columns");
            List<Double> meanList = (List<Double>) config.get("mean");
            List<Double> stdList = (List<Double>) config.get("std");
            params.mean = meanList.stream().mapToDouble(Double::doubleValue).toArray();
            params.std = stdList.stream().mapToDouble(Double::doubleValue).toArray();

            // 如果 Python 端也导出了 feature_order（强烈建议），则加载；否则为 null
            if (config.containsKey("feature_order")) {
                params.featureOrder = (List<String>) config.get("feature_order");
            } else {
                params.featureOrder = null;
            }
            return params;
        }
    }

    /**
     * 获取全部特征元数据（包含 key, name, type, options）
     */
    public List<Map<String, Object>> getAllFeatures() {
        return ALL_FEATURES;
    }

    /**
     * 获取指定模型的输出配置（包含 name, type, labels 等）
     * 若未显式设置 labels 且模型为分类任务，则尝试从 ALL_FEATURES 中自动推断标签
     */
    public Map<String, Object> getModelOutputConfig(String modelId) {
        Map<String, Object> cfg = OUTPUT_CONFIG.get(modelId);
        if (cfg == null) {
            return null;
        }
        // 若未设置 labels 且类型为 classification，则尝试推断
        if ((!cfg.containsKey("labels") || cfg.get("labels") == null)
                && "classification".equals(cfg.get("type"))) {
            // 尝试从 ALL_FEATURES 中找到对应目标列的 options
            for (Map<String, Object> feat : ALL_FEATURES) {
                if (modelId.equals(feat.get("key")) && feat.containsKey("options")) {
                    List<Map<String, Object>> options = (List<Map<String, Object>>) feat.get("options");
                    List<String> labels = new ArrayList<>();
                    for (Map<String, Object> opt : options) {
                        labels.add((String) opt.get("label"));
                    }
                    cfg.put("labels", labels);
                    break;
                }
            }
        }
        return cfg;
    }

    private final Map<String, PreprocessParams> preprocessCache = new HashMap<>();

    /**
     * 获取缓存的预处理参数，若无则加载
     */
    private PreprocessParams getCachedPreprocessParams(String modelId) throws IOException {
        if (preprocessCache.containsKey(modelId)) {
            return preprocessCache.get(modelId);
        }
        PreprocessParams params = loadPreprocessParams(modelId);
        preprocessCache.put(modelId, params);
        return params;
    }

    /**
     * 获取可用模型列表
     */
    public List<Map<String, String>> getAvailableModels() {
        List<Map<String, String>> models = new ArrayList<>();
        for (String modelId : OUTPUT_CONFIG.keySet()) {
            // 遍历 OUTPUT_CONFIG 的所有 key（模型 ID），为每个模型创建一个包含 id 和 name 的 Map
            Map<String, String> model = new HashMap<>();
            model.put("id", modelId);
            model.put("name", (String) OUTPUT_CONFIG.get(modelId).get("name"));
            models.add(model);
        }
        return models; // 返回给控制器/前端，用于下拉选择预测目标
    }

    /**
     * 根据模型ID获取特征元数据（过滤掉目标和mask列）
     */
    public List<Map<String, Object>> getFeatureMetas(String modelId) {
        List<String> mask = MASK_FEATURE_LIST.get(modelId);
        if (mask == null) {
            throw new IllegalArgumentException("模型不存在: " + modelId);
        }
        Set<String> excludeKeys = new HashSet<>(mask);
        excludeKeys.add(modelId); // 目标列也需要排除
        return ALL_FEATURES.stream()
                .filter(f -> !excludeKeys.contains(f.get("key")))
                .collect(Collectors.toList());
        // 根据 modelId 获取该模型需要屏蔽的特征键列表（以及目标列本身），确保这些特征不会出现在预测表单中
    }

    /**
     * 执行预测
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> predict(Map<String, Object> requestData) {
        String modelId = (String) requestData.get("modelId");
        Map<String, Object> featuresMap = (Map<String, Object>) requestData.get("features");
        if (modelId == null || featuresMap == null) {
            throw new IllegalArgumentException("请求参数缺少 modelId 或 features");
        }

        List<Map<String, Object>> featureMetas = getFeatureMetas(modelId);
        if (featuresMap.size() != featureMetas.size()) {
            throw new IllegalArgumentException("特征数量不匹配，需要 " + featureMetas.size() + " 个特征");
        }

        // ----- 新增：加载预处理参数 -----
        PreprocessParams preParams;
        try {
            preParams = getCachedPreprocessParams(modelId);
        } catch (IOException e) {
            throw new RuntimeException("无法加载模型预处理参数: " + e.getMessage(), e);
        }

        // 如果 Python 端提供了 featureOrder，则按训练列顺序重排特征值
        // 否则按 featureMetas 的顺序处理（需保证该顺序与训练一致）
        List<String> effectiveFeatureOrder;
        if (preParams.featureOrder != null) {
            effectiveFeatureOrder = preParams.featureOrder;
        } else {
            // 退化：使用 featureMetas 的 key 顺序
            effectiveFeatureOrder = featureMetas.stream()
                    .map(m -> (String) m.get("key"))
                    .collect(Collectors.toList());
        }

        // 构建数值列索引映射（列名 -> 在 mean/std 数组中的位置）
        Map<String, Integer> numColIndexMap = new HashMap<>();
        if (preParams.numericColumns != null) {
            for (int i = 0; i < preParams.numericColumns.size(); i++) {
                numColIndexMap.put(preParams.numericColumns.get(i), i);
            }
        }

        // 按照 effectiveFeatureOrder 顺序构造特征数组，同时标准化数值特征
        float[] featuresArray = new float[effectiveFeatureOrder.size()];
        int idx = 0;
        for (String key : effectiveFeatureOrder) {
            if (!featuresMap.containsKey(key)) {
                // 理论上不会发生，因为前端会发送所有需要的特征
                throw new IllegalArgumentException("缺少特征: " + key);
            }
            Object val = featuresMap.get(key);
            float rawValue;
            try {
                rawValue = Float.parseFloat(val.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("特征 '" + key + "' 的值不是有效数字: " + val);
            }

            // 如果是数值列且预处理参数中有它，进行标准化
            // 需要知道该列是否是数值类型，简单判断：存在于 numericColumns 列表即认为是数值列
            if (numColIndexMap.containsKey(key)) {
                int numIdx = numColIndexMap.get(key);
                double mean = preParams.mean[numIdx];
                double std = preParams.std[numIdx];
                if (std != 0.0) {
                    rawValue = (float) ((rawValue - mean) / std);
                } else {
                    // 标准差为0时，该列没变异，标准化为0（避免除零）
                    rawValue = 0.0f;
                }
            }
            featuresArray[idx++] = rawValue;
        }

        // ----- 预测逻辑（原样） -----
        try {
            Booster booster = getCachedBooster(modelId);
            DMatrix dmatrix = new DMatrix(featuresArray, 1, featuresArray.length, Float.NaN);
            System.out.println("Java preprocessed features: " + Arrays.toString(featuresArray));
            float[][] predictions = booster.predict(dmatrix);
            float rawPred = predictions[0][0];

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("modelId", modelId);
            result.put("modelName", OUTPUT_CONFIG.get(modelId).get("name"));

            Map<String, Object> outCfg = OUTPUT_CONFIG.get(modelId);
            String type = (String) outCfg.get("type");
            if ("classification".equals(type) && outCfg.containsKey("labels")) {
                List<String> labels = (List<String>) outCfg.get("labels");
                int classIdx = Math.round(rawPred);
                if (classIdx >= 0 && classIdx < labels.size()) {
                    result.put("predictionLabel", labels.get(classIdx));
                    result.put("prediction", classIdx);
                } else {
                    result.put("prediction", rawPred);
                }
            } else {
                result.put("prediction", rawPred);
            }
            return result;
        } catch (XGBoostError | IOException e) {
            throw new RuntimeException("模型预测失败: " + e.getMessage(), e);
        }
    }

}