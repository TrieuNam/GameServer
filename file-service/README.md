# File Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8540  
**Database**: N/A (Stateless — filesystem)

---

## 📋 Overview

File Service quản lý **upload, download và phục vụ file tài nguyên game** — game assets, avatar images, APK updates, patch files. Stateless service vì data lưu trên filesystem hoặc cloud storage.

### Core Features
- ✅ Upload game assets (images, audio, data files)
- ✅ Download với secure URL
- ✅ Static resource serving
- ✅ File metadata management
- ✅ CDN integration support

---

## 🎯 Flow Upload/Download

```
[Game Designer upload asset mới]
POST /api/file/upload (multipart/form-data)
        │
        ▼
file-service
├── Validate file type và size
├── Lưu file vào storage (local / S3 / CDN)
├── Generate unique fileName
└── Trả về: { fileName, downloadUrl }

[Client download patch file]
GET /api/file/download/{fileName}
        │
        ▼
├── Validate fileName
├── Stream file từ storage
└── Trả về file content với proper Content-Type
```

---

## 🔌 API Endpoints

```
POST  /api/file/upload                 - Upload file
GET   /api/file/download/{fileName}   - Download file
```

---

## 📦 API Examples

### Upload File
```bash
curl -X POST http://localhost:8540/api/file/upload \
  -F "file=@/path/to/patch_v1.2.apk" \
  -F "type=patch"
# Response: { "fileName": "patch_v1.2_20260316.apk", "downloadUrl": "/api/file/download/..." }
```

### Download File
```bash
curl http://localhost:8540/api/file/download/patch_v1.2_20260316.apk \
  -o patch_v1.2.apk
```

---

## ⚙️ Configuration

```yaml
file-service:
  storage-type: local          # local hoặc s3 hoặc cdn
  base-path: /data/game-files  # Local storage path
  max-file-size: 500MB
  allowed-types: apk, zip, json, png, jpg, ogg, mp3
  
# S3 config (nếu dùng cloud)
aws:
  s3:
    bucket: game-assets-bucket
    region: ap-southeast-1
```

---

## 🔧 Business Logic

### File Types
| Type | Mô tả | Max Size |
|------|-------|----------|
| `patch` | APK/game patch files | 500MB |
| `asset` | Game assets (images, audio) | 50MB |
| `data` | Config JSON/binary | 10MB |
| `avatar` | Player avatar images | 2MB |

### Security
- File type validation (by extension và MIME type)
- Virus scanning integration (optional)
- Signed URLs cho sensitive files (expires in 1h)

---

## 🚀 Running

```bash
cd GameServer/file-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **admin-service**: Upload game assets
- **client**: Download patches và assets

---

## 📊 Statistics

```
Storage:         Filesystem / AWS S3 / CDN
Controllers:     1 class (FileController)
Services:        1 class (FileService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~300 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22

