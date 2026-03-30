# 🎯 Giảm JAR Size KHÔNG Xóa Thư Viện

## 📋 Tổng Quan
Bạn có 51 services, mỗi service ~100-290 MB. Tổng ~5.4GB
**Mục tiêu**: Giảm xuống 30-50 MB/service (~2GB total) **KHÔNG xóa thư viện**

---

## ✅ 1. Maven Shade Plugin với minimizeJar (RECOMMENDED)
**Hiệu quả**: ⭐⭐⭐⭐⭐ (Tiết kiệm 30-50%)
**Độ khó**: Trung bình

### Nguyên lý:
- Phân tích bytecode, loại bỏ **classes không dùng** trong dependencies
- Relocate packages để tránh conflict
- Giữ lại dependencies nhưng chỉ classes thực sự dùng

### Implementation:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <!-- QUAN TRỌNG: Loại bỏ classes không dùng -->
                        <minimizeJar>true</minimizeJar>
                        
                        <!-- Giữ lại classes cần thiết -->
                        <filters>
                            <filter>
                                <artifact>*:*</artifact>
                                <excludes>
                                    <exclude>META-INF/*.SF</exclude>
                                    <exclude>META-INF/*.DSA</exclude>
                                    <exclude>META-INF/*.RSA</exclude>
                                    <exclude>META-INF/maven/**</exclude>
                                    <exclude>**/module-info.class</exclude>
                                </excludes>
                            </filter>
                            
                            <!-- Giữ lại classes Spring Boot cần -->
                            <filter>
                                <artifact>org.springframework.boot:*</artifact>
                                <includes>
                                    <include>**</include>
                                </includes>
                            </filter>
                            
                            <!-- Giữ lại Jackson cho reflection -->
                            <filter>
                                <artifact>com.fasterxml.jackson.*:*</artifact>
                                <includes>
                                    <include>**</include>
                                </includes>
                            </filter>
                        </filters>
                        
                        <!-- Transformer quan trọng -->
                        <transformers>
                            <!-- Spring Boot executable JAR -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                <mainClass>${start-class}</mainClass>
                            </transformer>
                            
                            <!-- Merge Spring handlers -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                <resource>META-INF/spring.handlers</resource>
                            </transformer>
                            
                            <!-- Merge Spring schemas -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                <resource>META-INF/spring.schemas</resource>
                            </transformer>
                            
                            <!-- Merge Spring factories (AutoConfiguration) -->
                            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                <resource>META-INF/spring.factories</resource>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### ⚠️ Lưu Ý Quan Trọng:
- Spring Boot reflection: Phải giữ lại filters cho Spring Boot và Jackson
- gRPC services: Thêm filter cho protobuf
- JAXB/XML: Giữ lại javax.xml.bind classes

---

## ✅ 2. Spring Boot Thin Launcher (BEST for 51 services)
**Hiệu quả**: ⭐⭐⭐⭐⭐ (Tiết kiệm 80-90%)
**Độ khó**: Dễ

### Nguyên lý:
- **Tách dependencies** ra khỏi JAR
- Tất cả services **share chung** dependencies folder
- Mỗi JAR chỉ còn **code của service đó** (~5-10 MB)

### Implementation:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <dependencies>
                <!-- Thin Launcher -->
                <dependency>
                    <groupId>org.springframework.boot.experimental</groupId>
                    <artifactId>spring-boot-thin-layout</artifactId>
                    <version>1.0.31.RELEASE</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

### Kích Hoạt:
```bash
# Build thin JARs (5-10 MB mỗi cái)
mvn clean package

# Download dependencies lần đầu (1 lần cho tất cả 51 services)
java -Dthin.root=. -jar user-service-1.0.0.jar --thin.dryrun

# Run bình thường
java -Dthin.root=. -jar user-service-1.0.0.jar
```

### Docker Compose:
```yaml
services:
  user-service:
    environment:
      - THIN_ROOT=/app/repository  # Shared folder
    volumes:
      - maven-repo:/app/repository  # All services share này

volumes:
  maven-repo:  # Chứa tất cả dependencies chung
```

### 📊 Kết Quả:
- **Trước**: 51 services × 100 MB = 5.1 GB
- **Sau**: 51 services × 10 MB + 500 MB shared = **1 GB**
- **Tiết kiệm**: 80%

---

## ✅ 3. ProGuard - Shrink Bytecode
**Hiệu quả**: ⭐⭐⭐⭐ (Tiết kiệm 20-30%)
**Độ khó**: Khó

### Nguyên lý:
- Loại bỏ unused code ở **bytecode level**
- Obfuscate class names (giảm string size)
- Optimize bytecode

### Implementation:
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
        <outjar>${project.build.finalName}-small.jar</outjar>
        
        <options>
            <!-- Không obfuscate (giữ tên class gốc) -->
            <option>-dontobfuscate</option>
            
            <!-- Shrink: loại bỏ unused code -->
            <option>-dontshrink</option>
            
            <!-- Giữ lại Spring Boot classes -->
            <option>-keep class org.springframework.boot.** { *; }</option>
            <option>-keep @org.springframework.stereotype.** class * { *; }</option>
            <option>-keep @org.springframework.web.bind.annotation.** class * { *; }</option>
            
            <!-- Giữ lại Main class -->
            <option>-keep class ${start-class} { *; }</option>
        </options>
    </configuration>
</plugin>
```

---

## ✅ 4. Chuyển Sang Undertow (Thay Tomcat)
**Hiệu quả**: ⭐⭐⭐ (Tiết kiệm 2-3 MB/service = 100-150 MB total)
**Độ khó**: Dễ

### Implementation:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- Loại bỏ Tomcat -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Thêm Undertow -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### ✅ Ưu điểm:
- Nhẹ hơn Tomcat (2-3 MB)
- Performance tốt hơn
- Memory footprint nhỏ hơn

---

## ✅ 5. Pack200 Compression (Legacy Java)
**Hiệu quả**: ⭐⭐ (Tiết kiệm 5-10%)
**Độ khó**: Dễ

⚠️ **LƯU Ý**: Pack200 deprecated trong Java 14+, không dùng được với Java 21

---

## ✅ 6. JAR Compression - xz/zstd
**Hiệu quả**: ⭐⭐⭐ (Tiết kiệm 10-15%)
**Độ khó**: Dễ

### Docker Image Build:
```dockerfile
FROM openjdk:21-slim

# Install xz-utils
RUN apt-get update && apt-get install -y xz-utils

# Copy và nén JAR
COPY target/*.jar app.jar
RUN xz -9 -k app.jar

# Run: giải nén và execute
CMD xz -d -c app.jar.xz > app.jar && java -jar app.jar
```

---

## ✅ 7. Spring Boot Layered JARs + Docker
**Hiệu quả**: ⭐⭐⭐⭐ (Docker image size, rebuild time)
**Độ khó**: Dễ

### pom.xml:
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

### Dockerfile:
```dockerfile
FROM openjdk:21-slim as builder
WORKDIR /app
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM openjdk:21-slim
WORKDIR /app
# Copy layers (dependencies cache được)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

---

## ✅ 8. CDS (Class Data Sharing)
**Hiệu quả**: ⭐⭐⭐ (Startup time, memory)
**Độ khó**: Trung bình

### Nguyên lý:
- Pre-load và share classes giữa các JVM instances
- Giảm memory footprint khi chạy nhiều services cùng lúc

### Implementation:
```bash
# 1. Generate CDS archive
java -Xshare:dump -XX:SharedArchiveFile=app-cds.jsa -jar user-service.jar

# 2. Run với CDS
java -Xshare:on -XX:SharedArchiveFile=app-cds.jsa -jar user-service.jar
```

---

## ✅ 9. GraalVM Native Image (Advanced)
**Hiệu quả**: ⭐⭐⭐⭐⭐ (Tiết kiệm 50-70%, startup < 100ms)
**Độ khó**: Cao

### ⚠️ Hạn chế:
- Không hỗ trợ reflection tốt
- Build time rất lâu (10-15 phút/service)
- Phức tạp với Spring Boot

### Implementation:
```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.0</version>
</plugin>
```

---

## ✅ 10. Exclude Unused AutoConfiguration
**Hiệu quả**: ⭐⭐ (Tiết kiệm 1-2 MB)
**Độ khó**: Dễ

### application.yml:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      # Thêm các AutoConfig không dùng
```

---

## 📊 So Sánh Hiệu Quả

| Cách                      | Tiết Kiệm | Độ Khó | Recommend | Áp dụng cho 51 services |
|---------------------------|-----------|--------|-----------|-------------------------|
| **Thin Launcher**         | 80-90%    | Dễ     | ⭐⭐⭐⭐⭐   | 5.1GB → 1GB             |
| **Maven Shade minimizeJar** | 30-50%  | TB     | ⭐⭐⭐⭐    | 5.1GB → 2.5-3.5GB       |
| **ProGuard**              | 20-30%    | Khó    | ⭐⭐⭐     | 5.1GB → 3.5-4GB         |
| **Undertow thay Tomcat**  | 2-3 MB    | Dễ     | ⭐⭐⭐⭐    | Tiết kiệm 100-150MB     |
| **Layered JARs**          | Cache     | Dễ     | ⭐⭐⭐⭐    | Docker rebuild nhanh    |
| **GraalVM Native**        | 50-70%    | Cao    | ⭐⭐       | Phức tạp, không linh hoạt |

---

## 🎯 Khuyến Nghị Implement

### 🏆 Plan A: QUICK WINS (1-2 giờ)
1. **Undertow thay Tomcat** (tất cả 51 services)
2. **Layered JARs** trong Docker (build time nhanh hơn)
3. Exclude unused AutoConfiguration

**Kết quả**: 5.1GB → 4.8GB, Docker rebuild nhanh hơn 3-5x

---

### 🏆 Plan B: BALANCED (1 ngày)
1. **Maven Shade minimizeJar** (10 services nặng nhất)
2. **Undertow** (41 services còn lại)
3. **Layered JARs** (Docker)

**Kết quả**: 5.1GB → 3GB (giảm 40%)

---

### 🏆 Plan C: ULTIMATE (2-3 ngày)
1. **Thin Launcher** cho TẤT CẢ 51 services
2. **Layered JARs** trong Docker
3. **CDS** cho production

**Kết quả**: 5.1GB → 1GB (giảm 80%), startup nhanh hơn, memory thấp hơn

---

## 🚀 Implement Ngay

### Option 1: Thin Launcher (RECOMMENDED)
```bash
# Apply cho tất cả services
cd GameServer
./apply-thin-launcher.sh
```

### Option 2: Maven Shade
```bash
# Apply cho top 10 heaviest services
cd GameServer
./apply-shade-plugin.sh --top=10
```

### Option 3: Undertow
```bash
# Replace Tomcat với Undertow
cd GameServer
./switch-to-undertow.sh --all
```

---

## 📞 Chọn Plan Nào?

**Bạn muốn implement plan nào?**
- **Plan A**: Quick wins, 1-2 giờ, giảm 6%
- **Plan B**: Balanced, 1 ngày, giảm 40%
- **Plan C**: Ultimate, 2-3 ngày, giảm 80%

Hoặc tôi có thể **demo 1 service** với từng cách để bạn thấy kết quả?
