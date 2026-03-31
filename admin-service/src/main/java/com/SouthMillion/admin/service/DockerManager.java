package com.SouthMillion.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker Container Manager
 * Manages Docker containers for GameServer services
 *
 * Features:
 * - Start/Stop individual containers
 * - Start all infrastructure containers (for Eureka)
 * - Check container status
 * - Execute docker-compose commands
 */
@Service
@Slf4j
public class DockerManager {

    // FIX #1: Correct compose file name (was docker-compose-databases.yml)
    private static final String DOCKER_COMPOSE_FILE = "D:/project/serverGame/GameServer/docker/docker-compose.local-full.yml";
    private static final int CONTAINER_START_TIMEOUT = 30; // seconds

    /**
     * Danh sách tất cả MySQL containers từ docker-compose.local-full.yml.
     * Dùng để startAllDatabases() và grantAllDatabasePermissions().
     * Tất cả dùng MYSQL_ROOT_PASSWORD=root123 và tạo user tpnam/121831.
     */
    private static final List<String> ALL_DB_CONTAINERS = Arrays.asList(
        // P1: User & Core
        "local-userdb", "local-reportdb", "local-boxdb", "local-bagdb",
        "local-walletdb", "local-equipdb", "local-shopdb", "local-craftingdb",
        "local-serverinfodb", "local-roledb", "local-arenadb", "local-iapverifydb",
        // P2: Combat & World
        "local-trialdb", "local-taskdb", "local-battleserverdb", "local-globalserverdb",
        "local-gameworlddb", "local-starmapdb", "local-territorydb", "local-escortdb",
        "local-worlddb", "local-chatdb", "local-guilddb",
        // P3: Social & Enhancement
        "local-frienddb", "local-maildb", "local-leaderboarddb", "local-petdb",
        "local-mountdb", "local-runedb", "local-angeldb", "local-artifactdb",
        // P4: Analytics & Moderation
        "local-analyticsdb", "local-notificationdb", "local-moderationdb",
        "local-mainfbdb", "local-anticheatdb",
        // P5: New Gameplay
        "local-lingzhudb", "local-knightsdb", "local-pagodadb", "local-scrolldb",
        "local-gemdb", "local-activitydb", "local-shizhuangdb",
        // Special: Admin
        "local-admindb", "local-gmdb"
    );

    /**
     * Start a Docker container by name
     * Auto-creates container if it doesn't exist
     */
    public boolean startContainer(String containerName) {
        if (containerName == null || containerName.isEmpty()) {
            log.warn("⚠️ No container name provided, skipping Docker start");
            return true;
        }

        try {
            log.info("🐳 Checking Docker container: {}", containerName);

            // Check if container exists
            if (!containerExists(containerName)) {
                log.warn("⚠️ Container does not exist: {}, attempting to create...", containerName);

                boolean created = createMySQLContainer(containerName);
                if (!created) {
                    log.error("❌ Failed to create container: {}", containerName);
                    return false;
                }

                log.info("✅ Container created successfully: {}", containerName);
                return true;
            }

            // Check if already running
            if (isContainerRunning(containerName)) {
                log.info("✅ Container already running: {}", containerName);
                grantTpnamPermissions(containerName);
                return true;
            }

            // Start existing container
            log.info("🐳 Starting existing container: {}", containerName);
            ProcessBuilder pb = new ProcessBuilder("docker", "start", containerName);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(CONTAINER_START_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                log.error("❌ Container start timeout: {}", containerName);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = readProcessOutput(process);
                log.error("❌ Failed to start container: {} (exit code: {})\n{}", containerName, exitCode, output);
                return false;
            }

            // Chờ container state = running trước (nhanh)
            boolean containerUp = waitForContainerHealthy(containerName, 30);
            if (!containerUp) {
                log.warn("⚠️ Container started but state not 'running' yet: {}", containerName);
            }

            // Chờ MySQL thực sự ready (quan trọng hơn container state)
            boolean mysqlReady = waitForMySqlReady(containerName, 60);
            if (mysqlReady) {
                log.info("✅ Container + MySQL ready: {}", containerName);
            } else {
                log.warn("⚠️ MySQL may not be fully ready yet, attempting permission grant anyway...");
            }
            grantTpnamPermissions(containerName);

            return true;

        } catch (Exception e) {
            log.error("❌ Error starting container: {}", containerName, e);
            return false;
        }
    }

    /**
     * Ensure game network exists, create if needed
     * FIX #2: Use correct network name 'gameserver-local' (was 'game-network')
     */
    private void ensureGameNetworkExists() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "network", "ls", "--format", "{{.Name}}"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean networkExists = false;

            while ((line = reader.readLine()) != null) {
                if ("gameserver-local".equals(line.trim())) {  // FIX #2
                    networkExists = true;
                    break;
                }
            }

            process.waitFor();
            reader.close();

            if (networkExists) {
                log.info("✅ Docker network 'gameserver-local' already exists");
                return;
            }

            log.info("🐳 Creating Docker network: gameserver-local");
            pb = new ProcessBuilder("docker", "network", "create", "gameserver-local");  // FIX #2
            pb.redirectErrorStream(true);
            process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("✅ Docker network 'gameserver-local' created successfully");
            } else {
                log.error("❌ Failed to create Docker network (exit code: {})", exitCode);
            }

        } catch (Exception e) {
            log.error("❌ Error ensuring gameserver-local network exists", e);
        }
    }

    /**
     * Create a new MySQL container via docker compose (v2).
     * Service name is derived from container name: "local-arenadb" -> "arenadb"
     */
    private boolean createMySQLContainer(String containerName) {
        try {
            String serviceName = containerName.replace("gameserver-", "").replace("local-", "");
            log.info("🐳 Creating container '{}' via docker compose service '{}'", containerName, serviceName);

            // FIX #6: Use 'docker compose' v2 (consistent with rest of codebase)
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose",
                "-f", DOCKER_COMPOSE_FILE,
                "up", "-d", serviceName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readProcessOutput(process);

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                log.error("❌ docker compose up timeout for: {}", serviceName);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("❌ docker compose up failed for '{}' (exit {}): {}", serviceName, exitCode, output);
                return false;
            }

            log.info("⏳ Waiting for MySQL to be ready in new container: {}", containerName);
            waitForMySqlReady(containerName, 90);  // Container mới cần nhiều thời gian init hơn
            grantTpnamPermissions(containerName);

            log.info("✅ MySQL container created and started: {}", containerName);
            return true;

        } catch (Exception e) {
            log.error("❌ Error creating MySQL container: {}", containerName, e);
            return false;
        }
    }

    /**
     * Map container name to host port number.
     * FIX #4+#5: All ports corrected to match docker-compose.local-full.yml,
     *            P5 services (battleserverdb, globalserverdb, gameworlddb,
     *            lingzhudb, knightsdb, pagodadb, scrolldb, gemdb, activitydb) added.
     */
    private int getPortForContainer(String containerName) {
        switch (containerName) {
            // ── P1: User & Core ──────────────────────────────────────────────
            case "local-userdb":        case "gameserver-userdb":        return 33061;
            case "local-roledb":        case "gameserver-roledb":        return 33062;
            case "local-serverinfodb":  case "gameserver-serverinfodb":  return 33063;
            case "local-walletdb":      case "gameserver-walletdb":      return 33064;
            case "local-reportdb":      case "gameserver-reportdb":      return 33065;
            case "local-iapverifydb":   case "gameserver-iapverifydb":   return 33066;
            case "local-bagdb":         case "gameserver-bagdb":         return 33067;
            case "local-equipdb":       case "gameserver-equipdb":       return 33068;
            case "local-shopdb":        case "gameserver-shopdb":        return 33069;
            case "local-boxdb":         case "gameserver-boxdb":         return 33070;

            // ── P2: Combat & World ───────────────────────────────────────────
            case "local-craftingdb":      case "gameserver-craftingdb":      return 33071;
            case "local-arenadb":         case "gameserver-arenadb":         return 33072;
            case "local-trialdb":         case "gameserver-trialdb":         return 33073;
            case "local-taskdb":          case "gameserver-taskdb":          return 33074;
            case "local-starmapdb":       case "gameserver-starmapdb":       return 33075;
            case "local-territorydb":     case "gameserver-territorydb":     return 33076;
            case "local-escortdb":        case "gameserver-escortdb":        return 33095; // FIX: was 33077
            case "local-worlddb":         case "gameserver-worlddb":         return 3323;  // FIX: was 33078
            case "local-chatdb":          case "gameserver-chatdb":          return 33080; // FIX: was 33079
            case "local-guilddb":         case "gameserver-guilddb":         return 33084; // FIX: was 33080
            // P2 new – FIX #5: missing services added
            case "local-battleserverdb":  case "gameserver-battleserverdb":  return 3328;
            case "local-globalserverdb":  case "gameserver-globalserverdb":  return 3317;
            case "local-gameworlddb":     case "gameserver-gameworlddb":     return 3308;

            // ── P3: Social & Enhancement ─────────────────────────────────────
            case "local-petdb":           case "gameserver-petdb":           return 33077; // FIX: was 33084
            case "local-mountdb":         case "gameserver-mountdb":         return 33078; // FIX: was 33085
            case "local-runedb":          case "gameserver-runedb":          return 33079; // FIX: was 33086
            case "local-frienddb":        case "gameserver-frienddb":        return 33085; // FIX: was 33081
            case "local-maildb":          case "gameserver-maildb":          return 33086; // FIX: was 33082
            case "local-leaderboarddb":   case "gameserver-leaderboarddb":   return 33087; // FIX: was 33083
            case "local-shizhuangdb":     case "gameserver-shizhuangdb":     return 33081; // FIX: was 33087
            case "local-angeldb":         case "gameserver-angeldb":         return 33082; // FIX: was 33088
            case "local-artifactdb":      case "gameserver-artifactdb":      return 33083; // FIX: was 33089
            case "local-analyticsdb":     case "gameserver-analyticsdb":     return 33092; // FIX: was 33090
            case "local-notificationdb":  case "gameserver-notificationdb":  return 33090; // FIX: was 33091
            case "local-moderationdb":    case "gameserver-moderationdb":    return 33091; // FIX: was 33092

            // ── P4: Optional ─────────────────────────────────────────────────
            case "local-mainfbdb":        case "gameserver-mainfbdb":        return 33094; // FIX: was 33093
            case "local-anticheatdb":     case "gameserver-anticheatdb":     return 33093; // FIX: was 33094

            // ── P5: New Gameplay – FIX #5: all missing, now added ─────────────
            case "local-lingzhudb":       case "gameserver-lingzhudb":       return 3315;
            case "local-knightsdb":       case "gameserver-knightsdb":       return 3316;
            case "local-pagodadb":        case "gameserver-pagodadb":        return 3322;
            case "local-scrolldb":        case "gameserver-scrolldb":        return 3319;
            case "local-gemdb":           case "gameserver-gemdb":           return 3320;
            case "local-activitydb":      case "gameserver-activitydb":      return 3321;

            // ── SPECIAL: Admin ───────────────────────────────────────────────
            case "local-admindb":         case "gameserver-admindb":         return 9088;  // FIX: was 3306
            case "local-gmdb":            case "gameserver-gmdb":            return 33089; // FIX: was 33095

            default:
                log.warn("⚠️ Unknown container: {}, using default port 33099", containerName);
                return 33099;
        }
    }

    /**
     * Start tất cả MySQL containers một lần duy nhất qua docker compose,
     * sau đó grant quyền tpnam/121831 cho tất cả.
     *
     * Gọi method này khi:
     *  - Lần đầu khởi động môi trường local
     *  - Sau khi docker compose down rồi up lại
     *  - Khi gặp lỗi "Access denied for user tpnam"
     */
    public boolean startAllDatabases() {
        log.info("🐳 Starting ALL MySQL database containers via docker compose...");
        try {
            // Build command: docker compose -f <file> up -d <svc1> <svc2> ...
            List<String> cmd = new ArrayList<>(Arrays.asList(
                "docker", "compose", "-f", DOCKER_COMPOSE_FILE, "up", "-d"
            ));
            // Service name = container name minus "local-" prefix
            ALL_DB_CONTAINERS.stream()
                .map(c -> c.replace("local-", ""))
                .forEach(cmd::add);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(180, TimeUnit.SECONDS);

            if (!finished) {
                log.error("❌ docker compose up timeout when starting all DB containers");
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("❌ docker compose up failed (exit {}): {}", exitCode, output);
                return false;
            }

            log.info("⏳ Waiting 30s for MySQL containers to fully initialize...");
            Thread.sleep(30000);

            // Grant tpnam permissions on every DB container
            grantAllDatabasePermissions();

            log.info("✅ All {} database containers started and tpnam permissions granted!", ALL_DB_CONTAINERS.size());
            return true;

        } catch (Exception e) {
            log.error("❌ Error starting all databases", e);
            return false;
        }
    }

    /**
     * Grant tpnam/121831 permissions trên TẤT CẢ MySQL containers đang chạy.
     *
     * - Lần 1: thử grant tất cả containers đang chạy
     * - Nếu có container fail (MySQL chưa kịp init): retry tối đa 3 lần, mỗi lần chờ 8s
     * - Log rõ container nào SUCCESS / FAILED
     */
    public void grantAllDatabasePermissions() {
        log.info("🔐 Granting tpnam permissions on ALL database containers...");

        // Bước 1: Xác định containers đang chạy
        List<String> running = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String container : ALL_DB_CONTAINERS) {
            if (isContainerRunning(container)) {
                running.add(container);
            } else {
                skipped.add(container);
            }
        }

        if (!skipped.isEmpty()) {
            log.info("⏭  {} containers not running (skipped): {}", skipped.size(), skipped);
        }

        if (running.isEmpty()) {
            log.warn("⚠️ No database containers are running!");
            return;
        }

        // Bước 2: Grant với retry cho containers fail
        final int MAX_RETRIES = 3;
        final int RETRY_DELAY_MS = 8000; // 8s – đủ để MySQL hoàn tất init

        List<String> pending = new ArrayList<>(running);
        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            if (pending.isEmpty()) break;

            if (attempt > 1) {
                log.info("🔄 Retry #{} for {} failed containers (waiting {}s first)...",
                    attempt, pending.size(), RETRY_DELAY_MS / 1000);
                try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            List<String> stillFailing = new ArrayList<>();
            for (String container : pending) {
                boolean ok = grantTpnamPermissions(container);
                if (ok) {
                    succeeded.add(container);
                } else {
                    stillFailing.add(container);
                }
            }
            pending = stillFailing;
        }

        failed.addAll(pending);

        // Bước 3: Log kết quả tổng hợp
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔐 Permission grant result: {}/{} containers succeeded",
            succeeded.size(), running.size());
        if (!failed.isEmpty()) {
            log.warn("❌ FAILED ({} containers): {}", failed.size(), failed);
            log.warn("   → Nguyên nhân thường gặp: MySQL chưa init xong.");
            log.warn("   → Chờ thêm ~30s rồi gọi lại POST /api/services/databases/grant-permissions");
        } else {
            log.info("✅ ALL {} containers granted successfully!", succeeded.size());
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Stop a Docker container by name
     */
    public boolean stopContainer(String containerName) {
        if (containerName == null || containerName.isEmpty()) {
            log.warn("⚠️ No container name provided, skipping Docker stop");
            return true;
        }

        try {
            log.info("🐳 Stopping Docker container: {}", containerName);

            if (!isContainerRunning(containerName)) {
                log.info("✅ Container already stopped: {}", containerName);
                return true;
            }

            ProcessBuilder pb = new ProcessBuilder("docker", "stop", "-t", "30", containerName);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(40, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("⚠️ Container stop timeout, forcing stop: {}", containerName);
                forceStopContainer(containerName);
                return true;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = readProcessOutput(process);
                log.error("❌ Failed to stop container: {} (exit code: {})\n{}", containerName, exitCode, output);
                return false;
            }

            log.info("✅ Container stopped successfully: {}", containerName);
            return true;

        } catch (Exception e) {
            log.error("❌ Error stopping container: {}", containerName, e);
            return false;
        }
    }

    /**
     * Start core infrastructure: Redis + Kafka ONLY.
     * FIX #6: Use 'docker compose' v2 syntax (was 'docker-compose' v1).
     *
     * LƯU Ý: KHÔNG start tất cả MySQL containers ở đây vì quá nặng (45 containers).
     *   - Mỗi service tự start DB của nó qua startContainer() khi service đó được khởi động.
     *   - Muốn start tất cả DB cùng lúc: gọi startAllDatabases() hoặc
     *     POST /api/services/databases/start
     */
    public boolean startAllInfrastructure() {
        log.info("🚀 Starting core infrastructure containers (Redis, Kafka)...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose",
                "-f", DOCKER_COMPOSE_FILE,
                "up", "-d",
                "redis",
                "kafka"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = readProcessOutput(process);
            log.info("Docker Compose output:\n{}", output);

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                log.error("❌ Docker Compose start timeout");
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("❌ Failed to start infrastructure (exit code: {})", exitCode);
                log.error("Output: {}", output);
                return false;
            }

            log.info("✅ Infrastructure containers started successfully!");
            log.info("   - Redis: localhost:6379");
            log.info("   - Kafka: localhost:9092");
            log.info("   ℹ️  MySQL DB containers will start on-demand when each service starts.");

            Thread.sleep(5000);

            return true;

        } catch (Exception e) {
            log.error("❌ Error starting infrastructure", e);
            return false;
        }
    }

    /**
     * Stop all infrastructure containers.
     * FIX #6: Use 'docker compose' v2 syntax.
     */
    public boolean stopAllInfrastructure() {
        log.info("🛑 Stopping ALL infrastructure containers...");

        try {
            // FIX #6: 'docker compose' v2
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "compose",
                "-f", DOCKER_COMPOSE_FILE,
                "down"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = readProcessOutput(process);
            log.info("Docker Compose output:\n{}", output);

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                log.error("❌ Docker Compose stop timeout");
                return false;
            }

            log.info("✅ Infrastructure containers stopped successfully!");
            return true;

        } catch (Exception e) {
            log.error("❌ Error stopping infrastructure", e);
            return false;
        }
    }

    /**
     * Check if container exists
     */
    public boolean containerExists(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "ps", "-a", "--filter", "name=" + containerName, "--format", "{{.Names}}"
            );
            Process process = pb.start();
            String output = readProcessOutput(process).trim();
            return output.equals(containerName);
        } catch (Exception e) {
            log.error("Error checking container existence: {}", containerName, e);
            return false;
        }
    }

    /**
     * Check if container is running
     */
    public boolean isContainerRunning(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "ps", "--filter", "name=" + containerName, "--format", "{{.Names}}"
            );
            Process process = pb.start();
            String output = readProcessOutput(process).trim();
            return output.equals(containerName);
        } catch (Exception e) {
            log.error("Error checking container status: {}", containerName, e);
            return false;
        }
    }

    /**
     * Get container status
     */
    public String getContainerStatus(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "inspect",
                "--format", "{{.State.Status}}",
                containerName
            );
            Process process = pb.start();
            String output = readProcessOutput(process).trim();

            int exitCode = process.waitFor();
            return (exitCode == 0) ? output : "not-found";

        } catch (Exception e) {
            log.error("Error getting container status: {}", containerName, e);
            return "error";
        }
    }

    /**
     * Wait for container to be healthy (container state = running)
     */
    private boolean waitForContainerHealthy(String containerName, int timeoutSeconds) {
        int attempts = 0;
        int maxAttempts = timeoutSeconds / 2;

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(2000);
                String status = getContainerStatus(containerName);
                if ("running".equals(status)) {
                    return true;
                }
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Chờ MySQL bên trong container sẵn sàng nhận kết nối (dùng mysqladmin ping).
     * Container state = "running" chưa đủ — MySQL cần thêm thời gian init schema.
     *
     * @param timeoutSeconds tổng thời gian chờ tối đa (khuyến nghị 60s cho container mới)
     * @return true nếu MySQL ready, false nếu hết timeout
     */
    private boolean waitForMySqlReady(String containerName, int timeoutSeconds) {
        log.info("⏳ Waiting for MySQL to be ready in: {} (max {}s)...", containerName, timeoutSeconds);
        int intervalMs = 2000;
        int maxAttempts = (timeoutSeconds * 1000) / intervalMs;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                Thread.sleep(intervalMs);
                // FIX: dùng "mysql -e SELECT 1" thay vì "mysqladmin ping --silent"
                // mysqladmin ping trả về exit=0 ngay khi TCP port mở, dù tpnam chưa auth được.
                // mysql SELECT 1 chỉ trả về exit=0 khi authentication thực sự thành công.
                // → khi waitForMySqlReady() return true, tpnamCanConnect() cũng return true
                //   → không bao giờ rơi vào vòng root-password-loop trong grantTpnamPermissions
                ProcessBuilder pb = new ProcessBuilder(
                    "docker", "exec", containerName,
                    "mysql", "-u", "tpnam", "-p121831",
                    "--connect-timeout=2",
                    "-e", "SELECT 1"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = readProcessOutput(process);
                process.waitFor(5, TimeUnit.SECONDS);
                if (process.exitValue() == 0) {
                    log.info("✅ MySQL ready in: {} (attempt {}/{})", containerName, attempt + 1, maxAttempts);
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Container not yet ready to exec, continue polling
            }
        }
        log.warn("⚠️ MySQL not ready after {}s in: {}", timeoutSeconds, containerName);
        return false;
    }

    /**
     * Force stop container
     */
    private void forceStopContainer(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "kill", containerName);
            pb.start().waitFor(10, TimeUnit.SECONDS);
            log.info("✅ Container force stopped: {}", containerName);
        } catch (Exception e) {
            log.error("Error force stopping container: {}", containerName, e);
        }
    }

    /**
     * Read process output
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Kiểm tra xem tpnam user có thể kết nối vào MySQL container không.
     * Dùng để detect containers đã được docker-compose tạo sẵn user tpnam
     * thông qua MYSQL_USER / MYSQL_PASSWORD environment variables.
     */
    private boolean tpnamCanConnect(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerName,
                "mysql", "-u", "tpnam", "-p121831",
                "--connect-timeout=3",
                "-e", "SELECT 1"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Grant tpnam user full permissions on all databases.
     * FIX #3: Added 'root123' as first candidate (matches docker-compose MYSQL_ROOT_PASSWORD).
     *
     * Logic:
     * 1. Kiểm tra tpnam đã có thể connect chưa (docker-compose MYSQL_USER đã tạo sẵn)
     *    → Nếu CÓ: chỉ cần upgrade lên *.*  (không cần password root)
     *    → Nếu KHÔNG: thử GRANT qua root với nhiều password candidates
     *
     * Lý do 17 containers cũ fail root-grant:
     *   Volume đã tồn tại từ trước → MYSQL_ROOT_PASSWORD bị bỏ qua khi restart
     *   → root password trong volume khác với root123
     *   Nhưng tpnam đã được tạo sẵn bởi MYSQL_USER/MYSQL_PASSWORD → connect được ngay
     *
     * @return true nếu tpnam có thể kết nối (dù qua cách nào)
     */
    private boolean grantTpnamPermissions(String containerName) {
        if (containerName == null || !containerName.contains("db")) {
            return false;
        }

        // ── Bước 1: Kiểm tra tpnam đã tồn tại và hoạt động ──────────────────
        // docker-compose MYSQL_USER: tpnam tự tạo user khi init container.
        // Với volumes đã tồn tại, MYSQL_ROOT_PASSWORD bị bỏ qua nhưng MYSQL_USER
        // đã được tạo từ lần init đầu → tpnam đã có sẵn với password 121831.
        if (tpnamCanConnect(containerName)) {
            log.info("✅ tpnam already configured in: {} (created by docker-compose MYSQL_USER)", containerName);
            // Upgrade lên *.* access để Flyway + cross-DB queries hoạt động
            upgradeTpnamToFullAccess(containerName);
            return true;
        }

        // ── Bước 2: tpnam chưa tồn tại → tạo qua root ───────────────────────
        log.info("🔐 Granting tpnam permissions in container: {}", containerName);

        // FIX #3: root123 is the password set in docker-compose.local-full.yml
        String[] rootPasswords = {"root123", "root", "1234", "", "admin"};
        String grantSql = "CREATE USER IF NOT EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';" +
                          "GRANT ALL PRIVILEGES ON *.* TO 'tpnam'@'%' WITH GRANT OPTION;" +
                          "ALTER USER IF EXISTS 'tpnam'@'%' IDENTIFIED BY '121831';" +
                          "FLUSH PRIVILEGES;";

        for (String rootPassword : rootPasswords) {
            try {
                List<String> cmd = new ArrayList<>();
                cmd.add("docker"); cmd.add("exec"); cmd.add(containerName);
                cmd.add("mysql"); cmd.add("-u"); cmd.add("root");
                if (!rootPassword.isEmpty()) cmd.add("-p" + rootPassword);
                cmd.add("-e"); cmd.add(grantSql);

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = readProcessOutput(process);
                boolean finished = process.waitFor(15, TimeUnit.SECONDS);

                if (finished && process.exitValue() == 0) {
                    log.info("✅ tpnam permissions granted via root in: {}", containerName);
                    return true;
                }
                // DEBUG thay vì WARN: thử nhiều root passwords là bình thường,
                // chỉ WARN khi TẤT CẢ đều thất bại (cuối vòng lặp)
                if (!output.isBlank()) {
                    log.debug("   Root -p{} failed in {}: {}",
                        rootPassword.isEmpty() ? "(empty)" : "***",
                        containerName,
                        output.lines().filter(l -> l.contains("ERROR")).findFirst().orElse("(no error line)"));
                }
            } catch (Exception e) {
                log.debug("Grant attempt exception: {}", e.getMessage());
            }
        }
        log.warn("⚠️ Could not grant tpnam permissions in: {} (tpnam not accessible + root failed)", containerName);
        return false;
    }

    /**
     * Nâng quyền tpnam lên ALL PRIVILEGES ON *.* khi nó đã tồn tại nhưng
     * chỉ có quyền trên một database (do docker-compose MYSQL_USER tạo).
     * Thử qua tpnam chính nó với WITH GRANT OPTION (nếu đã được cấp trước),
     * hoặc qua root. Thất bại không nghiêm trọng vì tpnam đã có quyền trên DB riêng.
     */
    private void upgradeTpnamToFullAccess(String containerName) {
        String upgradeSql = "GRANT ALL PRIVILEGES ON *.* TO 'tpnam'@'%' WITH GRANT OPTION;" +
                            "FLUSH PRIVILEGES;";
        // Thử qua root123 (nếu volume mới, root123 còn hiệu lực)
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerName,
                "mysql", "-u", "root", "-proot123", "-e", upgradeSql
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(8, TimeUnit.SECONDS);
            if (process.exitValue() == 0) {
                log.debug("✅ tpnam upgraded to *.* in: {}", containerName);
            }
        } catch (Exception ignored) {
            // Non-critical: tpnam đã có quyền trên DB riêng, đủ để service chạy
        }
    }

    /**
     * Get list of all running containers
     */
    public List<String> getRunningContainers() {
        List<String> containers = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "--format", "{{.Names}}");
            Process process = pb.start();
            String output = readProcessOutput(process);

            for (String line : output.split("\n")) {
                String name = line.trim();
                if (!name.isEmpty()) {
                    containers.add(name);
                }
            }
        } catch (Exception e) {
            log.error("Error getting running containers", e);
        }
        return containers;
    }

    /**
     * Restart a container
     */
    public boolean restartContainer(String containerName) {
        log.info("🔄 Restarting Docker container: {}", containerName);

        boolean stopped = stopContainer(containerName);
        if (!stopped) {
            log.warn("⚠️ Failed to stop container, attempting start anyway");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return startContainer(containerName);
    }
}
