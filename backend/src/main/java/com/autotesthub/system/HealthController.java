package com.autotesthub.system;

import com.autotesthub.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", "AutoTest Hub");
        data.put("status", "UP");
        data.put("time", Instant.now().toString());
        return ApiResponse.ok(data);
    }

    @GetMapping("/health/db")
    public ApiResponse<Map<String, Object>> databaseHealth() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("database", "MySQL");
        data.put("status", result != null && result == 1 ? "UP" : "DOWN");
        return ApiResponse.ok(data);
    }
}
