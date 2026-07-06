package org.hongxi.redis.multi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration properties for multiple Redis clusters.
 *
 * @author javahongxi
 */
@ConfigurationProperties(prefix = "spring.redis")
public class MultiRedisProperties {

    /**
     * Whether to enable auto-register mode.
     * When enabled, RedisTemplate beans will be automatically registered
     * for each cluster defined in {@code clusters}.
     */
    private boolean autoRegister = false;

    private Map<String, Cluster> clusters = new LinkedHashMap<>();

    public boolean isAutoRegister() {
        return autoRegister;
    }

    public void setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
    }

    public Map<String, Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, Cluster> clusters) {
        this.clusters = clusters;
    }

    public static class Cluster {

        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private Duration timeout;
        private Lettuce lettuce = new Lettuce();

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public Lettuce getLettuce() { return lettuce; }
        public void setLettuce(Lettuce lettuce) { this.lettuce = lettuce; }
    }

    public static class Lettuce {

        private Pool pool;

        public Pool getPool() { return pool; }
        public void setPool(Pool pool) { this.pool = pool; }
    }

    public static class Pool {

        private int maxActive = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private Duration maxWait;

        public int getMaxActive() { return maxActive; }
        public void setMaxActive(int maxActive) { this.maxActive = maxActive; }

        public int getMaxIdle() { return maxIdle; }
        public void setMaxIdle(int maxIdle) { this.maxIdle = maxIdle; }

        public int getMinIdle() { return minIdle; }
        public void setMinIdle(int minIdle) { this.minIdle = minIdle; }

        public Duration getMaxWait() { return maxWait; }
        public void setMaxWait(Duration maxWait) { this.maxWait = maxWait; }
    }
}
