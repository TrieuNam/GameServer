package com.SouthMillion.file_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Tests")
class FileServiceTest {

    @Mock private MultipartFile multipartFile;

    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        fileService = new FileService();
        setField(fileService, "uploadDir", tempDir.toString());
        setField(fileService, "baseUrl", "http://localhost:8540/files");
    }

    /** Inject @Value field via reflection since we're not running Spring context. */
    private static void setField(Object target, String fieldName, String value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // =========================================================
    // uploadFile()
    // =========================================================
    @Nested
    @DisplayName("uploadFile()")
    class UploadFile {

        @Test
        @DisplayName("TC-FILE-001 [P] Upload file hop le – luu vao disk va tra ve URL")
        void uploadFile_validFile_savesAndReturnsUrl() throws IOException {
            byte[] content = "hello world".getBytes();
            given(multipartFile.isEmpty()).willReturn(false);
            given(multipartFile.getOriginalFilename()).willReturn("test.txt");
            given(multipartFile.getInputStream()).willReturn(new ByteArrayInputStream(content));

            String url = fileService.uploadFile(multipartFile);

            assertThat(url).startsWith("http://localhost:8540/files/");
            assertThat(url).endsWith("_test.txt");
            // Verify file was physically created
            String fileName = url.replace("http://localhost:8540/files/", "");
            assertThat(Files.exists(tempDir.resolve(fileName))).isTrue();
        }

        @Test
        @DisplayName("TC-FILE-002 [P] Upload file – ten file duoc prefix bang UUID")
        void uploadFile_validFile_fileNameContainsUuidPrefix() throws IOException {
            given(multipartFile.isEmpty()).willReturn(false);
            given(multipartFile.getOriginalFilename()).willReturn("avatar.png");
            given(multipartFile.getInputStream()).willReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

            String url = fileService.uploadFile(multipartFile);

            // UUID format: 8-4-4-4-12 hex chars followed by underscore and original name
            String fileName = url.replace("http://localhost:8540/files/", "");
            assertThat(fileName).matches("[0-9a-f\\-]+_avatar\\.png");
        }

        @Test
        @DisplayName("TC-FILE-003 [N] File rong – nem IllegalArgumentException")
        void uploadFile_emptyFile_throws() {
            given(multipartFile.isEmpty()).willReturn(true);

            assertThatThrownBy(() -> fileService.uploadFile(multipartFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File is empty");
        }

        @Test
        @DisplayName("TC-FILE-004 [P] Thu muc upload chua ton tai – tu dong tao thu muc")
        void uploadFile_uploadDirNotExists_createsDirectory() throws Exception {
            Path subDir = tempDir.resolve("uploads/subdir");
            setField(fileService, "uploadDir", subDir.toString());

            given(multipartFile.isEmpty()).willReturn(false);
            given(multipartFile.getOriginalFilename()).willReturn("file.txt");
            given(multipartFile.getInputStream()).willReturn(new ByteArrayInputStream("data".getBytes()));

            String url = fileService.uploadFile(multipartFile);

            assertThat(url).isNotNull();
            assertThat(Files.exists(subDir)).isTrue();
        }
    }

    // =========================================================
    // downloadFile()
    // =========================================================
    @Nested
    @DisplayName("downloadFile()")
    class DownloadFile {

        @Test
        @DisplayName("TC-FILE-005 [P] Download file ton tai – tra ve byte array")
        void downloadFile_existingFile_returnsByteArray() throws IOException {
            byte[] expectedContent = "file content".getBytes();
            Path file = tempDir.resolve("myfile.txt");
            Files.write(file, expectedContent);

            byte[] result = fileService.downloadFile("myfile.txt");

            assertThat(result).isEqualTo(expectedContent);
        }

        @Test
        @DisplayName("TC-FILE-006 [N] File khong ton tai – nem IllegalArgumentException")
        void downloadFile_notFound_throws() {
            assertThatThrownBy(() -> fileService.downloadFile("nonexistent.txt"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File not found");
        }

        @Test
        @DisplayName("TC-FILE-007 [P] Download file nhi phan – noi dung chinh xac")
        void downloadFile_binaryFile_returnsExactBytes() throws IOException {
            byte[] binaryData = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
            Path file = tempDir.resolve("binary.dat");
            Files.write(file, binaryData);

            byte[] result = fileService.downloadFile("binary.dat");

            assertThat(result).isEqualTo(binaryData);
        }
    }
}
