# Service Interaction Patterns

Cac pattern va mau code de services trong GameServer communicate voi nhau.

## Pattern 1: REST Call via Feign Client

### Khai Bao Client (Task Service goi User Service)

File: `task-service/src/main/java/com/SouthMillion/task_service/client/UserServiceClient.java`

```java
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
  name = "user-service",
  url = "${client.user-service.url:http://localhost:9016}",
  fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

  @GetMapping("/api/user/{id}")
  UserDTO getUserById(@PathVariable("id") String userId);

  @GetMapping("/api/user/email/{email}")
  UserDTO getUserByEmail(@PathVariable("email") String email);
}
```

### Fallback (Graceful Degradation)

```java
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserServiceClientFallback implements UserServiceClient {
  private static final Logger logger = LoggerFactory.getLogger(UserServiceClientFallback.class);

  @Override
  public UserDTO getUserById(String userId) {
    logger.warn("User service unavailable, returning empty user for id: {}", userId);
    return UserDTO.builder()
      .id(userId)
      .name("Unknown User")
      .build();
  }

  @Override
  public UserDTO getUserByEmail(String email) {
    logger.warn("User service unavailable, returning empty user for email: {}", email);
    return UserDTO.builder()
      .name("Unknown User")
      .build();
  }
}
```

### Usage in Service

```java
@Service
public class TaskDomainService {
  private final UserServiceClient userServiceClient;

  public TaskDomainService(UserServiceClient userServiceClient) {
    this.userServiceClient = userServiceClient;
  }

  public TaskDTO createTask(TaskDTO taskDTO) {
    // Validate user exists
    UserDTO user = userServiceClient.getUserById(taskDTO.getUserId());
    if (user == null) {
      throw new UserNotFoundException("User not found: " + taskDTO.getUserId());
    }

    // Create task
    Task task = new Task();
    task.setUserId(taskDTO.getUserId());
    task.setTitle(taskDTO.getTitle());
    task.setStatus(TaskStatus.PENDING);

    Task saved = taskRepository.save(task);
    return mapToDTO(saved);
  }
}
```

### Configuration in application.yml

```yaml
feign:
  client:
    config:
      user-service:
        connectTimeout: 10000
        readTimeout: 10000
        loggerLevel: full
        errorDecoder: com.SouthMillion.common.error.FeignErrorDecoder

client:
  user-service:
    url: http://localhost:9016
```

---

## Pattern 2: Event-Driven via Message Bus (RabbitMQ/Kafka)

### Producer (Task Service Publishes Event)

File: `task-service/src/main/java/com/SouthMillion/task_service/event/TaskEventPublisher.java`

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskEventPublisher {
  private final RabbitTemplate rabbitTemplate;  // For RabbitMQ
  // Or
  private final KafkaTemplate<String, Object> kafkaTemplate;  // For Kafka

  public TaskEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishTaskCreated(TaskDTO task) {
    TaskCreatedEvent event = new TaskCreatedEvent(
      task.getId(),
      task.getUserId(),
      task.getTitle(),
      System.currentTimeMillis()
    );

    // RabbitMQ
    rabbitTemplate.convertAndSend("task.exchange", "task.created", event);

    // Or Kafka
    // kafkaTemplate.send("task-events", task.getId(), event);
  }

  public void publishTaskCompleted(String taskId, String userId) {
    TaskCompletedEvent event = new TaskCompletedEvent(taskId, userId, System.currentTimeMillis());
    rabbitTemplate.convertAndSend("task.exchange", "task.completed", event);
  }
}
```

### Consumer (Notification Service Listens)

File: `notification-service/src/main/java/com/SouthMillion/notification_service/listener/TaskEventListener.java`

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TaskEventListener {
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;

  public TaskEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
  }

  @RabbitListener(queues = "task.created.queue")
  public void onTaskCreated(String message) {
    try {
      TaskCreatedEvent event = objectMapper.readValue(message, TaskCreatedEvent.class);
      notificationService.sendTaskCreatedNotification(event.getUserId(), event.getTaskTitle());
    } catch (Exception e) {
      logger.error("Failed to process task created event", e);
    }
  }

  @RabbitListener(queues = "task.completed.queue")
  public void onTaskCompleted(String message) {
    try {
      TaskCompletedEvent event = objectMapper.readValue(message, TaskCompletedEvent.class);
      notificationService.sendTaskCompletedNotification(event.getUserId());
    } catch (Exception e) {
      logger.error("Failed to process task completed event", e);
    }
  }
}
```

### Configuration

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Or for Kafka
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service-group
      auto-offset-reset: earliest
    producer:
      acks: all
```

### Event Classes

```java
public class TaskCreatedEvent {
  private String taskId;
  private String userId;
  private String taskTitle;
  private long timestamp;

  // Constructor, getters, setters
}

public class TaskCompletedEvent {
  private String taskId;
  private String userId;
  private long timestamp;

  // Constructor, getters, setters
}
```

---

## Pattern 3: Database Consistency (Saga Pattern)

Khi task service can update user service data:

```java
// Task Service
@Service
public class TaskService {
  private final UserServiceClient userServiceClient;
  private final TaskRepository taskRepository;
  private final TaskEventPublisher eventPublisher;

  public void completeTask(String taskId) {
    Task task = taskRepository.findById(taskId)
      .orElseThrow(() -> new TaskNotFoundException(taskId));

    // Step 1: Update task status locally
    task.setStatus(TaskStatus.COMPLETED);
    task.setCompletedAt(LocalDateTime.now());
    taskRepository.save(task);

    // Step 2: Publish event (user service listens and updates stats)
    eventPublisher.publishTaskCompleted(taskId, task.getUserId());

    // Step 3: Fallback if user service fails (data is still consistent)
    try {
      userServiceClient.incrementUserTaskCount(task.getUserId());
    } catch (Exception e) {
      logger.warn("Failed to increment user task count, will retry via event listener", e);
    }
  }
}
```

---

## Pattern 4: Service Discovery (If Using Eureka)

### Register Service (Trong pom.xml)

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Configuration

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
```

### Feign Client (No hardcoded URL)

```java
@FeignClient(
  name = "user-service",
  fallback = UserServiceClientFallback.class
  // URL is discovered via Eureka
)
public interface UserServiceClient {
  @GetMapping("/api/user/{id}")
  UserDTO getUserById(@PathVariable String id);
}
```

---

## Pattern 5: Retry & Circuit Breaker (Resilience4j)

### Configuration (pom.xml)

```xml
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot2</artifactId>
</dependency>
```

### Usage

```java
@Service
public class TaskService {
  
  @Retry(name = "userServiceRetry")
  @CircuitBreaker(name = "userServiceBreaker")
  public UserDTO getUser(String userId) {
    return userServiceClient.getUserById(userId);
  }
}
```

### Configuration

```yaml
resilience4j:
  retry:
    instances:
      userServiceRetry:
        max-attempts: 3
        wait-duration: 1000
  circuit-breaker:
    instances:
      userServiceBreaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000
        permitted-number-of-calls-in-half-open-state: 3
```

---

## Pattern 6: API Gateway (Route Services)

### Gateway Service Config

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: task-service
          uri: http://localhost:9015
          predicates:
            - Path=/task/**
          filters:
            - RewritePath=/task(?<segment>/?.*), /api/task$\{segment}
        - id: user-service
          uri: http://localhost:9016
          predicates:
            - Path=/user/**
          filters:
            - RewritePath=/user(?<segment>/?.*), /api/user$\{segment}
```

### Client Request Flow

```
Client
  ↓
Gateway (9001) → /task/create
  ↓
Task Service (9015) → /api/task/create
  ↓
Response → Client
```

---

## Best Practices

1. **Always use fallbacks** — kaya service mag-fail anytime
2. **Publish events async** — walang hard dependencies
3. **Retry with exponential backoff** — avoid thundering herd
4. **Log everything** — debug cross-service issues
5. **Monitor latency** — catch slow services early
6. **Use timeouts** — prevent hanging requests
7. **Versioning** — maintain backward compatibility sa APIs

---

## Common Mistakes to Avoid

❌ **Bad**: Blocking call to external service
```java
UserDTO user = userServiceClient.getUserById(userId);  // Can timeout, blocks thread
```

✅ **Good**: Async with fallback
```java
try {
  UserDTO user = userServiceClient.getUserById(userId);
} catch (Exception e) {
  logger.warn("User service unavailable, continuing with default");
  return defaultUserDTO;
}
```

❌ **Bad**: No error handling in event consumer
```java
@RabbitListener(queues = "task.queue")
public void onTaskEvent(TaskEvent event) {
  notificationService.sendNotification(event);  // If this fails, message is lost
}
```

✅ **Good**: Error handling with retry
```java
@RabbitListener(queues = "task.queue")
public void onTaskEvent(TaskEvent event) {
  try {
    notificationService.sendNotification(event);
  } catch (Exception e) {
    logger.error("Failed to send notification, requeue", e);
    throw e;  // Let RabbitMQ requeue
  }
}
```

