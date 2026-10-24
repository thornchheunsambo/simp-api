package com.example.demo.controller;

import com.example.demo.dto.Dtos.NotificationResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;

    @GetMapping
    public List<NotificationResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return notifications.mine(user);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return notifications.markRead(user, id);
    }
}