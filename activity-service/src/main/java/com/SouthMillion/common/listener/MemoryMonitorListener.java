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

// Send memory usage report and optimization suggestions to channel
log.info(logDetailedMemoryUsage());
log.info(suggestOptimizations());
}

private String logDetailedMemoryUsage() {
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

long usedHeap    = heapUsage.getUsed()    / 1024 / 1024;
long maxHeap     = heapUsage.getMax()  > 0 ? heapUsage.getMax()    / 1024 / 1024 : -1;
long usedNonHeap = nonHeapUsage.getUsed() / 1024 / 1024;
long maxNonHeap  = nonHeapUsage.getMax() > 0 ? nonHeapUsage.getMax() / 1024 / 1024 : -1;

int threadCount = threadBean.getThreadCount();

StringBuilder report = new StringBuilder();
report.append("Memory Usage Report (After Startup)\n");
report.append("Heap Memory: ").append(usedHeap).append("/").append(maxHeap).append(" MB\n");
report.append("Non-Heap: ").append(usedNonHeap).append("/").append(maxNonHeap).append(" MB\n");
report.append("Threads: ").append(threadCount).append("\n");

// Detailed memory pools
List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
for (MemoryPoolMXBean pool : pools) {
if (pool.getName().contains("Metaspace") || pool.getName().contains("Eden")) {
MemoryUsage usage = pool.getUsage();
long used = usage.getUsed() / 1024 / 1024;
long max = usage.getMax() > 0 ? usage.getMax() / 1024 / 1024 : -1;
report.append(pool.getName()).append(": ").append(used).append(" MB")
.append(max > 0 ? " / " + max + " MB" : "").append("\n");
}
}

// Calculate total estimated process memory
long totalEstimated = usedHeap + usedNonHeap + (threadCount * 1); // ~1MB per thread estimate
report.append("Estimated Total: ~").append(totalEstimated).append(" MB\n");

return report.toString();
}

private String suggestOptimizations() {
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

long usedHeap    = heapUsage.getUsed()    / 1024 / 1024;
long maxHeap     = heapUsage.getMax()  > 0 ? heapUsage.getMax()    / 1024 / 1024 : -1;
long usedNonHeap = nonHeapUsage.getUsed() / 1024 / 1024;

StringBuilder suggestions = new StringBuilder();

// Warning if using > 80% of heap
if (maxHeap > 0 && usedHeap * 100 / maxHeap > 80) {
suggestions.append("HIGH HEAP USAGE (>80%)!\n");
suggestions.append("Consider increasing -Xmx or reducing workload\n");
}

// Warning if Metaspace is too high
if (usedNonHeap > 80) {
suggestions.append("HIGH METASPACE USAGE (").append(usedNonHeap).append(" MB)!\n");
suggestions.append("Consider: -XX:MaxMetaspaceSize=64m\n");
}

// Success message if within limits
if (usedHeap + usedNonHeap < 100) {
suggestions.append("Memory usage is healthy (< 100 MB total)\n");
} else if (usedHeap + usedNonHeap < 150) {
suggestions.append("Memory usage is acceptable (< 150 MB total)\n");
}

return suggestions.toString();
}
}