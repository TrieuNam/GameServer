package com.SouthMillion.file_service.grpc;

import com.google.protobuf.ByteString;
import com.SouthMillion.file_service.service.FileService;
import org.SouthMillion.proto.file.FileProto.*;
import org.SouthMillion.proto.file.FileServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceGrpcImpl extends FileServiceGrpc.FileServiceImplBase {
    
    private final FileService fileService;
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Override
    public void uploadFile(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver) {
        try {
            // Convert ByteString to MultipartFile
            byte[] fileBytes = request.getFileData().toByteArray();
            MultipartFile file = new MockMultipartFile(
                request.getFileName(),
                request.getFileName(),
                "application/octet-stream",
                fileBytes
            );
            
            String savedFileName = fileService.uploadFile(file);
            
            responseObserver.onNext(UploadFileResponse.newBuilder()
                .setFileName(savedFileName)
                .setUrl("/api/file/download/" + savedFileName)
                .setSuccess(true)
                .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            responseObserver.onNext(UploadFileResponse.newBuilder()
                .setSuccess(false)
                .setError("Failed to upload file: " + e.getMessage())
                .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getFileUrl(GetFileUrlRequest request, StreamObserver<GetFileUrlResponse> responseObserver) {
        try {
            // Check if file exists
            Path filePath = Paths.get(uploadDir).resolve(request.getFileName());
            
            if (Files.exists(filePath)) {
                responseObserver.onNext(GetFileUrlResponse.newBuilder()
                    .setUrl("/api/file/download/" + request.getFileName())
                    .setSuccess(true)
                    .build());
            } else {
                responseObserver.onNext(GetFileUrlResponse.newBuilder()
                    .setSuccess(false)
                    .setError("File not found")
                    .build());
            }
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to get file URL", e);
            responseObserver.onError(e);
        }
    }
}

