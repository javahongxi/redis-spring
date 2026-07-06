package org.hongxi.redis.multi.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Verifies multi-Redis connectivity on startup.
 *
 * @author javahongxi
 */
@Component
public class SampleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleRunner.class);

    private final StringRedisTemplate orderStringRedisTemplate;
    private final StringRedisTemplate userStringRedisTemplate;
    private final StringRedisTemplate cacheStringRedisTemplate;
    private final StringRedisTemplate sessionStringRedisTemplate;

    private final RedisTemplate<String, Object> cacheRedisTemplate;

    public SampleRunner(StringRedisTemplate orderStringRedisTemplate,
                        StringRedisTemplate userStringRedisTemplate,
                        StringRedisTemplate cacheStringRedisTemplate,
                        StringRedisTemplate sessionStringRedisTemplate,
                        RedisTemplate<String, Object> cacheRedisTemplate) {
        this.orderStringRedisTemplate = orderStringRedisTemplate;
        this.userStringRedisTemplate = userStringRedisTemplate;
        this.cacheStringRedisTemplate = cacheStringRedisTemplate;
        this.sessionStringRedisTemplate = sessionStringRedisTemplate;
        this.cacheRedisTemplate = cacheRedisTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("========== Multi-Redis Sample Verification ==========");

        // Verify order Redis standalone (port 6379)
        verifyRedis("order", orderStringRedisTemplate);

        // Verify user Redis standalone (port 6380)
        verifyRedis("user", userStringRedisTemplate);

        // Verify cache Redis cluster (port 7001-7003)
        verifyRedis("cache", cacheStringRedisTemplate);

        // Verify session Redis cluster (port 7011-7013)
        verifyRedis("session", sessionStringRedisTemplate);

        log.info("========== All verifications passed! ==========");

        // Verify RedisTemplate
        User user = new User("lily", 20, new Date());
        cacheRedisTemplate.opsForValue().set("user", user);
        log.info("user: {}", cacheRedisTemplate.opsForValue().get("user"));
        cacheRedisTemplate.delete("user");
    }

    private void verifyRedis(String name, StringRedisTemplate template) {
        try {
            LettuceConnectionFactory factory = (LettuceConnectionFactory) template.getConnectionFactory();

            // 1. Show connection factory configuration (expected target)
            if (factory.getClusterConfiguration() != null) {
                RedisClusterConfiguration clusterConfig = factory.getClusterConfiguration();
                log.info("[{}] Config -> CLUSTER nodes={}", name, clusterConfig.getClusterNodes());
            } else {
                RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
                log.info("[{}] Config -> {}:{}", name, config.getHostName(), config.getPort());
            }

            // 2. Query actual server info via INFO command (proves real connection)
            Properties serverInfo = factory.getConnection().serverCommands().info("server");
            if (serverInfo != null && serverInfo.getProperty("tcp_port") != null) {
                // Standalone mode: keys are plain (e.g. "tcp_port", "redis_version")
                log.info("[{}] Server -> tcp_port={}, redis_version={}", name,
                        serverInfo.getProperty("tcp_port"),
                        serverInfo.getProperty("redis_version"));
            } else if (serverInfo != null) {
                // Cluster mode: keys are node-prefixed (e.g. "127.0.0.1:7001.tcp_port")
                Map<String, String> nodePorts = new LinkedHashMap<>();
                String version = null;
                for (String key : serverInfo.stringPropertyNames()) {
                    if (key.endsWith(".tcp_port")) {
                        String node = key.substring(0, key.length() - ".tcp_port".length());
                        nodePorts.put(node, serverInfo.getProperty(key));
                    }
                    if (key.endsWith(".redis_version") && version == null) {
                        version = serverInfo.getProperty(key);
                    }
                }
                log.info("[{}] Server -> CLUSTER nodes={}, redis_version={}", name, nodePorts, version);
            }

            // 3. Verify read/write
            String key = "sample:test:" + name;
            String value = "hello-" + name + "-" + System.currentTimeMillis();

            template.opsForValue().set(key, value);
            Object result = template.opsForValue().get(key);
            template.delete(key);

            if (value.equals(result)) {
                log.info("[{}] Read/Write OK: set={}, get={}", name, value, result);
            } else {
                log.error("[{}] Read/Write MISMATCH: set={}, get={}", name, value, result);
            }
        } catch (Exception e) {
            log.error("[{}] Redis FAILED: {}", name, e.getMessage());
        }
    }
}
