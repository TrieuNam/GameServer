package com.SouthMillion.scroll_service.grpc;

import com.SouthMillion.scroll_service.entity.ScrollItem;
import com.SouthMillion.scroll_service.entity.ScrollMeta;
import com.SouthMillion.scroll_service.service.ScrollService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.proto.scroll.*;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ScrollServiceGrpcImpl extends ScrollServiceGrpc.ScrollServiceImplBase {

    private final ScrollService scrollService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void getMeta(GetMetaRequest request, StreamObserver<ScrollMetaResponse> observer) {
        log.info("[ScrollGrpc] GetMeta: roleId={}", request.getRoleId());
        try {
            ScrollMeta meta = scrollService.getMeta(request.getRoleId());
            observer.onNext(ScrollMetaResponse.newBuilder()
                    .setSuccess(true).setMessage("OK")
                    .setMeta(toScrollMetaData(meta))
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ScrollGrpc] GetMeta error", e);
            observer.onNext(ScrollMetaResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getList(GetListRequest request, StreamObserver<GetListResponse> observer) {
        log.info("[ScrollGrpc] GetList: roleId={}", request.getRoleId());
        try {
            List<ScrollItem> items = scrollService.getList(request.getRoleId());
            GetListResponse.Builder resp = GetListResponse.newBuilder().setSuccess(true).setMessage("OK");
            if (items != null) {
                items.forEach(i -> resp.addItems(toScrollItemData(i)));
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ScrollGrpc] GetList error", e);
            observer.onNext(GetListResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void draw(DrawRequest request, StreamObserver<GenericResponse> observer) {
        log.info("[ScrollGrpc] Draw: roleId={} count={}", request.getRoleId(), request.getCount());
        try {
            Map<String, Object> result = scrollService.draw(request.getRoleId(), request.getCount());
            boolean ok = Boolean.TRUE.equals(result.get("success"));
            String json = objectMapper.writeValueAsString(result);
            observer.onNext(GenericResponse.newBuilder()
                    .setSuccess(ok).setMessage(ok ? "OK" : "failed").setDataJson(json)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[ScrollGrpc] Draw error", e);
            observer.onNext(GenericResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    private ScrollMetaData toScrollMetaData(ScrollMeta m) {
        return ScrollMetaData.newBuilder()
                .setId(m.getId() != null ? m.getId() : 0L)
                .setRoleId(m.getRoleId() != null ? m.getRoleId() : 0L)
                .setFreeNum(m.getFreeNum() != null ? m.getFreeNum() : 0)
                .setBaoDiNum(m.getBaoDiNum() != null ? m.getBaoDiNum() : 0)
                .build();
    }

    private ScrollItemData toScrollItemData(ScrollItem i) {
        return ScrollItemData.newBuilder()
                .setId(i.getId() != null ? i.getId() : 0L)
                .setRoleId(i.getRoleId() != null ? i.getRoleId() : 0L)
                .setScrollIndex(i.getScrollIndex() != null ? i.getScrollIndex() : 0)
                .setItemId(i.getItemId() != null ? i.getItemId() : 0)
                .setLevel(i.getLevel() != null ? i.getLevel() : 0)
                .setWearMark(i.getWearMark() != null ? i.getWearMark() : 0)
                .setParam(i.getParam() != null ? i.getParam() : 0)
                .build();
    }
}
