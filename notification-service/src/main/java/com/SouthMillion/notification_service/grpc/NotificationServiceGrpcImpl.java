package com.SouthMillion.notification_service.grpc;

import com.SouthMillion.notification_service.entity.Notification;
import com.SouthMillion.notification_service.service.NotificationService;
import org.SouthMillion.proto.notification.NotificationProto.*;
import org.SouthMillion.proto.notification.NotificationServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.SouthMillion.grpc.common.ResponseStatus;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceGrpcImpl extends NotificationServiceGrpc.NotificationServiceImplBase {
    
    private final NotificationService notificationService;
    
    @Override
    public void sendNotification(SendNotificationRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            Map<String, Object> data = new HashMap<>();
            // Parse JSON data if needed
            
            notificationService.createNotification(
                request.getPlayerId(),
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                data
            );
            
            responseObserver.onNext(ResponseStatus.newBuilder()
                .setCode(200)
                .setMessage("Notification sent successfully")
                .setSuccess(true)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                .setCode(500)
                .setMessage("Failed to send notification: " + e.getMessage())
                .setSuccess(false)
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getPlayerNotifications(GetPlayerNotificationsRequest request, StreamObserver<NotificationsResponse> responseObserver) {
        try {
            List<Notification> notifications = notificationService.getPlayerNotifications(request.getPlayerId());
            
            List<org.SouthMillion.proto.notification.NotificationProto.Notification> protoNotifications = notifications.stream()
                .limit(request.getLimit() > 0 ? request.getLimit() : notifications.size())
                .map(this::toProtoNotification)
                .collect(Collectors.toList());
            
            responseObserver.onNext(NotificationsResponse.newBuilder()
                .addAllNotifications(protoNotifications)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get player notifications", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getUnreadNotifications(GetUnreadNotificationsRequest request, StreamObserver<NotificationsResponse> responseObserver) {
        try {
            List<Notification> unread = notificationService.getUnreadNotifications(request.getPlayerId());
            
            List<org.SouthMillion.proto.notification.NotificationProto.Notification> protoNotifications = unread.stream()
                .map(this::toProtoNotification)
                .collect(Collectors.toList());
            
            responseObserver.onNext(NotificationsResponse.newBuilder()
                .addAllNotifications(protoNotifications)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get unread notifications", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void markAsRead(MarkAsReadRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            notificationService.markAsRead(request.getNotificationId());
            
            responseObserver.onNext(ResponseStatus.newBuilder()
                .setCode(200)
                .setMessage("Notification marked as read")
                .setSuccess(true)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to mark notification as read", e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                .setCode(500)
                .setMessage("Failed to mark as read: " + e.getMessage())
                .setSuccess(false)
                .build());
            responseObserver.onCompleted();
        }
    }
    
    private org.SouthMillion.proto.notification.NotificationProto.Notification toProtoNotification(Notification notification) {
        org.SouthMillion.proto.notification.NotificationProto.Notification.Builder builder = org.SouthMillion.proto.notification.NotificationProto.Notification.newBuilder()
            .setId(notification.getId())
            .setPlayerId(notification.getPlayerId())
            .setType(notification.getType())
            .setTitle(notification.getTitle())
            .setMessage(notification.getMessage() != null ? notification.getMessage() : "")
            .setData(notification.getData() != null ? notification.getData() : "")
            .setStatus(notification.getStatus())
            .setCreatedAt(notification.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        
        if (notification.getErrorMessage() != null) {
            builder.setErrorMessage(notification.getErrorMessage());
        }
        if (notification.getSentAt() != null) {
            builder.setSentAt(notification.getSentAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        if (notification.getReadAt() != null) {
            builder.setReadAt(notification.getReadAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        
        return builder.build();
    }
}

