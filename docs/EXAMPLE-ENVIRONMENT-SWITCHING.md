# Example: Switching Between Local and Production

## Scenario

Bạn đang phát triển `analytics-service` trên local machine, và cần deploy lên production server mà **không rebuild**.

## Step 1: Development (Local)

### Build service
```powershell
cd D:\project\serverGame\GameServer\analytics-service
mvn clean package -DskipTests
```

**Output:** `target\analytics-service-1.0.0.jar` (32 MB)

### Run locally với default config
```powershell
java -jar target\analytics-service-1.0.0.jar
```

**Config used:**
- Profile: `local` (default từ application.yml hoặc auto-detect)
- Database: `jdbc:mysql://127.0.0.1:33092/game_analytics`
- Username: `tpnam` (từ application-local.yml)
- Password: `121831` (từ application-local.yml)

### Hoặc dùng helper script
```powershell
cd D:\project\serverGame\GameServer
.\start-service-local.ps1 -Service analytics-service
```

### Test service
```powershell
# Verify service started
curl http://localhost:8510/actuator/health

# Response: {"status":"UP"}
```

---

## Step 2: Deploy to Production (Same JAR!)

### Copy JAR to production server
```powershell
# Local machine
scp target\analytics-service-1.0.0.jar user@prod-server:/opt/services/
```

### On production server
```bash
cd /opt/services

# Set production environment variables
export SPRING_PROFILES_ACTIVE=prod
export DB_URL="jdbc:mysql://prod-db.internal:3306/game_analytics"
export DB_USERNAME="analytics_prod"
export DB_PASSWORD="SecureProductionPassword123"

# Run SAME jar file
java -jar analytics-service-1.0.0.jar
```

**Config used:**
- Profile: `prod` (từ SPRING_PROFILES_ACTIVE)
- Database: `jdbc:mysql://prod-db.internal:3306/game_analytics` (từ DB_URL)
- Username: `analytics_prod` (từ DB_USERNAME)
- Password: `SecureProductionPassword123` (từ DB_PASSWORD)

**🎉 Không cần rebuild! Cùng file JAR nhưng config khác.**

---

## Step 3: Switching Back to Local for Testing

### Stop production service
```bash
# On production server
pkill -f analytics-service-1.0.0.jar
```

### Back to local machine
```powershell
# Clear any production env vars
Remove-Item Env:DB_URL -ErrorAction SilentlyContinue
Remove-Item Env:DB_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue

# Run local
cd D:\project\serverGame\GameServer\analytics-service
java -jar target\analytics-service-1.0.0.jar --spring.profiles.active=local
```

---

## Step 4: Using Docker (Production)

### Create Dockerfile
```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/analytics-service-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build image
```powershell
docker build -t analytics-service:1.0.0 .
```

### Run with environment variables
```powershell
docker run -d \
  -p 8510:8510 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:mysql://mysql:3306/game_analytics" \
  -e DB_USERNAME="gameuser" \
  -e DB_PASSWORD="dbpass123" \
  --name analytics-service \
  analytics-service:1.0.0
```

### Using docker-compose
```yaml
# docker-compose.yml
version: '3.8'

services:
  analytics-service:
    image: analytics-service:1.0.0
    ports:
      - "8510:8510"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/game_analytics
      DB_USERNAME: gameuser
      DB_PASSWORD: ${DB_PASSWORD}  # From .env file
    depends_on:
      - mysql
  
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: game_analytics
      MYSQL_USER: gameuser
      MYSQL_PASSWORD: dbpass123
    ports:
      - "3306:3306"
```

```powershell
# Run
docker-compose up -d

# Logs
docker-compose logs -f analytics-service
```

---

## Step 5: Multiple Environments (Staging + Production)

### Staging server
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL="jdbc:mysql://staging-db:3306/game_analytics"
export DB_USERNAME="staging_user"
export DB_PASSWORD="StagingPassword"

java -jar analytics-service-1.0.0.jar
```

### Production server
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL="jdbc:mysql://prod-db:3306/game_analytics"
export DB_USERNAME="prod_user"
export DB_PASSWORD="ProductionPassword"

java -jar analytics-service-1.0.0.jar
```

**🎯 Same JAR, different databases, no rebuild!**

---

## Step 6: Kubernetes Deployment

### Create secret
```bash
kubectl create secret generic analytics-db-credentials \
  --from-literal=url='jdbc:mysql://mysql-service:3306/game_analytics' \
  --from-literal=username='analytics_prod' \
  --from-literal=password='SecureK8sPassword'
```

### Deployment manifest
```yaml
# k8s/analytics-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: analytics-service
  namespace: game-services
spec:
  replicas: 3
  selector:
    matchLabels:
      app: analytics-service
  template:
    metadata:
      labels:
        app: analytics-service
    spec:
      containers:
      - name: analytics-service
        image: analytics-service:1.0.0
        ports:
        - containerPort: 8510
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: analytics-db-credentials
              key: url
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: analytics-db-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: analytics-db-credentials
              key: password
        resources:
          limits:
            memory: "512Mi"
            cpu: "500m"
          requests:
            memory: "256Mi"
            cpu: "250m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8510
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8510
          initialDelaySeconds: 30
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: analytics-service
  namespace: game-services
spec:
  selector:
    app: analytics-service
  ports:
  - protocol: TCP
    port: 8510
    targetPort: 8510
  type: ClusterIP
```

### Deploy
```bash
kubectl apply -f k8s/analytics-service-deployment.yaml
kubectl get pods -n game-services
kubectl logs -f deployment/analytics-service -n game-services
```

---

## Summary

| Environment | Profile | DB URL | Credentials | JAR File | Build Required? |
|-------------|---------|--------|-------------|----------|-----------------|
| **Local** | `local` | localhost:33092 | tpnam/121831 | analytics-service-1.0.0.jar | ✅ Initial build |
| **Staging** | `prod` | staging-db:3306 | From env vars | Same JAR | ❌ No |
| **Production** | `prod` | prod-db:3306 | From env vars | Same JAR | ❌ No |
| **Docker** | `prod` | mysql:3306 | From env vars | Same JAR | ❌ No |
| **Kubernetes** | `prod` | mysql-service:3306 | From secrets | Same JAR | ❌ No |

## Key Takeaway

🎯 **Build once, deploy everywhere với configs khác nhau!**  
✅ Không cần rebuild khi chuyển môi trường  
✅ Dễ dàng rollback - cùng JAR file  
✅ CI/CD đơn giản hơn  
✅ Bảo mật tốt hơn - credentials không hardcode  
