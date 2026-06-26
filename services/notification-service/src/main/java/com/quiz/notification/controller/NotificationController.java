package com.quiz.notification.controller;

import com.quiz.notification.dto.BaseResponse;
import com.quiz.notification.dto.NotificationResponse;
import com.quiz.notification.service.NotificationPersistenceService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationPersistenceService notificationPersistenceService;

    public NotificationController(NotificationPersistenceService notificationPersistenceService) {
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("userId");
        List<NotificationResponse> data = notificationPersistenceService.listForUser(userId);
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK.value(), "Notifications retrieved", data));
    }
}
