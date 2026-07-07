package org.hongxi.redis.multi.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Verifies multi-Redis read/write operations on startup.
 *
 * @author javahongxi
 */
@Order(2)
@Component
public class ReadWriteSampleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReadWriteSampleRunner.class);

    private final RedisTemplate<String, Object> orderRedisTemplate;
    private final StringRedisTemplate userStringRedisTemplate;
    private final RedisTemplate<String, Object> cacheRedisTemplate;
    private final StringRedisTemplate sessionStringRedisTemplate;

    public ReadWriteSampleRunner(RedisTemplate<String, Object> orderRedisTemplate,
                                 StringRedisTemplate userStringRedisTemplate,
                                 RedisTemplate<String, Object> cacheRedisTemplate,
                                 StringRedisTemplate sessionStringRedisTemplate) {
        this.orderRedisTemplate = orderRedisTemplate;
        this.userStringRedisTemplate = userStringRedisTemplate;
        this.cacheRedisTemplate = cacheRedisTemplate;
        this.sessionStringRedisTemplate = sessionStringRedisTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("========== Multi-Redis Read/Write Verification ==========");

        // Verify order Redis (RedisTemplate with User object)
        verifyObjectReadWrite("order", orderRedisTemplate);

        // Verify user Redis (StringRedisTemplate with simple string)
        verifyStringReadWrite("user", userStringRedisTemplate);

        // Verify cache Redis (RedisTemplate with User object)
        verifyObjectReadWrite("cache", cacheRedisTemplate);

        // Verify session Redis (StringRedisTemplate with simple string)
        verifyStringReadWrite("session", sessionStringRedisTemplate);

        log.info("========== All read/write verifications passed! ==========");
    }

    private void verifyObjectReadWrite(String name, RedisTemplate<String, Object> template) {
        try {
            String key = "sample:test:" + name;
            User value = new User(name + "-user", 20, new Date());

            template.opsForValue().set(key, value);
            Object result = template.opsForValue().get(key);
            template.delete(key);

            log.info("[{}] Read/Write OK: set={}, get={}", name, value, result);
        } catch (Exception e) {
            log.error("[{}] Read/Write FAILED: {}", name, e.getMessage());
        }
    }

    private void verifyStringReadWrite(String name, StringRedisTemplate template) {
        try {
            String key = "sample:test:" + name;
            String value = "hello-" + name + "-" + System.currentTimeMillis();

            template.opsForValue().set(key, value);
            String result = template.opsForValue().get(key);
            template.delete(key);

            if (value.equals(result)) {
                log.info("[{}] Read/Write OK: set={}, get={}", name, value, result);
            } else {
                log.error("[{}] Read/Write MISMATCH: set={}, get={}", name, value, result);
            }
        } catch (Exception e) {
            log.error("[{}] Read/Write FAILED: {}", name, e.getMessage());
        }
    }
}
