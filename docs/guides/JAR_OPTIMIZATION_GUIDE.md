# ⚡ Hướng dẫn Tối ưu JAR Size - Giảm từ 100MB xuống 30-50MB

## 📊 Hiện trạng
```
Gateway:     290MB  ❌ Quá nặng!
Bag:         133MB  ❌
Role:        132MB  ❌
User:        113MB  ❌
Average:     ~108MB ❌
```

## 🎯 Mục tiêu
```
Gateway:     50-60MB  ✅
Services:    30-40MB  ✅
Average:     ~35MB    ✅
Tiết kiệm:   ~70%     🎉
```

## 🔍 Nguyên nhân JAR nặng

### 1. **Spring Boot Fat JAR mặc định**
- Đóng gói TẤT CẢ dependencies vào 1 JAR
- Bao gồm cả dependencies không dùng

### 2. **Duplicate Dependencies**
- Mỗi service đều có: Spring Boot, Netty, gRPC, Jackson, etc
- 50 services = 50 lần lặp lại các lib giống nhau

### 3. **Unnecessary Dependencies**
- Spring Boot Autoconfigure: ~10MB
- Netty (embedded): ~5-10MB  
- Tomcat embedded: ~10MB
- Hibernate: ~8MB
- Jackson: ~2MB

### 4. **Debug Info & Source**
- Classes chứa debug info
- Không strip bytecode

## ✅ Giải pháp

### **Option 1: Thin JAR với Shared Libs (RECOMMENDED)** ⭐

Tách dependencies ra ngoài, share giữa các services.

**Cấu trúc:**
```
/app
  /lib/           # Shared libs (download 1 lần)
    spring-boot-3.5.3.jar
    grpc-1.61.0.jar
    mysql-connector.jar
    ...
  /services/      # Thin JARs (chỉ code của bạn)
    user-service.jar    (5-10MB)
    role-service.jar    (5-10MB)
    wallet-service.jar  (5-10MB)
```

**Lợi ích:**
- Service JAR: 5-10MB (chỉ code)
- Shared libs: 80MB (tải 1 lần, dùng chung)
- Total: ~300MB cho 50 services (vs 5GB hiện tại!)

### **Option 2: Spring Boot Layered JARs**

Tách JAR thành layers để Docker cache tốt hơn.

### **Option 3: ProGuard Shrinking** 

Loại bỏ bytecode không dùng (advanced).

---

## 🚀 Implementation

### **OPTION 1: Thin JAR (Recommended)**

#### Bước 1: Thêm vào parent `pom.xml`

Thêm profile `thin` vào file `/GameServer/pom.xml`:

```xml
<profiles>
    <!-- Profile for thin JAR build -->
    <profile>
        <id>thin</id>
        <activation>
            <activeByDefault>false</activeByDefault>
        </activation>
        <build>
            <pluginManagement>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <configuration>
                            <layout>ZIP</layout>
                            <excludeGroupIds>
                                org.springframework.boot,
                                org.springframework,
                                io.grpc,
                                com.google.protobuf,
                                io.netty,
                                com.fasterxml.jackson.core,
                                mysql,
                                org.hibernate,
                                org.flywaydb,
                                redis.clients
                            </excludeGroupIds>
                        </configuration>
                    </plugin>
                </plugins>
            </pluginManagement>
        </build>
    </profile>
</profiles>
```

#### Bước 2: Build với thin profile

```bash
# Build tất cả services với thin JARs
mvn clean package -Pthin -DskipTests

# Build 1 service
cd user-service
mvn clean package -Pthin -DskipTests
```

#### Bước 3: Tạo shared lib folder

```bash
# Extract dependencies 1 lần
cd user-service
mvn dependency:copy-dependencies -DoutputDirectory=../shared-libs -DincludeScope=runtime
```

#### Bước 4: Update Dockerfile

Tạo `Dockerfile.thin`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy shared libs (layer sẽ được cache)
COPY shared-libs/ /app/lib/

# Copy thin JAR
ARG JAR_FILE
COPY ${JAR_FILE} app.jar

# Run với classpath
ENTRYPOINT ["java", "-cp", "/app/lib/*:/app/app.jar", "com.SouthMillion.user.UserServiceApplication"]
```

---

### **OPTION 2: Exclude Unused Dependencies**

#### Tối ưu `spring-boot-starter-web`

Loại bỏ Tomcat nếu không dùng REST API:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- Exclude Tomcat, dùng Netty -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
        <!-- Exclude Jackson XML -->
        <exclusion>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Thêm Netty (nhẹ hơn Tomcat) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### Tối ưu Hibernate

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <exclusions>
        <!-- Exclude Hibernate Validator nếu không validate phức tạp -->
        <exclusion>
            <groupId>org.hibernate.validator</groupId>
            <artifactId>hibernate-validator</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### Tối ưu gRPC

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <exclusions>
        <!-- Exclude debug info -->
        <exclusion>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-services</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

### **OPTION 3: Maven Dependency Plugin**

#### Analyze dependencies

```bash
cd user-service
mvn dependency:analyze

# Output sẽ show:
# - Used declared dependencies
# - Unused declared dependencies  ← XÓA CÁI NÀY!
# - Used undeclared dependencies
```

#### Xóa unused dependencies

```bash
# List unused
mvn dependency:analyze | grep "Unused declared"

# Xóa khỏi pom.xml
```

---

### **OPTION 4: Spring Boot Layered JARs**

Tối ưu cho Docker build cache.

#### Cấu hình trong pom.xml

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

#### Dockerfile multi-stage

```dockerfile
# Stage 1: Extract layers
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy layers (cache-friendly)
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

---

### **OPTION 5: ProGuard Shrinking (Advanced)**

Xóa bytecode không dùng.

```xml
<plugin>
    <groupId>com.github.wvengen</groupId>
    <artifactId>proguard-maven-plugin</artifactId>
    <version>2.6.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>proguard</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <proguardVersion>7.3.2</proguardVersion>
        <injar>${project.build.finalName}.jar</injar>
        <outjar>${project.build.finalName}-min.jar</outjar>
        <libs>
            <lib>${java.home}/jmods</lib>
        </libs>
        <options>
            <option>-dontshrink</option>
            <option>-dontoptimize</option>
            <option>-keep class com.SouthMillion.** { *; }</option>
            <option>-keep class org.springframework.** { *; }</option>
        </options>
    </configuration>
</plugin>
```

---

## 📊 So sánh các phương án

| Method | JAR Size | Complexity | Docker Cache | Recommended |
|--------|----------|------------|--------------|-------------|
| Fat JAR (default) | 100-130MB | ⭐ Easy | ❌ Poor | ❌ No |
| Thin JAR + Shared Libs | 5-10MB | ⭐⭐⭐ Medium | ✅ Excellent | ✅ **YES** |
| Layered JAR | 100-130MB | ⭐⭐ Easy | ✅ Good | ✅ Good |
| Exclude Dependencies | 70-90MB | ⭐⭐ Medium | ⚠️ Fair | ✅ Good |
| ProGuard | 50-70MB | ⭐⭐⭐⭐ Hard | ⚠️ Fair | ⚠️ Advanced |

---

## 🎯 Khuyến nghị

### **Short term (Ngay lập tức):**

1. **Exclude unused dependencies** - Dễ, hiệu quả ngay
2. **Layered JARs** - Improve Docker cache

### **Long term (Tốt nhất):**

3. **Thin JAR + Shared Libs** - Tiết kiệm tối đa

---

## 📝 Quick Commands

### Build thin JARs:
```bash
mvn clean package -Pthin -DskipTests
```

### Analyze dependencies:
```bash
mvn dependency:tree > deps.txt
mvn dependency:analyze
```

### Check JAR size:
```bash
ls -lh target/*.jar
```

### Extract shared libs:
```bash
mvn dependency:copy-dependencies -DoutputDirectory=shared-libs
```

---

## 📈 Kết quả dự kiến

### Fat JAR (hiện tại):
```
50 services × 108MB = 5.4GB
Docker images: ~15GB
```

### Thin JAR (sau tối ưu):
```
Shared libs: 80MB (1 lần)
50 services × 8MB = 400MB
Total: 480MB
Docker images: ~2GB
Tiết kiệm: ~85%! 🎉
```

---

## 🚨 Lưu ý

1. **Common-lib phải lightweight** - Đây là dependency chung của tất cả services
2. **Test kỹ sau khi exclude** - Đảm bảo không thiếu class
3. **CI/CD cần update** - Build script cần thay đổi
4. **Docker compose cần update** - Mount shared-libs volume

---

## 📚 Next Steps

1. Chạy `mvn dependency:analyze` cho từng service
2. Xóa unused dependencies
3. Implement thin JAR profile
4. Update Dockerfiles
5. Test thoroughly
6. Deploy!
