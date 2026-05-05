package edu.jlu.services;

import edu.jlu.models.SleepHealthRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SleepHealthService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<SleepHealthRecord> getAllRecords() {
        return jdbcTemplate.query(
            "SELECT * FROM sleep_health_dataset LIMIT 50",
            new SleepHealthRecordRowMapper()
        );
    }

    public SleepHealthRecord getRecordById(Integer personId) {
        List<SleepHealthRecord> results = jdbcTemplate.query(
            "SELECT * FROM sleep_health_dataset WHERE person_id = ?",
            new SleepHealthRecordRowMapper(),
            personId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public int getRecordCount() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sleep_health_dataset",
            Integer.class
        );
        return count != null ? count : 0;
    }

    public Map<String, Object> searchRecordsPaginated(
            int start, int length, String search,
            Integer orderColumn, String orderDir) {

        Map<String, Object> result = new HashMap<>();

        String[] ALLOWED_COLUMNS = {
            "person_id", "age", "gender", "occupation",
            "sleep_duration_hrs", "sleep_quality_score", "stress_score",
            "chronotype", "sleep_disorder_risk", "country"
        };

        String searchClause = "";
        List<Object> searchParams = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            searchClause = "AND (" +
                "person_id LIKE ? OR age LIKE ? OR gender LIKE ? OR occupation LIKE ? OR " +
                "sleep_duration_hrs LIKE ? OR sleep_quality_score LIKE ? OR stress_score LIKE ? OR " +
                "chronotype LIKE ? OR sleep_disorder_risk LIKE ? OR country LIKE ?)";
            for (int i = 0; i < 10; i++) {
                searchParams.add("%" + search.trim() + "%");
            }
        }

        String orderClause = "ORDER BY person_id ASC";
        if (orderColumn != null && orderColumn >= 0 && orderColumn < ALLOWED_COLUMNS.length && orderDir != null && !orderDir.trim().isEmpty()) {
            String dir = "desc".equalsIgnoreCase(orderDir) ? "DESC" : "ASC";
            orderClause = "ORDER BY " + ALLOWED_COLUMNS[orderColumn] + " " + dir;
        }

        String dataSql = "SELECT * FROM sleep_health_dataset WHERE 1=1 "
                       + searchClause + " " + orderClause + " LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(searchParams);
        dataParams.add(length);
        dataParams.add(start);
        List<SleepHealthRecord> records = jdbcTemplate.query(dataSql,
            new SleepHealthRecordRowMapper(), dataParams.toArray());

        String countSql = "SELECT COUNT(*) FROM sleep_health_dataset WHERE 1=1 " + searchClause;
        Integer filteredCount = jdbcTemplate.queryForObject(countSql, Integer.class,
            searchParams.toArray());

        result.put("data", records);
        result.put("recordsFiltered", filteredCount != null ? filteredCount : 0);
        result.put("recordsTotal", getRecordCount());

        return result;
    }

    // ========== 可视化统计数据 - 从聚合表获取 ==========

    public List<Map<String, Object>> getAgeDistribution() {
        String sql = "SELECT age_group AS age_group, record_count AS count " +
                "FROM age_distribution ORDER BY age_group";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getSleepDisorderRiskDistribution() {
        String sql = "SELECT risk_level AS name, record_count AS value " +
                "FROM sleep_disorder_risk";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getSleepDurationByOccupation() {
        String sql = "SELECT occupation AS name, avg_duration AS value " +
                "FROM sleep_duration_by_occupation ORDER BY value DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getSleepQualityByChronotype() {
        String sql = "SELECT chronotype AS name, avg_quality AS value " +
                "FROM sleep_quality_by_chronotype";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getSleepDurationVsQuality() {
        String sql = "SELECT sleep_duration_hrs AS x, sleep_quality_score AS y " +
                "FROM sleep_health_dataset LIMIT 100";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getStressScoreDistribution() {
        String sql = "SELECT stress_group AS name, record_count AS value " +
                "FROM stress_distribution ORDER BY name";
        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> getDashboardStats() {
        String sql = "SELECT total_records, avg_sleep_duration, avg_sleep_quality, avg_stress_score " +
                "FROM dashboard_stats WHERE id = 1";
        Map<String, Object> row = jdbcTemplate.queryForMap(sql);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", row.get("total_records"));
        stats.put("avgSleepDuration", row.get("avg_sleep_duration"));
        stats.put("avgSleepQuality", row.get("avg_sleep_quality"));
        stats.put("avgStressScore", row.get("avg_stress_score"));
        return stats;
    }

    private static class SleepHealthRecordRowMapper implements RowMapper<SleepHealthRecord> {
        @Override
        public SleepHealthRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            SleepHealthRecord record = new SleepHealthRecord();
            record.setPersonId(rs.getInt("person_id"));
            record.setAge(rs.getInt("age"));
            record.setGender(rs.getString("gender"));
            record.setOccupation(rs.getString("occupation"));
            record.setBmi(rs.getDouble("bmi"));
            record.setCountry(rs.getString("country"));
            record.setSleepDurationHrs(rs.getDouble("sleep_duration_hrs"));
            record.setSleepQualityScore(rs.getDouble("sleep_quality_score"));
            record.setRemPercentage(rs.getDouble("rem_percentage"));
            record.setDeepSleepPercentage(rs.getDouble("deep_sleep_percentage"));
            record.setSleepLatencyMins(rs.getInt("sleep_latency_mins"));
            record.setWakeEpisodesPerNight(rs.getInt("wake_episodes_per_night"));
            record.setCaffeineMgBeforeBed(rs.getInt("caffeine_mg_before_bed"));
            record.setAlcoholUnitsBeforeBed(rs.getDouble("alcohol_units_before_bed"));
            record.setScreenTimeBeforeBedMins(rs.getInt("screen_time_before_bed_mins"));
            record.setExerciseDay(rs.getInt("exercise_day"));
            record.setStepsThatDay(rs.getInt("steps_that_day"));
            record.setNapDurationMins(rs.getInt("nap_duration_mins"));
            record.setStressScore(rs.getDouble("stress_score"));
            record.setWorkHoursThatDay(rs.getDouble("work_hours_that_day"));
            record.setChronotype(rs.getString("chronotype"));
            record.setMentalHealthCondition(rs.getString("mental_health_condition"));
            record.setHeartRateRestingBpm(rs.getInt("heart_rate_resting_bpm"));
            record.setSleepAidUsed(rs.getInt("sleep_aid_used"));
            record.setShiftWork(rs.getInt("shift_work"));
            record.setRoomTemperatureCelsius(rs.getDouble("room_temperature_celsius"));
            record.setWeekendSleepDiffHrs(rs.getDouble("weekend_sleep_diff_hrs"));
            record.setSeason(rs.getString("season"));
            record.setDayType(rs.getString("day_type"));
            record.setCognitivePerformanceScore(rs.getDouble("cognitive_performance_score"));
            record.setSleepDisorderRisk(rs.getString("sleep_disorder_risk"));
            record.setFeltRested(rs.getInt("felt_rested"));
            return record;
        }
    }
}
