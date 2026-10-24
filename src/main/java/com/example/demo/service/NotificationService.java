package com.example.demo.service;

import com.example.demo.dto.Dtos.NotificationResponse;
import com.example.demo.entity.Notification;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository repo;

    public void send(Long userId, String type, String message) {
        repo.save(Notification.builder().userId(userId).type(type).message(message).build());
    }

    public List<NotificationResponse> mine(AuthUser user) {
        return repo.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(this::toResponse).toList();
    }

    public NotificationResponse markRead(AuthUser user, Long id) {
        Notification n = repo.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (!n.getUserId().equals(user.id())) throw ApiException.notFound("Notification not found");
        n.setRead(true);
        return toResponse(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}