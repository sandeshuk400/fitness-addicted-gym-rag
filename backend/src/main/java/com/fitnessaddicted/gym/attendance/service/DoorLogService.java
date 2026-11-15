package com.fitnessaddicted.gym.attendance.service;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessaddicted.gym.attendance.api.DoorLogEntry;
import com.fitnessaddicted.gym.common.PageResponse;

@Service
public class DoorLogService {

    private final JdbcTemplate jdbc;

    public DoorLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoorLogEntry> list(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Long count = jdbc.queryForObject("select count(*) from door_event", Long.class);
        long total = count == null ? 0 : count;
        List<DoorLogEntry> rows = jdbc.query(
                "select e.id, e.scanned_at, e.device_user_id, e.device_serial, e.member_id, m.full_name, e.result "
                        + "from door_event e left join member_profile m on m.user_id = e.member_id "
                        + "order by e.scanned_at desc, e.id desc limit ? offset ?",
                (rs, i) -> new DoorLogEntry(rs.getLong(1), rs.getTimestamp(2).toInstant(), rs.getInt(3),
                        rs.getString(4), rs.getObject(5) == null ? null : rs.getLong(5),
                        rs.getString(6), rs.getString(7)),
                s, (long) p * s);
        return PageResponse.from(new PageImpl<>(rows, PageRequest.of(p, s), total));
    }
}