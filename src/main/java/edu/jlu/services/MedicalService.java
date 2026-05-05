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
                "  COUNT(*) AS peopleCount, " +
                "  ROUND(AVG(screen_time_before_bed_mins), 1) AS avgScreen, " +
                "  ROUND(AVG(sleep_latency_mins), 1) AS avgLatency, " +
                "  ROUND(AVG(caffeine_mg_before_bed), 1) AS avgCaffeine, " +
                "  ROUND(AVG(alcohol_units_before_bed), 1) AS avgAlcohol " +
                "FROM sleep_health_dataset " +
                "GROUP BY occupation " +
                "HAVING peopleCount > 5 ";
        return jdbcTemplate.queryForList(sql);
    }
}
