package edu.jlu.services;

import edu.jlu.models.BarChartData;
import edu.jlu.models.ComparisonResult;
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
    public ComparisonResult getComparisonData(String dimension) {
        ComparisonResult result = new ComparisonResult();

        String binSql = buildBinSql(dimension);
        String barSql = String.format(
            "SELECT %s AS category, COUNT(*) AS count " +
            "FROM sub_comparison_data GROUP BY %s ORDER BY category",
            binSql, binSql
        );
        List<Map<String, Object>> barData = jdbcTemplate.queryForList(barSql);

        List<String> categories = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map<String, Object> row : barData) {
            String cat = String.valueOf(row.get("category"));
            categories.add(cat);
            counts.add(((Number) row.get("count")).intValue());
        }

        BarChartData barChart = new BarChartData();
        barChart.setCategories(categories);
        barChart.setCounts(counts);
        result.setBarChart(barChart);

        Map<String, List<Double>> metricAverages = new HashMap<>();
        Map<String, Double> metricMaxValues = new HashMap<>();

        for (String metric : METRICS) {
            String avgSql = String.format(
                "SELECT %s AS category, AVG(%s) AS avg_value " +
                "FROM sub_comparison_data GROUP BY %s ORDER BY category",
                binSql, metric, binSql
            );
            List<Map<String, Object>> avgData = jdbcTemplate.queryForList(avgSql);
            List<Double> values = new ArrayList<>();
            for (Map<String, Object> row : avgData) {
                Double val = ((Number) row.get("avg_value")).doubleValue();
                values.add(Math.round(val * 100) / 100.0);
            }
            metricAverages.put(metric, values);

            Double maxVal = jdbcTemplate.queryForObject(
                "SELECT MAX(" + metric + ") FROM sub_comparison_data", Double.class);
            metricMaxValues.put(metric, maxVal != null ? maxVal : 100.0);
        }
        result.setMetricAverages(metricAverages);
        result.setMetricMaxValues(metricMaxValues);
        result.setCategories(categories);
        result.setMetricLabels(Arrays.asList(METRIC_LABELS));

        return result;
    }

    private String buildBinSql(String dimension) {
        switch (dimension) {
            case "age":
                return "CASE " +
                       "  WHEN age < 25 THEN '<25' " +
                       "  WHEN age < 35 THEN '25-34' " +
                       "  WHEN age < 45 THEN '35-44' " +
                       "  WHEN age < 55 THEN '45-54' " +
                       "  WHEN age < 65 THEN '55-64' " +
                       "  ELSE '≥65' END";
            case "gender":
                return "gender";
            case "occupation":
                return "occupation";
            case "bmi":
                return "CASE " +
                       "  WHEN bmi < 18.5 THEN '偏瘦' " +
                       "  WHEN bmi < 25 THEN '正常' " +
                       "  WHEN bmi < 30 THEN '超重' " +
                       "  ELSE '肥胖' END";
            case "country":
                return "country";
            default:
                throw new IllegalArgumentException("Unknown dimension: " + dimension);
        }
    }
}
