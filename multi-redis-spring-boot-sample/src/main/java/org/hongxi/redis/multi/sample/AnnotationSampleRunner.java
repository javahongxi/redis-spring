package org.hongxi.redis.multi.sample;

import org.hongxi.redis.multi.annotation.RedisCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Demonstrates the usage of {@code @RedisCluster} annotation for automatic
 * RedisTemplate injection.
 * <p>
 * <b>Note:</b> This runner is only active when {@code spring.data.redis.auto-register=false},
 * which is Mode 1 (Builder + Annotation).
 * <p>
 * The {@code @RedisCluster} annotation injects RedisTemplate instances directly into fields
 * without the need to define beans manually.
 *
 * @author javahongxi
 * @see org.hongxi.redis.multi.annotation.RedisCluster
 */
@Order(3)
@Component
@ConditionalOnProperty(name = "spring.data.redis.auto-register", havingValue = "false", matchIfMissing = true)
public class AnnotationSampleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnnotationSampleRunner.class);

    /**
     * Inject RedisTemplate for the "order" cluster using @RedisCluster annotation.
     * The template is automatically created by RedisClusterBeanPostProcessor.
     */
    @RedisCluster("order")
    private RedisTemplate<String, Object> orderRedisTemplate;

    /**
     * Inject StringRedisTemplate for the "user" cluster using @RedisCluster annotation.
     */
    @RedisCluster("user")
    private StringRedisTemplate userStringRedisTemplate;

    /**
     * Inject RedisTemplate for the "cache" cluster (Redis Cluster mode).
     */
    @RedisCluster("cache")
    private RedisTemplate<String, Object> cacheRedisTemplate;

    /**
     * Inject StringRedisTemplate for the "session" cluster (Redis Cluster mode).
     */
    @RedisCluster("session")
    private StringRedisTemplate sessionStringRedisTemplate;

    @Override
    public void run(String... args) {
        demonstrateAnnotationMode();
    }

    /**
     * Demonstrates usage of @RedisCluster injected templates.
     */
    private void demonstrateAnnotationMode() {
        log.info("========== @RedisCluster Annotation Mode Demo ==========");

        // Use orderRedisTemplate (RedisTemplate with User object)
        User orderUser = new User("order-test", 25, new Date());
        orderRedisTemplate.opsForValue().set("annotation:order:test", orderUser);
        Object orderValue = orderRedisTemplate.opsForValue().get("annotation:order:test");
        log.info("[order] @RedisCluster demo: {}", orderValue);
        orderRedisTemplate.delete("annotation:order:test");

        // Use userStringRedisTemplate (StringRedisTemplate with simple string)
        userStringRedisTemplate.opsForValue().set("annotation:user:test", "Hello from @RedisCluster!");
        String userValue = userStringRedisTemplate.opsForValue().get("annotation:user:test");
        log.info("[user] @RedisCluster demo: {}", userValue);
        userStringRedisTemplate.delete("annotation:user:test");

        // Use cacheRedisTemplate (RedisTemplate with User object)
        User cacheUser = new User("cache-test", 30, new Date());
        cacheRedisTemplate.opsForValue().set("annotation:cache:test", cacheUser);
        Object cacheValue = cacheRedisTemplate.opsForValue().get("annotation:cache:test");
        log.info("[cache] @RedisCluster demo: {}", cacheValue);
        cacheRedisTemplate.delete("annotation:cache:test");

        // Use sessionStringRedisTemplate (StringRedisTemplate with simple string)
        sessionStringRedisTemplate.opsForValue().set("annotation:session:test", "Session cluster demo!");
        String sessionValue = sessionStringRedisTemplate.opsForValue().get("annotation:session:test");
        log.info("[session] @RedisCluster demo: {}", sessionValue);
        sessionStringRedisTemplate.delete("annotation:session:test");

        log.info("========== @RedisCluster Annotation Mode Demo Complete ==========");
    }
}
