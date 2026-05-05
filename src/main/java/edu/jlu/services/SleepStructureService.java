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
        String sql = "SELECT MIN(age) as minAge, MAX(age) as maxAge FROM sleep_structure";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        Map<String, Object> ageRange = new HashMap<>();
        ageRange.put("minAge", ((Number) result.get("minAge")).intValue());
        ageRange.put("maxAge", ((Number) result.get("maxAge")).intValue());
        return ageRange;
    }

    public Map<String, Object> getChartData(Integer minAge, Integer maxAge) {
        Map<String, Object> result = new HashMap<>();

        // 性别数据，按 男 -> 女 -> 其他 的顺序
        String genderSql = "SELECT gender, AVG(sleep_duration_hrs) as avg_duration, " +
                "AVG(rem_percentage) as avg_rem, AVG(deep_sleep_percentage) as avg_deep, " +
                "AVG(light_sleep_percentage) as avg_light, AVG(sleep_quality_score) as avg_quality, " +
                "AVG(wake_episodes_per_night) as avg_wake_episodes " +
                "FROM sleep_structure WHERE age BETWEEN ? AND ? GROUP BY gender " +
                "ORDER BY CASE gender " +
                "WHEN 'male' THEN 1 " +
                "WHEN 'female' THEN 2 " +
                "WHEN 'other' THEN 3 " +
                "ELSE 4 END";
        List<Map<String, Object>> genderData = jdbcTemplate.queryForList(genderSql, minAge, maxAge);

        // 睡眠类型数据，按 早鸟型(Morning) -> 中间型(Neutral) -> 夜猫子型(Evening) 的顺序
        String chronotypeSql = "SELECT chronotype, AVG(sleep_duration_hrs) as avg_duration, " +
                "AVG(rem_percentage) as avg_rem, AVG(deep_sleep_percentage) as avg_deep, " +
                "AVG(light_sleep_percentage) as avg_light, AVG(sleep_quality_score) as avg_quality, " +
                "AVG(wake_episodes_per_night) as avg_wake_episodes " +
                "FROM sleep_structure WHERE age BETWEEN ? AND ? GROUP BY chronotype " +
                "ORDER BY CASE chronotype " +
                "WHEN 'Morning' THEN 1 " +
                "WHEN 'Neutral' THEN 2 " +
                "WHEN 'Evening' THEN 3 " +
                "ELSE 4 END";
        List<Map<String, Object>> chronotypeData = jdbcTemplate.queryForList(chronotypeSql, minAge, maxAge);

        // 心理健康数据，按 健康(Healthy) -> 焦虑(Anxiety) -> 抑郁(Depression) -> 共病(Both) 的顺序
        String mentalSql = "SELECT mental_health_condition, AVG(sleep_duration_hrs) as avg_duration, " +
                "AVG(rem_percentage) as avg_rem, AVG(deep_sleep_percentage) as avg_deep, " +
                "AVG(light_sleep_percentage) as avg_light, AVG(sleep_quality_score) as avg_quality, " +
                "AVG(wake_episodes_per_night) as avg_wake_episodes " +
                "FROM sleep_structure WHERE age BETWEEN ? AND ? GROUP BY mental_health_condition " +
                "ORDER BY CASE mental_health_condition " +
                "WHEN 'Healthy' THEN 1 " +
                "WHEN 'Anxiety' THEN 2 " +
                "WHEN 'Depression' THEN 3 " +
                "WHEN 'Both' THEN 4 " +
                "ELSE 5 END";
        List<Map<String, Object>> mentalHealthData = jdbcTemplate.queryForList(mentalSql, minAge, maxAge);

        result.put("gender", genderData);
        result.put("chronotype", chronotypeData);
        result.put("mentalHealth", mentalHealthData);
        result.put("minAge", minAge);
        result.put("maxAge", maxAge);

        return result;
    }
}