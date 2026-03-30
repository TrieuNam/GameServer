package com.SouthMillion.localization_service.grpc;

import com.SouthMillion.localization_service.service.LocalizationService;
import org.SouthMillion.proto.localization.LocalizationProto.*;
import org.SouthMillion.proto.localization.LocalizationServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalizationServiceGrpcImpl extends LocalizationServiceGrpc.LocalizationServiceImplBase {
    
    private final LocalizationService localizationService;
    
    @Override
    public void translate(TranslateRequest request, StreamObserver<TranslateResponse> responseObserver) {
        try {
            String translation = localizationService.translate(request.getKey(), request.getLanguage());
            
            responseObserver.onNext(TranslateResponse.newBuilder()
                .setKey(request.getKey())
                .setLanguage(request.getLanguage())
                .setValue(translation)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to translate", e);
            responseObserver.onError(e);
        }
    }
    
    @Override
    public void getAllTranslations(GetAllTranslationsRequest request, StreamObserver<AllTranslationsResponse> responseObserver) {
        try {
            Map<String, String> translations = localizationService.getAll(request.getLanguage());
            
            responseObserver.onNext(AllTranslationsResponse.newBuilder()
                .setLanguage(request.getLanguage())
                .putAllTranslations(translations)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get all translations", e);
            responseObserver.onError(e);
        }
    }
}

