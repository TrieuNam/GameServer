# GameServer Monitoring & Logging Guide

## 📊 Tổng quan

Hệ thống monitoring và logging đã được tích hợp đầy đủ với:

1. **Control Panel**: Xem logs realtime của từng service
2. **Prometheus**: Thu thập metrics từ tất cả services
3. **Grafana**: Dashboard visualization  
4. **Loki**: Log aggregation (optional)

---

## 🚀 Bước 1: Start Monitoring Stack

```powershell
cd D:\project\serverGame\GameServer\scripts
.\start-monitoring.ps1
```

Hoặc thủ công:
```powershell
cd D:\project\serverGame\GameServer\docker
docker-compose -f docker-compose.local-full.yml up -d prometheus grafana loki
```

**Access URLs:**
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Loki**: http://localhost:3100

---

## 📋 Bước 2: Xem Logs trong Control Panel

1. Start admin-service:
   ```powershell
   cd D:\project\serverGame\GameServer\admin-service
   java -jar target\admin-service-1.0.0.jar
   ```

2. Truy cập Control Panel: http://localhost:9091

3. Bấm nút **"📋 Logs"** trên service card

4. **Features**:
   - ✅ Auto-refresh mỗi 3 giây
   - ✅ Chọn số dòng: 50/100/200/500/1000
   - ✅ Highlight ERROR/WARN/INFO
   - ✅ Download logs to file
   - ✅ Real-time updates

---

## 📊 Bước 3: Xem Metrics trong Prometheus

1. Truy cập: http://localhost:9090/targets

2. Check services status (UP/DOWN)

3. Query metrics examples:
   ```promql
   # CPU Usage
   process_cpu_usage{service="admin-service"}
   
   # Memory Usage %
   jvm_memory_used_bytes / jvm_memory_max_bytes * 100
   
   # HTTP Request Rate
   rate(http_server_requests_seconds_count[5m])
   
   # Service Uptime
   process_uptime_seconds
   ```

---

## 📈 Bước 4: Xem Dashboard trong Grafana

1. Truy cập: http://localhost:3000
   - Username: `admin`
   - Password: `admin123`

2. Navigate: **Home → Dashboards → GameServer Monitoring Dashboard**

3. **Dashboard bao gồm**:
   - Services Status (UP/DOWN)
   - CPU Usage by Service
   - Memory Usage by Service  
   - HTTP Requests Rate
   - Error Rate
   - Response Time

4. **Filter by Phase**: P0, P1, P2, ADMIN

---

## 🔧 API Endpoints

### Get Service Logs
```http
GET /api/services/{serviceName}/logs?lines=100
```

**Response:**
```json
{
  "serviceName": "user-service",
  "lines": 100,
  "logs": [
    "2026-01-24 22:00:00 INFO  Starting UserServiceApplication",
    "2026-01-24 22:00:01 INFO  Connected to database"
  ],
  "timestamp": "2026-01-24T22:00:00"
}
```

### Get Service Status
```http
GET /api/services/{serviceName}/status
```

### Get All Services
```http
GET /api/services
```

### Start Service
```http
POST /api/services/{serviceName}/start
```

### Start Phase
```http
POST /api/services/phase/P0/start
```

---

## 🎯 Use Cases

### 1. Service bị lỗi - Debug ngay
1. Mở Control Panel
2. Bấm "📋 Logs" trên service bị lỗi
3. Xem ERROR logs realtime
4. Download logs để phân tích chi tiết

### 2. Monitor Performance
1. Mở Grafana Dashboard
2. Xem CPU/Memory usage của tất cả services
3. Check request rate và response time
4. Alert khi service có vấn đề

### 3. Aggregate Logs từ nhiều services
1. Mở Grafana
2. Add Loki datasource
3. Query logs: `{service="user-service"} |= "ERROR"`
4. Filter by time range, service, phase

---

## 📁 Cấu trúc Files

```
GameServer/
├── admin-service/
│   ├── src/.../controller/ServiceController.java  # Logs API
│   ├── src/.../service/ServiceManager.java        # Logs collection
│   └── src/.../resources/static/control-panel.html  # UI with logs viewer
├── docker/
│   ├── docker-compose.local-full.yml  # Monitoring stack
│   ├── prometheus/
│   │   └── prometheus.yml             # Scraping config
│   └── grafana/
│       └── provisioning/
│           ├── datasources/           # Prometheus + Loki
│           └── dashboards/            # GameServer dashboard
└── scripts/
    └── start-monitoring.ps1           # One-click setup
```

---

## ⚙️ Configuration

### Thêm Service mới vào Prometheus

Edit [prometheus/prometheus.yml](d:\project\serverGame\GameServer\docker\prometheus\prometheus.yml):

```yaml
scrape_configs:
  - job_name: 'my-new-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8888']
        labels:
          service: 'my-new-service'
          phase: 'P1'
```

### Enable Metrics trong Spring Boot Service

**pom.xml:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**application.yml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 🛠️ Troubleshooting

### Logs không hiển thị?
- Check service có đang chạy: `GET /api/services/{serviceName}/status`
- Verify ServiceManager đang track process
- Check logs trong terminal khi start service

### Prometheus không scrape được service?
- Verify service expose `/actuator/prometheus`
- Check network: `curl http://localhost:9091/actuator/prometheus`
- Check prometheus targets: http://localhost:9090/targets

### Grafana không kết nối Prometheus?
- Check datasource config trong Grafana
- Verify Prometheus URL: `http://prometheus:9090`
- Test connection trong Grafana UI

---

## 📚 Resources

- **Prometheus Docs**: https://prometheus.io/docs/
- **Grafana Docs**: https://grafana.com/docs/
- **Loki Docs**: https://grafana.com/docs/loki/
- **Micrometer Docs**: https://micrometer.io/docs/

---

## ✅ Quick Start Checklist

- [ ] Start monitoring stack: `.\start-monitoring.ps1`
- [ ] Start admin-service với Prometheus enabled
- [ ] Verify Prometheus targets: http://localhost:9090/targets
- [ ] Login Grafana và explore dashboard
- [ ] Test logs viewer trong Control Panel
- [ ] Start P0 services và monitor metrics
- [ ] Check logs khi service có ERROR

---

**🎉 Happy Monitoring!**
