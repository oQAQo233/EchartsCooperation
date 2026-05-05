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
        String sql = "SELECT country AS name, record_count AS value, " +
            "avg_duration AS avgDuration, avg_quality AS avgQuality " +
            "FROM country_distribution ORDER BY value DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
