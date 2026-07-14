package org.hongxi.redis.multi.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Verifies multi-Redis connectivity on startup (connection and server info only).
 *
 * @author javahongxi
 */
@Component
public class ConnectionSampleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConnectionSampleRunner.class);

    private final StringRedisTemplate defaultStringRedisTemplate;
    private final StringRedisTemplate orderStringRedisTemplate;
    private final StringRedisTemplate userStringRedisTemplate;
    private final StringRedisTemplate cacheStringRedisTemplate;
    private final StringRedisTemplate sessionStringRedisTemplate;

    public ConnectionSampleRunner(StringRedisTemplate defaultStringRedisTemplate,
                                  StringRedisTemplate orderStringRedisTemplate,
                                  StringRedisTemplate userStringRedisTemplate,
                                  StringRedisTemplate cacheStringRedisTemplate,
                                  StringRedisTemplate sessionStringRedisTemplate) {
        this.defaultStringRedisTemplate = defaultStringRedisTemplate;
        this.orderStringRedisTemplate = orderStringRedisTemplate;
        this.userStringRedisTemplate = userStringRedisTemplate;
        this.cacheStringRedisTemplate = cacheStringRedisTemplate;
        this.sessionStringRedisTemplate = sessionStringRedisTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("");
        log.info("========== Multi-Redis Connection Verification ==========");

        verifyConnection("default", defaultStringRedisTemplate);
        verifyConnection("order", orderStringRedisTemplate);
        verifyConnection("user", userStringRedisTemplate);
        verifyConnection("cache", cacheStringRedisTemplate);
        verifyConnection("session", sessionStringRedisTemplate);

        log.info("========== Connection verification complete ==========");
    }

    private void verifyConnection(String name, StringRedisTemplate template) {
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
            try (RedisConnection connection = factory.getConnection()) {
                Properties serverInfo = connection.serverCommands().info("server");
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
            }
        } catch (Exception e) {
            log.error("[{}] Connection verification FAILED: {}", name, e.getMessage());
        }
    }
}
