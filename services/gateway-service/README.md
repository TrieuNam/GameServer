Gateway service (skeleton)

Purpose
- Acts as the API gateway and entry-point for clients. Responsibilities:
  - Routing to downstream services
  - JWT authentication/authorization filter
  - Rate limiting

Quick start (development)
1. This README is a placeholder. Create a Spring Boot project (Maven/Gradle) with the following dependencies:
   - Spring Boot Web
   - Spring Cloud Gateway
   - Spring Security (JWT filters)
   - Spring Cloud Netflix Eureka Client

2. Example application.properties (development):
```
spring.application.name=gateway-service
server.port=8080
spring.cloud.config.uri=http://config-service:8888
eureka.client.serviceUrl.defaultZone=http://eureka:8761/eureka/
```

3. Build & run (example using Maven):
```
mvn -f gateway-service/pom.xml spring-boot:run
```

Routing example (application.yml):
```
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
```

Next steps I can do for you
- Scaffold a complete Spring Boot gateway skeleton in `services/gateway-service` (pom.xml, main app, sample filter).
- Add Dockerfile and build pipeline for the gateway.