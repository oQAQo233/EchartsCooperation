package edu.jlu.services;

import edu.jlu.models.CountryDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class MapService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<CountryDistribution> getCountryDistribution() {
        String sql = "SELECT name, value, avgDuration, avgQuality FROM sub_country_distribution";
        return jdbcTemplate.query(sql, new CountryDistributionRowMapper());
    }

    private static class CountryDistributionRowMapper implements RowMapper<CountryDistribution> {
        @Override
        public CountryDistribution mapRow(ResultSet rs, int rowNum) throws SQLException {
            CountryDistribution item = new CountryDistribution();
            item.setName(rs.getString("name"));
            item.setValue(rs.getInt("value"));
            item.setAvgDuration(rs.getDouble("avgDuration"));
            item.setAvgQuality(rs.getDouble("avgQuality"));
            return item;
        }
    }
}
