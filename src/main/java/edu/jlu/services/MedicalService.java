package edu.jlu.services;

import edu.jlu.models.BedtimeBehaviorImpact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class MedicalService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<BedtimeBehaviorImpact> getBedtimeBehaviorImpact() {
        String sql = "SELECT groupName, peopleCount, avgScreen, avgLatency, avgCaffeine, avgAlcohol " +
                "FROM sub_bedtime_behavior";
        return jdbcTemplate.query(sql, new BedtimeBehaviorImpactRowMapper());
    }

    private static class BedtimeBehaviorImpactRowMapper implements RowMapper<BedtimeBehaviorImpact> {
        @Override
        public BedtimeBehaviorImpact mapRow(ResultSet rs, int rowNum) throws SQLException {
            BedtimeBehaviorImpact item = new BedtimeBehaviorImpact();
            item.setGroupName(rs.getString("groupName"));
            item.setPeopleCount(rs.getInt("peopleCount"));
            item.setAvgScreen(rs.getDouble("avgScreen"));
            item.setAvgLatency(rs.getDouble("avgLatency"));
            item.setAvgCaffeine(rs.getDouble("avgCaffeine"));
            item.setAvgAlcohol(rs.getDouble("avgAlcohol"));
            return item;
        }
    }
}
