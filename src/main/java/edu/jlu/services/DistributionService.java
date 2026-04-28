package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DistributionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> getDistributionData(String innerType, String outerType) {
        String innerBin = buildInnerBinSql(innerType);
        String outerBin = buildOuterBinSql(outerType);

        String sql = String.format(
            "SELECT %s AS inner_name, %s AS outer_name, COUNT(*) AS value " +
            "FROM sleep_health_dataset GROUP BY %s, %s ORDER BY inner_name, outer_name",
            innerBin, outerBin, innerBin, outerBin
        );

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(sql);

        // innerList: aggregate by inner_name
        Map<String, Integer> innerCounts = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String innerName = String.valueOf(row.get("inner_name"));
            int cnt = ((Number) row.get("value")).intValue();
            innerCounts.merge(innerName, cnt, Integer::sum);
        }

        List<Map<String, Object>> innerList = new ArrayList<>();
        for (Map.Entry<String, Integer> e : innerCounts.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            innerList.add(item);
        }

        // outerList: NxM rows with inner_name + outer_name + value
        List<Map<String, Object>> outerList = new ArrayList<>();
        for (Map<String, Object> row : rawData) {
            Map<String, Object> item = new HashMap<>();
            item.put("inner_name", row.get("inner_name"));
            item.put("outer_name", row.get("outer_name"));
            item.put("value", row.get("value"));
            outerList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inner", innerList);
        result.put("outer", outerList);
        return result;
    }

    private String buildInnerBinSql(String type) {
        switch (type) {
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
                throw new IllegalArgumentException("Unknown inner type: " + type);
        }
    }

    private String buildOuterBinSql(String type) {
        switch (type) {
            case "sleepDuration":
                return "CONCAT(FLOOR(sleep_duration_hrs), '-', FLOOR(sleep_duration_hrs) + 1)";
            case "sleepQuality":
                return "CASE " +
                       "  WHEN sleep_quality_score <= 3 THEN '1-3' " +
                       "  WHEN sleep_quality_score <= 5 THEN '4-5' " +
                       "  WHEN sleep_quality_score <= 7 THEN '6-7' " +
                       "  ELSE '8-10' END";
            case "remPercentage":
                return "CASE " +
                       "  WHEN rem_percentage < 15 THEN '<15%' " +
                       "  WHEN rem_percentage < 20 THEN '15-20%' " +
                       "  WHEN rem_percentage < 25 THEN '20-25%' " +
                       "  ELSE '≥25%' END";
            case "deepSleep":
                return "CASE " +
                       "  WHEN deep_sleep_percentage < 20 THEN '<20%' " +
                       "  WHEN deep_sleep_percentage < 30 THEN '20-30%' " +
                       "  WHEN deep_sleep_percentage < 40 THEN '30-40%' " +
                       "  ELSE '≥40%' END";
            case "sleepLatency":
                return "CASE " +
                       "  WHEN sleep_latency_mins < 10 THEN '<10min' " +
                       "  WHEN sleep_latency_mins < 20 THEN '10-20min' " +
                       "  WHEN sleep_latency_mins < 30 THEN '20-30min' " +
                       "  ELSE '≥30min' END";
            case "wakeEpisodes":
                return "CASE " +
                       "  WHEN wake_episodes_per_night = 0 THEN '0次' " +
                       "  WHEN wake_episodes_per_night <= 2 THEN '1-2次' " +
                       "  WHEN wake_episodes_per_night <= 4 THEN '3-4次' " +
                       "  ELSE '>4次' END";
            default:
                throw new IllegalArgumentException("Unknown outer type: " + type);
        }
    }
}
