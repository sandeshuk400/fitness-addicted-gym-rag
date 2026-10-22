package com.fitnessaddicted.gym.common;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes security-relevant events to the audit_log table.
 * Joins the caller's transaction when there is one, so an audit row never refers to data
 * that was rolled back. Do not call it from read-only transactions.
 */
@Service
public class AuditService {

    private static final String INSERT_SQL = """
            INSERT INTO audit_log (actor_id, action, entity_type, entity_id, details)
            VALUES (?, ?, ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AuditService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(Long actorId, String action, String entityType, String entityId, Map<String, ?> details) {
        String json;
        try {
            json = objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize audit details", e);
        }
        jdbc.update(INSERT_SQL, actorId, action, entityType, entityId, json);
    }
}
