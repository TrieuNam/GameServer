# Eureka Server

**Version**: 1.0.0  
**Phase**: P0 (Core Infrastructure)  
**Port**: 8761  
**Database**: N/A (In-memory)

---

## 📋 Overview

Eureka Server là **Service Discovery** trung tâm của hệ thống microservices. Tất cả các services đăng ký vào đây khi khởi động và tra cứu địa chỉ của nhau thông qua Eureka, cho phép load balancing và high availability tự động.

### Core Features
- ✅ Service registration & discovery
- ✅ Health monitoring cho tất cả services
- ✅ Load balancing discovery (Round-robin)
- ✅ Dashboard web quản lý
- ✅ Self-preservation mode
- ✅ Peer-to-peer replication (multi-node)

---

## 🎯 Vai Trò Trong Hệ Thống

```
[Mọi Service] ──register──► [Eureka Server :8761]
                                      │
[Gateway / Feign Client] ──lookup──► [Service Registry]
                                      │
                              ◄── Trả về danh sách instances
```

Eureka là **first service** phải chạy trước tất cả services khác.

---

## 🔌 Endpoints

> ⚠️ Các endpoint dưới đây được **Spring Cloud Eureka tự động cung cấp**, không cần viết controller.

| Method | Path | Mô tả |
|--------|------|--------|
| GET | /eureka/apps | Danh sách tất cả registered services |
| GET | /eureka/apps/{appName} | Thông tin service cụ thể |
| GET | /actuator/health | Health check |
| GET | / | Dashboard web (Eureka UI) |

### Dashboard
- **URL**: http://localhost:8761
- Hiển thị tất cả services đã đăng ký, trạng thái (UP/DOWN), instance info

---

## ⚙️ Configuration

```yaml
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false  # Không tự đăng ký chính mình
    fetch-registry: false         # Không fetch registry từ peer
  server:
    enable-self-preservation: true
    eviction-interval-timer-in-ms: 60000
```

---

## 🔧 Business Logic

### Self-Preservation Mode
- Khi > 85% instances gửi heartbeat → chế độ bình thường
- Khi < 85% instances gửi heartbeat → kích hoạt self-preservation (không xóa instances)
- Ngăn chặn mass eviction khi mạng tạm thời gián đoạn

### Heartbeat
- Services gửi heartbeat mỗi **30 giây**
- Nếu không nhận heartbeat trong **90 giây** → instance bị xóa
- Eviction check chạy mỗi **60 giây**

---

## 🚀 Running

```bash
cd GameServer/eureka-server
mvn clean install
mvn spring-boot:run
```

> ⚠️ **Luôn khởi động TRƯỚC TẤT CẢ services khác**

---

## 🔗 Integration Points

### Services đăng ký vào Eureka (tất cả ~57 services)
```yaml
# Cấu hình mỗi service cần có:
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

## 📊 Statistics

```
Configuration:   1 file
Framework:       Spring Cloud Netflix Eureka Server
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~50 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22

