package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.*;

@Service
public class ComparisonService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] METRICS = {
        "sleep_duration_hrs", "sleep_quality_score", "rem_percentage",
        "deep_sleep_percentage", "sleep_latency_mins", "wake_episodes_per_night"
    };

    private static final String[] METRIC_LABELS = {
        "睡眠时长(h)", "睡眠质量评分", "REM睡眠占比(%)",
        "深度睡眠占比(%)", "入睡潜伏期(min)", "夜间觉醒次数"
    };

    @Cacheable(value = "comparisonData", key = "#dimension")
    public Map<String, Object> getComparisonData(String dimension) {
        Map<String, Object> result = new HashMap<>();

        // 从聚合表获取 barChart 数据
        String barSql = "SELECT category, record_count AS count " +
                "FROM comparison_bar_" + dimension + " " +
                "ORDER BY category";
        List<Map<String, Object>> barData = jdbcTemplate.queryForList(barSql);

        List<String> categories = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map<String, Object> row : barData) {
            String cat = String.valueOf(row.get("category"));
            categories.add(cat);
            counts.add(((Number) row.get("count")).intValue());
        }

        Map<String, Object> barChart = new HashMap<>();
        barChart.put("categories", categories);
        barChart.put("counts", counts);
        result.put("barChart", barChart);

        // 从聚合表获取各指标均值
        Map<String, List<Double>> metricAverages = new HashMap<>();
        Map<String, Double> metricMaxValues = new HashMap<>();

        for (String metric : METRICS) {
            String avgSql = "SELECT category, metric_avg " +
                    "FROM comparison_metrics_" + dimension + " " +
                    "WHERE metric_name = ? " +
                    "ORDER BY category";
            List<Map<String, Object>> avgData = jdbcTemplate.queryForList(avgSql, metric);
            List<Double> values = new ArrayList<>();
            for (Map<String, Object> row : avgData) {
                Double val = ((Number) row.get("metric_avg")).doubleValue();
                values.add(Math.round(val * 100) / 100.0);
            }
            metricAverages.put(metric, values);

            // max值从原始数据查询
            Double maxVal = jdbcTemplate.queryForObject(
                "SELECT MAX(" + metric + ") FROM sleep_health_dataset", Double.class);
            metricMaxValues.put(metric, maxVal != null ? maxVal : 100.0);
        }
        result.put("metricAverages", metricAverages);
        result.put("metricMaxValues", metricMaxValues);
        result.put("categories", categories);
        result.put("metricLabels", Arrays.asList(METRIC_LABELS));

        return result;
    }
}
