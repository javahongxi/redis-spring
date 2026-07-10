package org.hongxi.redis.multi.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application for multi-redis-spring-boot-starter.
 * <p>
 * Switch scenarios by changing {@code spring.profiles.active} in {@code application.yml}:
 * <ul>
 *   <li>{@code official} &mdash; Official format (single cluster)</li>
 *   <li>{@code clusters} &mdash; Multi-cluster format (default)</li>
 *   <li>{@code mixed} &mdash; Official + clusters</li>
 * </ul>
 * Switch mode via {@code spring.data.redis.auto-register}: {@code true} (auto-register) or {@code false} (Builder mode).
 * <p>
 * <b>Prerequisites:</b> Start 3 standalone (6379/6380/6381) + 2 cluster (cache:7001-7006, session:7011-7016) before running.
 *
 * @author javahongxi
 */
@SpringBootApplication
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
