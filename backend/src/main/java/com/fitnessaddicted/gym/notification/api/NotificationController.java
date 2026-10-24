package com.fitnessaddicted.gym.notification.api;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessaddicted.gym.common.CurrentUser;
import com.fitnessaddicted.gym.notification.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('MEMBER')")
    public List<NotificationResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(CurrentUser.id(jwt));
    }

    @GetMapping("/mine/unread-count")
    @PreAuthorize("hasRole('MEMBER')")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("unread", service.unreadCount(CurrentUser.id(jwt)));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('MEMBER')")
    public NotificationResponse markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.markRead(CurrentUser.id(jwt), id);
    }
}