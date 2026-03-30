package com.SouthMillion.common.optimization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Suggests JVM arguments for optimal memory usage
 * Detects current settings and provides recommendations
 */
@Component
public class JvmArgumentsSuggester {

    private static final Logger log = LoggerFactory.getLogger(JvmArgumentsSuggester.class);

    /**
     * Fix: dùng @Value thay vì System.getProperty().
     * System.getProperty() không đọc application.yml → luôn trả về "unknown"
     * → tier luôn là MINIMAL dù service là eureka/gateway/config.
     * @EventListener chạy sau khi Spring ready nên @Value đã được inject đúng.
     */
    @Value("${spring.application.name:unknown}")
    private String appName;

    @EventListener(ApplicationReadyEvent.class)
    public void suggestArguments() {
        ServiceTier tier = detectTier(appName);
        
        log.info("════════════════════════════════════════════════");
        log.info("💡 RECOMMENDED JVM ARGUMENTS");
        log.info("════════════════════════════════════════════════");
        log.info("🎯 Service: {}", appName);
        log.info("📊 Tier: {}", tier);
        log.info("");
        log.info("📝 Add these JVM arguments to your Dockerfile or start script:");
        log.info("");
        
        switch (tier) {
            case CRITICAL:
                log.info("   -Xms128m -Xmx256m \\");
                log.info("   -XX:MetaspaceSize=64m \\");
                log.info("   -XX:MaxMetaspaceSize=128m \\");
                log.info("   -XX:+UseSerialGC \\");
                log.info("   -XX:TieredStopAtLevel=1 \\");
                log.info("   -Xss256k");
                log.info("");
                log.info("   Expected RAM: 150-250 MB");
                break;
                
            case ULTRA_LOW:
                log.info("   -Xms32m -Xmx64m \\");
                log.info("   -XX:MetaspaceSize=16m \\");
                log.info("   -XX:MaxMetaspaceSize=48m \\");
                log.info("   -XX:+UseSerialGC \\");
                log.info("   -XX:TieredStopAtLevel=1 \\");
                log.info("   -Xss128k \\");
                log.info("   -XX:MaxGCPauseMillis=1000 \\");
                log.info("   -XX:GCTimeRatio=4");
                log.info("");
                log.info("   Expected RAM: 50-80 MB");
                break;
                
            default: // MINIMAL
                log.info("   -Xms48m -Xmx96m \\");
                log.info("   -XX:MetaspaceSize=24m \\");
                log.info("   -XX:MaxMetaspaceSize=64m \\");
                log.info("   -XX:+UseSerialGC \\");
                log.info("   -XX:TieredStopAtLevel=1 \\");
                log.info("   -Xss128k \\");
                log.info("   -XX:+UseStringDeduplication");
                log.info("");
                log.info("   Expected RAM: 70-120 MB");
        }
        
        log.info("");
        log.info("🐳 Dockerfile example:");
        log.info("   ENV JAVA_OPTS=\"<arguments above>\"");
        log.info("   ENTRYPOINT java $JAVA_OPTS -jar app.jar");
        log.info("");
        log.info("☸️  Kubernetes example:");
        log.info("   resources:");
        log.info("     limits:");
        log.info("       memory: \"{}\"", tier.getK8sMemoryLimit());
        log.info("     requests:");
        log.info("       memory: \"{}\"", tier.getK8sMemoryRequest());
        log.info("════════════════════════════════════════════════");
    }

    private ServiceTier detectTier(String appName) {
        if (appName.contains("eureka") || appName.contains("gateway") || appName.contains("config")) {
            return ServiceTier.CRITICAL;
        }
        if (appName.contains("analytics") || appName.contains("scheduler") || 
            appName.contains("file") || appName.contains("localization") || 
            appName.contains("moderation")) {
            return ServiceTier.ULTRA_LOW;
        }
        return ServiceTier.MINIMAL;
    }

    private enum ServiceTier {
        CRITICAL("256Mi", "128Mi"),
        MINIMAL("96Mi", "48Mi"),
        ULTRA_LOW("64Mi", "32Mi");
        
        private final String k8sMemoryLimit;
        private final String k8sMemoryRequest;
        
        ServiceTier(String limit, String request) {
            this.k8sMemoryLimit = limit;
            this.k8sMemoryRequest = request;
        }
        
        public String getK8sMemoryLimit() { return k8sMemoryLimit; }
        public String getK8sMemoryRequest() { return k8sMemoryRequest; }
    }
}
