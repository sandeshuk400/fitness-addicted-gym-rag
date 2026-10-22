package com.fitnessaddicted.gym.attendance.api;

import com.fitnessaddicted.gym.attendance.service.AttendanceService;
import com.fitnessaddicted.gym.common.CurrentUser;
import com.fitnessaddicted.gym.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return attendanceService.checkIn(request.memberId());
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public AttendanceResponse checkOut(@Valid @RequestBody CheckInRequest request) {
        return attendanceService.checkOut(request.memberId());
    }

    /** How many members are inside right now. */
    @GetMapping("/occupancy")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public OccupancyResponse occupancy() {
        return attendanceService.occupancy();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public PageResponse<AttendanceResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return attendanceService.history(CurrentUser.id(jwt), page, size);
    }

    @GetMapping("/members/{memberId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public PageResponse<AttendanceResponse> forMember(@PathVariable Long memberId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return attendanceService.history(memberId, page, size);
    }
}
