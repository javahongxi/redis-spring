package org.hongxi.redis.multi;

import io.lettuce.core.ReadFrom;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for multiple Redis clusters.
 *
 * @author javahongxi
 */
@ConfigurationProperties(prefix = "spring.data.redis")
public class MultiRedisProperties {

    /**
     * Whether to enable auto-register mode.
     * When enabled, RedisTemplate beans will be automatically registered
     * for each cluster defined in {@code clusters}.
     */
    private boolean autoRegister = false;

    // ========== Official format compatibility (top-level fields) ==========
    // These fields support the official Spring Boot Redis configuration format
    // for backward compatibility. When 'clusters' is empty, these fields are used
    // to create a 'default' cluster.

    /** Redis host (official format compatibility) */
    private String host = "localhost";
    /** Redis port (official format compatibility) */
    private int port = 6379;
    /** Redis username for ACL (official format compatibility) */
    private String username;
    /** Redis password (official format compatibility) */
    private String password;
    /** Redis database index (official format compatibility) */
    private int database = 0;
    /** Connection/command timeout (official format compatibility) */
    private Duration timeout;
    /** Connect timeout (official format compatibility) */
    private Duration connectTimeout;
    /** Redis URL format (official format compatibility) */
    private String url;
    /** Top-level cluster config for official cluster mode (official format compatibility) */
    private ClusterConfig cluster = new ClusterConfig();
    /** Top-level lettuce config (official format compatibility) */
    private Lettuce lettuce = new Lettuce();

    // ========== Multi-redis format ==========

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

    // Getters and setters for official format compatibility fields

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getDatabase() { return database; }
    public void setDatabase(int database) { this.database = database; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public ClusterConfig getCluster() { return cluster; }
    public void setCluster(ClusterConfig cluster) { this.cluster = cluster; }

    public Lettuce getLettuce() { return lettuce; }
    public void setLettuce(Lettuce lettuce) { this.lettuce = lettuce; }

    /**
     * Check if using official format (top-level fields) instead of multi-redis format.
     * Returns true if 'clusters' is empty but either 'cluster.nodes' or 'host' is configured.
     */
    public boolean isUsingOfficialFormat() {
        if (clusters != null && !clusters.isEmpty()) {
            return false;
        }
        // Check if cluster mode (official format)
        if (cluster != null && cluster.getNodes() != null && !cluster.getNodes().isEmpty()) {
            return true;
        }
        // Check if standalone mode (official format) - host is not default
        return !"localhost".equals(host) || port != 6379 || password != null || url != null;
    }

    /**
     * Create a default cluster from official format configuration.
     */
    public Cluster createDefaultClusterFromOfficialFormat() {
        Cluster defaultCluster = new Cluster();
        defaultCluster.setUrl(url);
        defaultCluster.setHost(host);
        defaultCluster.setPort(port);
        defaultCluster.setUsername(username);
        defaultCluster.setPassword(password);
        defaultCluster.setDatabase(database);
        defaultCluster.setTimeout(timeout);
        defaultCluster.setConnectTimeout(connectTimeout);

        // If cluster.nodes is set, use cluster mode
        if (cluster != null && cluster.getNodes() != null && !cluster.getNodes().isEmpty()) {
            ClusterConfig clusterConfig = new ClusterConfig();
            clusterConfig.setNodes(cluster.getNodes());
            clusterConfig.setMaxRedirects(cluster.getMaxRedirects());
            clusterConfig.setReadFrom(cluster.getReadFrom());
            defaultCluster.setCluster(clusterConfig);
        }

        // Copy lettuce config
        if (lettuce != null) {
            defaultCluster.setLettuce(lettuce);
        }

        return defaultCluster;
    }

    public static class Cluster {

        private String url;
        private String host = "localhost";
        private int port = 6379;
        private String username;
        private String password;
        private int database = 0;
        private Duration timeout;
        private Duration connectTimeout;
        private ClusterConfig cluster = new ClusterConfig();
        private Lettuce lettuce = new Lettuce();

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }

        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

        public ClusterConfig getCluster() { return cluster; }
        public void setCluster(ClusterConfig cluster) { this.cluster = cluster; }

        public Lettuce getLettuce() { return lettuce; }
        public void setLettuce(Lettuce lettuce) { this.lettuce = lettuce; }

        /**
         * Check if this is a Redis Cluster mode (nodes configured).
         */
        public boolean isClusterMode() {
            return cluster != null && cluster.getNodes() != null && !cluster.getNodes().isEmpty();
        }
    }

    /**
     * Redis Cluster configuration (spring.data.redis.clusters.{name}.cluster.*).
     */
    public static class ClusterConfig {

        private List<String> nodes;
        private Integer maxRedirects;
        private ReadFrom readFrom;

        public List<String> getNodes() { return nodes; }
        public void setNodes(List<String> nodes) { this.nodes = nodes; }

        public Integer getMaxRedirects() { return maxRedirects; }
        public void setMaxRedirects(Integer maxRedirects) { this.maxRedirects = maxRedirects; }

        public ReadFrom getReadFrom() { return readFrom; }
        public void setReadFrom(ReadFrom readFrom) { this.readFrom = readFrom; }
    }

    public static class Lettuce {

        private Pool pool;
        private LettuceCluster cluster;

        public Pool getPool() { return pool; }
        public void setPool(Pool pool) { this.pool = pool; }

        public LettuceCluster getCluster() { return cluster; }
        public void setCluster(LettuceCluster cluster) { this.cluster = cluster; }
    }

    /**
     * Lettuce cluster-specific configuration (spring.data.redis.clusters.{name}.lettuce.cluster.*).
     */
    public static class LettuceCluster {

        private Refresh refresh = new Refresh();

        public Refresh getRefresh() { return refresh; }
        public void setRefresh(Refresh refresh) { this.refresh = refresh; }
    }

    /**
     * Cluster topology refresh configuration.
     */
    public static class Refresh {

        private boolean adaptive = false;
        private Duration period;

        public boolean isAdaptive() { return adaptive; }
        public void setAdaptive(boolean adaptive) { this.adaptive = adaptive; }

        public Duration getPeriod() { return period; }
        public void setPeriod(Duration period) { this.period = period; }
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
