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
        // 只允许数值列（按你的数据集可增删）
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
     * 兼容保留：原来的默认热力图（睡眠时长 × 睡眠质量）
     */
    public Map<String, Object> getSleepDurationVsQualityHeatmap() {
        String sql =
                "SELECT " +
                        "  FLOOR(sleep_duration_hrs) AS x_bucket, " +
                        "  CONCAT(FLOOR(sleep_duration_hrs), '-', FLOOR(sleep_duration_hrs) + 1) AS x_label, " +
                        "  CASE " +
                        "    WHEN sleep_quality_score <= 3 THEN '1-3' " +
                        "    WHEN sleep_quality_score <= 5 THEN '4-5' " +
                        "    WHEN sleep_quality_score <= 7 THEN '6-7' " +
                        "    ELSE '8-10' END AS y_label, " +
                        "  COUNT(*) AS value " +
                        "FROM sleep_health_dataset " +
                        "WHERE sleep_duration_hrs IS NOT NULL AND sleep_quality_score IS NOT NULL " +
                        "GROUP BY x_bucket, x_label, y_label " +
                        "ORDER BY x_bucket ASC, y_label ASC";

        List<String> yLabels = Arrays.asList("1-3", "4-5", "6-7", "8-10");
        return buildHeatmapDynamicX("睡眠时长 × 睡眠质量（人数）", sql, "x_label", yLabels, "y_label");
    }

    /**
     * 新增：range 分桶热力图（细化分桶用）
     *
     * 分桶区间：左闭右开 [start, end)
     * 超出范围：直接过滤（2A）
     */
    public Map<String, Object> getRangeHeatmap(
            String xField, String yField,
            double xStart, double xEnd, double xStep,
            double yStart, double yEnd, double yStep,
            String title
    ) {
        validateRangeParams("x", xStart, xEnd, xStep);
        validateRangeParams("y", yStart, yEnd, yStep);

        String xCol = mapNumericField(xField);
        String yCol = mapNumericField(yField);

        int xBins = (int) Math.ceil((xEnd - xStart) / xStep);
        int yBins = (int) Math.ceil((yEnd - yStart) / yStep);

        List<String> xLabels = buildRangeLabels(xStart, xStep, xBins);
        List<String> yLabels = buildRangeLabels(yStart, yStep, yBins);

        String sql =
                "SELECT " +
                        "  FLOOR((" + xCol + " - ?) / ?) AS x_idx, " +
                        "  FLOOR((" + yCol + " - ?) / ?) AS y_idx, " +
                        "  COUNT(*) AS value " +
                        "FROM sleep_health_dataset " +
                        "WHERE " + xCol + " IS NOT NULL AND " + yCol + " IS NOT NULL " +
                        "  AND " + xCol + " >= ? AND " + xCol + " < ? " +
                        "  AND " + yCol + " >= ? AND " + yCol + " < ? " +
                        "GROUP BY x_idx, y_idx " +
                        "ORDER BY x_idx, y_idx";

        Object[] params = new Object[] {
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
            Number vN  = (Number) r.get("value");
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
        if (bins > 200) { // 保护阈值：避免 step 太小导致 bins 巨大
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
        // 最多 2 位小数，去掉尾随 0
        String s = String.format(java.util.Locale.US, "%.2f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /**
     * 你原来那套：xLabels 动态提取（按 SQL 返回顺序去重），yLabels 固定。
     */
    private Map<String, Object> buildHeatmapDynamicX(
            String title,
            String sql,
            String xLabelKey,
            List<String> yLabels,
            String yLabelKey
    ) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        Map<String, Integer> xLabelToIndex = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String xLabel = String.valueOf(r.get(xLabelKey));
            xLabelToIndex.putIfAbsent(xLabel, xLabelToIndex.size());
        }
        List<String> xLabels = new ArrayList<>(xLabelToIndex.keySet());

        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String xLabel = String.valueOf(r.get(xLabelKey));
            String yLabel = String.valueOf(r.get(yLabelKey));
            Number value = (Number) r.get("value");

            Integer xIndex = xLabelToIndex.get(xLabel);
            int yIndex = yLabels.indexOf(yLabel);
            if (xIndex == null || yIndex < 0) continue;

            data.add(Arrays.asList(xIndex, yIndex, value.intValue()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("xLabels", xLabels);
        result.put("yLabels", yLabels);
        result.put("data", data);
        return result;
    }
}