package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class HeatmapService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> getSleepDurationVsQualityHeatmap() {
        String sql = "SELECT " +
                "ROUND(sleep_duration_hrs) as duration_bucket, " +
                "ROUND(sleep_quality_score) as quality_bucket, " +
                "AVG(sleep_quality_score) as value, " +
                "COUNT(*) as count " +
                "FROM sleep_health_record " +
                "WHERE sleep_duration_hrs IS NOT NULL AND sleep_quality_score IS NOT NULL " +
                "GROUP BY ROUND(sleep_duration_hrs), ROUND(sleep_quality_score) " +
                "ORDER BY duration_bucket, quality_bucket";

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> heatmapData = new ArrayList<>();

        for (Map<String, Object> row : rawData) {
            Map<String, Object> cell = new HashMap<>();
            cell.put("x", ((Number) row.get("duration_bucket")).doubleValue());
            cell.put("y", ((Number) row.get("quality_bucket")).doubleValue());
            cell.put("value", ((Number) row.get("value")).doubleValue());
            cell.put("count", ((Number) row.get("count")).intValue());
            heatmapData.add(cell);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", heatmapData);
        result.put("xField", "sleepDurationHrs");
        result.put("yField", "sleepQualityScore");
        result.put("title", "睡眠时长 vs 睡眠质量");
        return result;
    }

    public Map<String, Object> getRangeHeatmap(
            String xField,
            String yField,
            double xStart,
            double xEnd,
            double xStep,
            double yStart,
            double yEnd,
            double yStep,
            String title
    ) {
        String sql = "SELECT " +
                "FLOOR((" + xField + " - ?) / ?) * ? + ? as x_bucket, " +
                "FLOOR((" + yField + " - ?) / ?) * ? + ? as y_bucket, " +
                "AVG(" + yField + ") as value, " +
                "COUNT(*) as count " +
                "FROM sleep_health_record " +
                "WHERE " + xField + " IS NOT NULL AND " + yField + " IS NOT NULL " +
                "AND " + xField + " >= ? AND " + xField + " < ? " +
                "AND " + yField + " >= ? AND " + yField + " < ? " +
                "GROUP BY FLOOR((" + xField + " - ?) / ?) * ? + ?, " +
                "FLOOR((" + yField + " - ?) / ?) * ? + ? " +
                "ORDER BY x_bucket, y_bucket";

        List<Object> params = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            params.add(xStart);
            params.add(xStep);
            params.add(xStep);
            params.add(xStart);
            params.add(yStart);
            params.add(yStep);
            params.add(yStep);
            params.add(yStart);
            params.add(xStart);
            params.add(xEnd);
            params.add(yStart);
            params.add(yEnd);
        }
        params.addAll(params.subList(0, params.size()));

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(sql, params.toArray());
        List<Map<String, Object>> heatmapData = new ArrayList<>();

        for (Map<String, Object> row : rawData) {
            Map<String, Object> cell = new HashMap<>();
            cell.put("x", ((Number) row.get("x_bucket")).doubleValue());
            cell.put("y", ((Number) row.get("y_bucket")).doubleValue());
            cell.put("value", ((Number) row.get("value")).doubleValue());
            cell.put("count", ((Number) row.get("count")).intValue());
            heatmapData.add(cell);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", heatmapData);
        result.put("xField", xField);
        result.put("yField", yField);
        result.put("xStart", xStart);
        result.put("xEnd", xEnd);
        result.put("xStep", xStep);
        result.put("yStart", yStart);
        result.put("yEnd", yEnd);
        result.put("yStep", yStep);
        result.put("title", title != null ? title : xField + " vs " + yField);
        return result;
    }
}
