package com.fitnessaddicted.gym.attendance.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessaddicted.gym.attendance.service.DoorLogService;
import com.fitnessaddicted.gym.common.PageResponse;

/** Staff-only. Separate from the device key route, so the device key can never read data. */
@RestController
@RequestMapping("/api/v1/device/log")
public class DoorLogController {

    private final DoorLogService service;

    public DoorLogController(DoorLogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageResponse<DoorLogEntry> log(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size) {
        return service.list(page, size);
    }
}