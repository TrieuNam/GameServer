package com.SouthMillion.common.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * Monitors and logs memory usage after application startup
 * Provides optimization suggestions if memory usage is too high
 */
@Component
public class MemoryMonitorListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(MemoryMonitorListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Wait a bit for services to fully initialize
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logDetailedMemoryUsage();
        suggestOptimizations();
    }

    private void logDetailedMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        long usedHeap    = heapUsage.getUsed()    / 1024 / 1024;
        long maxHeap     = heapUsage.getMax()  > 0 ? heapUsage.getMax()    / 1024 / 1024 : -1;
        long usedNonHeap = nonHeapUsage.getUsed() / 1024 / 1024;
        long maxNonHeap  = nonHeapUsage.getMax() > 0 ? nonHeapUsage.getMax() / 1024 / 1024 : -1;
        
        int threadCount = threadBean.getThreadCount();
        
        log.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        log.info("ðŸ“Š MEMORY USAGE REPORT (After Startup)");
        log.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        log.info("ðŸ’¾ Heap Memory:     {} / {} MB ({}%)",
                 usedHeap, maxHeap,
                 maxHeap > 0 ? (usedHeap * 100 / maxHeap) : -1);
        log.info("ðŸ’¾ Non-Heap:        {} / {} MB ({}%)",
                 usedNonHeap,
                 maxNonHeap > 0 ? maxNonHeap : "N/A",
                 maxNonHeap > 0 ? (usedNonHeap * 100 / maxNonHeap) : -1);
        log.info("ðŸ§µ Threads:         {}", threadCount);
        
        // Detailed memory pools
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getName().contains("Metaspace") || pool.getName().contains("Eden")) {
                MemoryUsage usage = pool.getUsage();
                long used = usage.getUsed() / 1024 / 1024;
                long max = usage.getMax() > 0 ? usage.getMax() / 1024 / 1024 : -1;
                log.info("   {}: {} MB{}", 
                         pool.getName(), used, 
                         max > 0 ? " / " + max + " MB" : "");
            }
        }
        
        // Calculate total estimated process memory
        long totalEstimated = usedHeap + usedNonHeap + (threadCount * 1); // ~1MB per thread estimate
        log.info("ðŸ“¦ Estimated Total: ~{} MB", totalEstimated);
        log.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
    }

    private void suggestOptimizations() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        long usedHeap    = heapUsage.getUsed()    / 1024 / 1024;
        long maxHeap     = heapUsage.getMax()  > 0 ? heapUsage.getMax()    / 1024 / 1024 : -1;
        long usedNonHeap = nonHeapUsage.getUsed() / 1024 / 1024;
        
        // Warning if using > 80% of heap
        if (maxHeap > 0 && usedHeap * 100 / maxHeap > 80) {
            log.warn("âš ï¸  HIGH HEAP USAGE (>80%)!");
            log.warn("   Consider increasing -Xmx or reducing workload");
        }
        
        // Warning if Metaspace is too high
        if (usedNonHeap > 80) {
            log.warn("âš ï¸  HIGH METASPACE USAGE ({} MB)!", usedNonHeap);
            log.warn("   Consider: -XX:MaxMetaspaceSize=64m");
        }
        
        // Success message if within limits
        if (usedHeap + usedNonHeap < 100) {
            log.info("âœ… Memory usage is healthy (< 100 MB total)");
        } else if (usedHeap + usedNonHeap < 150) {
            log.info("âœ… Memory usage is acceptable (< 150 MB total)");
        }
    }
}

