package com.bank.core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configures Java 21 Virtual Threads as the default executor for Spring MVC's
 * request handling thread pool and the application task executor.
 *
 * <p>Virtual Threads (Project Loom, stable in Java 21) eliminate the need to
 * size a fixed platform-thread pool. Each incoming HTTP request or scheduled
 * task is dispatched to a lightweight virtual thread, enabling high concurrency
 * for I/O-bound workloads (Postgres queries, Redis commands, RabbitMQ calls)
 * without the overhead of reactive programming.</p>
 *
 * <p>To activate Virtual Threads for Tomcat's request handling, the property
 * {@code spring.threads.virtual.enabled=true} must be set in
 * {@code application.yml}. This bean supplements that by providing a
 * virtual-thread-backed {@link Executor} for {@code @Async} and
 * {@code @Scheduled} tasks.</p>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class VirtualThreadsConfig {

    /**
     * Exposes a virtual-thread-per-task {@link Executor} as a Spring bean,
     * which Spring Boot auto-configures as the default task executor for
     * {@code @Async} and {@code @Scheduled} methods when present.
     *
     * @return an {@link Executor} backed by a new virtual thread for each task
     */
    @Bean(name = "applicationTaskExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
