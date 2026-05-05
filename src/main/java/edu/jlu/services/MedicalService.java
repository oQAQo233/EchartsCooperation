package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MedicalService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getBedtimeBehaviorImpact() {
        String sql = "SELECT occupation AS groupName, " +
                "people_count AS peopleCount, " +
                "avg_screen AS avgScreen, " +
                "avg_latency AS avgLatency, " +
                "avg_caffeine AS avgCaffeine, " +
                "avg_alcohol AS avgAlcohol " +
                "FROM agg_bedtime_behavior_impact";
        return jdbcTemplate.queryForList(sql);
    }
}
