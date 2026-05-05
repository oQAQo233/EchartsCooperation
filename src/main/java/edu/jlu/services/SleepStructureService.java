package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SleepStructureService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> getAgeRange() {
        String sql = "SELECT min_age, max_age FROM agg_sleep_structure_age_range WHERE id = 1";
        Map<String, Object> row = jdbcTemplate.queryForMap(sql);
        Map<String, Object> ageRange = new HashMap<>();
        ageRange.put("minAge", ((Number) row.get("min_age")).intValue());
        ageRange.put("maxAge", ((Number) row.get("max_age")).intValue());
        return ageRange;
    }

    public Map<String, Object> getChartData(Integer minAge, Integer maxAge) {
        Map<String, Object> result = new HashMap<>();

        // 性别数据，从聚合表查询
        String genderSql = "SELECT dimension_value AS gender, avg_duration, avg_rem, avg_deep, " +
                "avg_light, avg_quality, avg_wake_episodes " +
                "FROM agg_sleep_structure_stats " +
                "WHERE dimension_type = 'gender' " +
                "ORDER BY CASE dimension_value " +
                "WHEN 'male' THEN 1 " +
                "WHEN 'female' THEN 2 " +
                "WHEN 'other' THEN 3 " +
                "ELSE 4 END";
        List<Map<String, Object>> genderData = jdbcTemplate.queryForList(genderSql);

        // 睡眠类型数据，从聚合表查询
        String chronotypeSql = "SELECT dimension_value AS chronotype, avg_duration, avg_rem, avg_deep, " +
                "avg_light, avg_quality, avg_wake_episodes " +
                "FROM agg_sleep_structure_stats " +
                "WHERE dimension_type = 'chronotype' " +
                "ORDER BY CASE dimension_value " +
                "WHEN 'Morning' THEN 1 " +
                "WHEN 'Neutral' THEN 2 " +
                "WHEN 'Evening' THEN 3 " +
                "ELSE 4 END";
        List<Map<String, Object>> chronotypeData = jdbcTemplate.queryForList(chronotypeSql);

        // 心理健康数据，从聚合表查询
        String mentalSql = "SELECT dimension_value AS mental_health_condition, avg_duration, avg_rem, avg_deep, " +
                "avg_light, avg_quality, avg_wake_episodes " +
                "FROM agg_sleep_structure_stats " +
                "WHERE dimension_type = 'mental_health' " +
                "ORDER BY CASE dimension_value " +
                "WHEN 'Healthy' THEN 1 " +
                "WHEN 'Anxiety' THEN 2 " +
                "WHEN 'Depression' THEN 3 " +
                "WHEN 'Both' THEN 4 " +
                "ELSE 5 END";
        List<Map<String, Object>> mentalHealthData = jdbcTemplate.queryForList(mentalSql);

        result.put("gender", genderData);
        result.put("chronotype", chronotypeData);
        result.put("mentalHealth", mentalHealthData);
        result.put("minAge", minAge);
        result.put("maxAge", maxAge);

        return result;
    }
}
