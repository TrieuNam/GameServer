package com.SouthMillion.analytics_service.grpc;

import com.SouthMillion.analytics_service.entity.PlayerEvent;
import com.SouthMillion.analytics_service.entity.PlayerKpi;
import com.SouthMillion.analytics_service.service.AnalyticsService;
import org.SouthMillion.proto.analytics.AnalyticsProto.*;
import org.SouthMillion.proto.analytics.AnalyticsServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.SouthMillion.grpc.common.ResponseStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceGrpcImpl extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {
    
    private final AnalyticsService analyticsService;
    
    @Override
    public void trackEvent(TrackEventRequest request, StreamObserver<ResponseStatus> responseObserver) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            // Parse JSON from string if needed
            
            analyticsService.trackEvent(
                request.getPlayerId(),
                request.getEventType(),
                request.getEventCategory(),
                eventData,
                request.getSessionId()
            );
            
            responseObserver.onNext(ResponseStatus.newBuilder()
                .setCode(200)
                .setMessage("Event tracked successfully")
                .setSuccess(true)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to track event", e);
            responseObserver.onNext(ResponseStatus.newBuilder()
                .setCode(500)
                .setMessage("Failed to track event: " + e.getMessage())
                .setSuccess(false)
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getPlayerEvents(GetPlayerEventsRequest request, StreamObserver<PlayerEventsResponse> responseObserver) {
        try {
            LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getStartTime()), 
                ZoneOffset.UTC
            );
            LocalDateTime endTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getEndTime()), 
                ZoneOffset.UTC
            );
            
            List<PlayerEvent> events = analyticsService.getPlayerEvents(
                request.getPlayerId(),
                startTime,
                endTime
            );
            
            List<org.SouthMillion.proto.analytics.AnalyticsProto.PlayerEvent> protoEvents = events.stream()
                .map(this::toProtoEvent)
                .collect(Collectors.toList());
            
            responseObserver.onNext(PlayerEventsResponse.newBuilder()
                .addAllEvents(protoEvents)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get player events", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getPlayerKpi(GetPlayerKpiRequest request, StreamObserver<PlayerKpiResponse> responseObserver) {
        try {
            LocalDateTime date = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getDate()), 
                ZoneOffset.UTC
            );
            
            PlayerKpi kpi = analyticsService.getPlayerKpi(request.getPlayerId(), date);
            
            if (kpi != null) {
                responseObserver.onNext(PlayerKpiResponse.newBuilder()
                    .setKpi(toProtoKpi(kpi))
                    .build());
            } else {
                responseObserver.onNext(PlayerKpiResponse.newBuilder().build());
            }
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get player KPI", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getTopSpenders(GetTopSpendersRequest request, StreamObserver<TopSpendersResponse> responseObserver) {
        try {
            LocalDateTime since = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getSince()), 
                ZoneOffset.UTC
            );
            
            List<PlayerKpi> topSpenders = analyticsService.getTopSpenders(since);
            
            List<org.SouthMillion.proto.analytics.AnalyticsProto.PlayerKpi> protoKpis = topSpenders.stream()
                .limit(request.getLimit() > 0 ? request.getLimit() : 10)
                .map(this::toProtoKpi)
                .collect(Collectors.toList());
            
            responseObserver.onNext(TopSpendersResponse.newBuilder()
                .addAllTopSpenders(protoKpis)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get top spenders", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getMostActiveUsers(GetMostActiveUsersRequest request, StreamObserver<MostActiveUsersResponse> responseObserver) {
        try {
            LocalDateTime since = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getSince()), 
                ZoneOffset.UTC
            );
            
            List<PlayerKpi> activeUsers = analyticsService.getMostActiveUsers(since);
            
            List<org.SouthMillion.proto.analytics.AnalyticsProto.PlayerKpi> protoKpis = activeUsers.stream()
                .limit(request.getLimit() > 0 ? request.getLimit() : 10)
                .map(this::toProtoKpi)
                .collect(Collectors.toList());
            
            responseObserver.onNext(MostActiveUsersResponse.newBuilder()
                .addAllActiveUsers(protoKpis)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get most active users", e);
            responseObserver.onError(e);
        }
    }
    
    private org.SouthMillion.proto.analytics.AnalyticsProto.PlayerEvent toProtoEvent(PlayerEvent event) {
        return org.SouthMillion.proto.analytics.AnalyticsProto.PlayerEvent.newBuilder()
            .setId(event.getId())
            .setPlayerId(event.getPlayerId())
            .setEventType(event.getEventType())
            .setEventCategory(event.getEventCategory())
            .setEventData(event.getEventData() != null ? event.getEventData() : "")
            .setEventTime(event.getEventTime().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setServerName(event.getServerName() != null ? event.getServerName() : "")
            .setSessionId(event.getSessionId() != null ? event.getSessionId() : "")
            .build();
    }
    
    private org.SouthMillion.proto.analytics.AnalyticsProto.PlayerKpi toProtoKpi(PlayerKpi kpi) {
        return org.SouthMillion.proto.analytics.AnalyticsProto.PlayerKpi.newBuilder()
            .setId(kpi.getId())
            .setPlayerId(kpi.getPlayerId())
            .setDate(kpi.getDate().toInstant(ZoneOffset.UTC).toEpochMilli())
            .setLoginCount(kpi.getLoginCount())
            .setSessionDuration(kpi.getSessionDuration())
            .setTotalSpent(kpi.getTotalSpent().toString())
            .setTotalEarned(kpi.getTotalEarned().toString())
            .setPurchaseCount(kpi.getPurchaseCount())
            .setBattlesPlayed(kpi.getBattlesPlayed())
            .setBattlesWon(kpi.getBattlesWon())
            .setPvpMatches(kpi.getPvpMatches())
            .setMessagesSent(kpi.getMessagesent())
            .setFriendRequests(kpi.getFriendRequests())
            .setTasksCompleted(kpi.getTasksCompleted())
            .setLevelsGained(kpi.getLevelsGained())
            .build();
    }
}

