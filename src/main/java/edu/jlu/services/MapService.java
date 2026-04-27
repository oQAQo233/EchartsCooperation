package edu.jlu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MapService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getCountryDistribution() {
        String sql = "SELECT country AS name, COUNT(*) AS value, " +
            "AVG(sleep_duration_hrs) as avgDuration, " +
            "AVG(sleep_quality_score) as avgQuality " +
            "FROM sleep_health_dataset GROUP BY country ORDER BY value DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
