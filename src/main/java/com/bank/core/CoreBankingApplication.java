package com.bank.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Core Banking API application.
 *
 * <p>This application implements a monolithic, hexagonal-architecture-based banking system
 * capable of managing accounts, processing internal fund transfers, and routing
 * asynchronous side-effect messages via RabbitMQ. It leverages Java 21 Virtual Threads
 * for high-throughput I/O concurrency.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 *   <li>Account creation and balance inquiries</li>
 *   <li>Idempotent fund transfers (via Redis)</li>
 *   <li>Transactional Outbox pattern for reliable RabbitMQ message delivery</li>
 *   <li>Prometheus metrics and OpenAPI documentation</li>
 * </ul>
 *
 * @author Core Banking Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class CoreBankingApplication {

    /**
     * Application main entry point.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(String[] args) {
        SpringApplication.run(CoreBankingApplication.class, args);
    }
}
