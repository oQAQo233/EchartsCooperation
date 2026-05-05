package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HeatmapService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 数值字段白名单（用于 range 分桶，防 SQL 注入）
     * key：前端允许传入的 field 名
     * value：数据库真实列名
     */
    private static final Map<String, String> NUMERIC_FIELD_WHITELIST = new HashMap<>();
    static {
        NUMERIC_FIELD_WHITELIST.put("sleep_duration_hrs", "sleep_duration_hrs");
        NUMERIC_FIELD_WHITELIST.put("sleep_quality_score", "sleep_quality_score");
        NUMERIC_FIELD_WHITELIST.put("stress_score", "stress_score");
        NUMERIC_FIELD_WHITELIST.put("room_temperature_celsius", "room_temperature_celsius");
        NUMERIC_FIELD_WHITELIST.put("weekend_sleep_diff_hrs", "weekend_sleep_diff_hrs");
        NUMERIC_FIELD_WHITELIST.put("sleep_latency_mins", "sleep_latency_mins");
        NUMERIC_FIELD_WHITELIST.put("wake_episodes_per_night", "wake_episodes_per_night");
        NUMERIC_FIELD_WHITELIST.put("bmi", "bmi");
        NUMERIC_FIELD_WHITELIST.put("heart_rate_resting_bpm", "heart_rate_resting_bpm");
        NUMERIC_FIELD_WHITELIST.put("age", "age");
    }

    /**
     * 从预聚合表获取热力图（睡眠时长 × 睡眠质量）
     */
    public Map<String, Object> getSleepDurationVsQualityHeatmap() {
        String sql = "SELECT duration_bucket, quality_bucket, record_count AS value " +
                "FROM agg_heatmap_duration_quality " +
                "ORDER BY duration_bucket ASC, quality_bucket ASC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> yLabels = Arrays.asList("1-3", "4-5", "6-7", "8-10");

        // 动态构建 xLabels
        Map<String, Integer> xLabelToIndex = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            int bucket = ((Number) r.get("duration_bucket")).intValue();
            String xLabel = bucket + "-" + (bucket + 1);
            xLabelToIndex.putIfAbsent(xLabel, xLabelToIndex.size());
        }
        List<String> xLabels = new ArrayList<>(xLabelToIndex.keySet());

        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            int bucket = ((Number) r.get("duration_bucket")).intValue();
            String xLabel = bucket + "-" + (bucket + 1);
            String yLabel = String.valueOf(r.get("quality_bucket"));
            Number value = (Number) r.get("value");

            Integer xIndex = xLabelToIndex.get(xLabel);
            int yIndex = yLabels.indexOf(yLabel);
            if (xIndex == null || yIndex < 0) continue;

            data.add(Arrays.asList(xIndex, yIndex, value.intValue()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("title", "睡眠时长 × 睡眠质量（人数）");
        result.put("xLabels", xLabels);
        result.put("yLabels", yLabels);
        result.put("data", data);
        return result;
    }

    /**
     * 动态范围热力图 - 保留动态SQL因为参数是动态的
     */
    public Map<String, Object> getRangeHeatmap(
            String xField, String yField,
            double xStart, double xEnd, double xStep,
            double yStart, double yEnd, double yStep,
            String title) {
        validateRangeParams("x", xStart, xEnd, xStep);
        validateRangeParams("y", yStart, yEnd, yStep);

        String xCol = mapNumericField(xField);
        String yCol = mapNumericField(yField);

        int xBins = (int) Math.ceil((xEnd - xStart) / xStep);
        int yBins = (int) Math.ceil((yEnd - yStart) / yStep);

        List<String> xLabels = buildRangeLabels(xStart, xStep, xBins);
        List<String> yLabels = buildRangeLabels(yStart, yStep, yBins);

        String sql = "SELECT " +
                "  FLOOR((" + xCol + " - ?) / ?) AS x_idx, " +
                "  FLOOR((" + yCol + " - ?) / ?) AS y_idx, " +
                "  COUNT(*) AS value " +
                "FROM sleep_health_dataset " +
                "WHERE " + xCol + " IS NOT NULL AND " + yCol + " IS NOT NULL " +
                "  AND " + xCol + " >= ? AND " + xCol + " < ? " +
                "  AND " + yCol + " >= ? AND " + yCol + " < ? " +
                "GROUP BY x_idx, y_idx ORDER BY x_idx, y_idx";

        Object[] params = {
                xStart, xStep,
                yStart, yStep,
                xStart, xEnd,
                yStart, yEnd
        };

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Number xiN = (Number) r.get("x_idx");
            Number yiN = (Number) r.get("y_idx");
            Number vN = (Number) r.get("value");
            if (xiN == null || yiN == null || vN == null) continue;

            int xi = xiN.intValue();
            int yi = yiN.intValue();
            if (xi < 0 || xi >= xBins || yi < 0 || yi >= yBins) continue;

            data.add(Arrays.asList(xi, yi, vN.intValue()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("title", (title != null && !title.trim().isEmpty())
                ? title
                : String.format("%s(range) × %s(range)（人数）", xField, yField));
        result.put("xLabels", xLabels);
        result.put("yLabels", yLabels);
        result.put("data", data);
        return result;
    }

    // ===================== helpers =====================

    private void validateRangeParams(String axis, double start, double end, double step) {
        if (Double.isNaN(start) || Double.isNaN(end) || Double.isNaN(step)) {
            throw new IllegalArgumentException(axis + " 参数不能为 NaN");
        }
        if (step <= 0) {
            throw new IllegalArgumentException(axis + "Step 必须 > 0");
        }
        if (end <= start) {
            throw new IllegalArgumentException(axis + "End 必须 > " + axis + "Start");
        }

        double bins = Math.ceil((end - start) / step);
        if (bins > 200) {
            throw new IllegalArgumentException(axis + " 分桶数量过大(" + (int) bins + ")，请增大 step 或缩小范围");
        }
    }

    private String mapNumericField(String field) {
        String col = NUMERIC_FIELD_WHITELIST.get(field);
        if (col == null) {
            throw new IllegalArgumentException("不支持的数值字段: " + field);
        }
        return col;
    }

    private List<String> buildRangeLabels(double start, double step, int bins) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < bins; i++) {
            double a = start + i * step;
            double b = start + (i + 1) * step;
            labels.add(formatNumber(a) + "-" + formatNumber(b));
        }
        return labels;
    }

    private String formatNumber(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) {
            return String.valueOf((long) Math.round(v));
        }
        String s = String.format(java.util.Locale.US, "%.2f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
