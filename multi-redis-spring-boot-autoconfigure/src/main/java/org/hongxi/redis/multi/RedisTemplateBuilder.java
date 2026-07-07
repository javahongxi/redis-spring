package org.hongxi.redis.multi;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builder for creating RedisTemplate instances bound to different Redis clusters.
 * <p>Inspired by Spring AI's ChatClient.Builder pattern.</p>
 *
 * @author javahongxi
 */
public class RedisTemplateBuilder {

    private final MultiRedisProperties properties;
    private final Map<String, LettuceConnectionFactory> connectionFactoryCache = new ConcurrentHashMap<>();

    public RedisTemplateBuilder(MultiRedisProperties properties) {
        this.properties = properties;
    }

    /**
     * Select a cluster by name and return a {@link RedisTemplateConfigurer} for building
     * a configured {@link RedisTemplate}.
     * <p>
     * Example usage:
     * <pre>
     * // With default serializers (java serialization, same as official)
     * RedisTemplate&lt;String, Object&gt; template = builder.cluster("order").build();
     *
     * // With custom serializers
     * RedisTemplate&lt;String, Object&gt; template = builder.cluster("order")
     *     .keySerializer(RedisSerializer.string())
     *     .valueSerializer(RedisSerializer.json())
     *     .build();
     * </pre>
     *
     * @param clusterName the cluster name defined in spring.data.redis.clusters.{name}
     * @return a RedisTemplateConfigurer for building the RedisTemplate
     */
    public RedisTemplateConfigurer<String, Object> cluster(String clusterName) {
        LettuceConnectionFactory factory = getConnectionFactory(clusterName);
        MultiRedisProperties.Cluster cluster = resolveCluster(clusterName);
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // Set serializers from configuration (default: java serialization, same as official)
        applySerializers(template, cluster);
        return new RedisTemplateConfigurer<>(template);
    }

    private MultiRedisProperties.Cluster resolveCluster(String clusterName) {
        MultiRedisProperties.Cluster cluster = properties.getClusters().get(clusterName);
        if (cluster == null && "default".equals(clusterName) && properties.isUsingOfficialFormat()) {
            cluster = properties.createDefaultClusterFromOfficialFormat();
        }
        return cluster;
    }

    private void applySerializers(RedisTemplate<String, Object> template, MultiRedisProperties.Cluster cluster) {
        MultiRedisProperties.Serializer serializerConfig = cluster.getSerializer();
        template.setKeySerializer(toRedisSerializer(serializerConfig.getKey()));
        template.setValueSerializer(toRedisSerializer(serializerConfig.getValue()));
        template.setHashKeySerializer(toRedisSerializer(serializerConfig.getHashKey()));
        template.setHashValueSerializer(toRedisSerializer(serializerConfig.getHashValue()));
    }

    private RedisSerializer<?> toRedisSerializer(MultiRedisProperties.SerializerType type) {
        if (type == null) {
            return RedisSerializer.java();
        }
        return switch (type) {
            case java -> RedisSerializer.java();
            case json -> RedisSerializer.json();
            case string -> RedisSerializer.string();
            case byteArray -> RedisSerializer.byteArray();
        };
    }

    /**
     * Select a cluster by name and return a configured {@link StringRedisTemplate}.
     *
     * @param clusterName the cluster name defined in spring.data.redis.clusters.{name}
     * @return a new StringRedisTemplate connected to the specified cluster
     */
    public StringRedisTemplate stringTemplate(String clusterName) {
        LettuceConnectionFactory factory = getConnectionFactory(clusterName);
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

    private LettuceConnectionFactory getConnectionFactory(String clusterName) {
        MultiRedisProperties.Cluster cluster = properties.getClusters().get(clusterName);
        
        // If cluster not found, check if using official format and clusterName is "default"
        if (cluster == null && "default".equals(clusterName) && properties.isUsingOfficialFormat()) {
            cluster = properties.createDefaultClusterFromOfficialFormat();
        }
        
        if (cluster == null) {
            throw new IllegalArgumentException(
                    "Redis cluster '" + clusterName + "' not found. Available: "
                            + properties.getClusters().keySet());
        }
        final MultiRedisProperties.Cluster finalCluster = cluster;
        return connectionFactoryCache.computeIfAbsent(clusterName, name -> createConnectionFactory(finalCluster));
    }

    private LettuceConnectionFactory createConnectionFactory(MultiRedisProperties.Cluster cluster) {
        LettuceConnectionFactory factory;

        // URL mode: parse RedisURI and extract connection details
        if (cluster.getUrl() != null && !cluster.getUrl().isEmpty()) {
            RedisURI redisURI = RedisURI.create(cluster.getUrl());
            if (cluster.isClusterMode()) {
                // URL + cluster mode: use nodes from cluster config, auth from URL
                RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(cluster.getCluster().getNodes());
                if (cluster.getCluster().getMaxRedirects() != null) {
                    clusterConfig.setMaxRedirects(cluster.getCluster().getMaxRedirects());
                }
                applyAuthFromURI(clusterConfig, redisURI, cluster);
                factory = buildClusterConnectionFactory(clusterConfig, cluster);
            } else {
                // URL + standalone mode: extract all from URL
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
                config.setHostName(redisURI.getHost());
                config.setPort(redisURI.getPort());
                config.setDatabase(redisURI.getDatabase());
                applyAuthFromURI(config, redisURI, cluster);
                factory = buildStandaloneConnectionFactory(config, cluster);
            }
        } else if (cluster.isClusterMode()) {
            // Redis Cluster mode
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(cluster.getCluster().getNodes());
            if (cluster.getCluster().getMaxRedirects() != null) {
                clusterConfig.setMaxRedirects(cluster.getCluster().getMaxRedirects());
            }
            applyAuth(clusterConfig, cluster);
            factory = buildClusterConnectionFactory(clusterConfig, cluster);
        } else {
            // Standalone mode
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(cluster.getHost());
            config.setPort(cluster.getPort());
            config.setDatabase(cluster.getDatabase());
            applyAuth(config, cluster);
            factory = buildStandaloneConnectionFactory(config, cluster);
        }
        factory.afterPropertiesSet();
        return factory;
    }

    private void applyAuth(RedisClusterConfiguration clusterConfig, MultiRedisProperties.Cluster cluster) {
        if (cluster.getUsername() != null && !cluster.getUsername().isEmpty()) {
            clusterConfig.setUsername(cluster.getUsername());
        }
        if (cluster.getPassword() != null && !cluster.getPassword().isEmpty()) {
            clusterConfig.setPassword(cluster.getPassword());
        }
    }

    private void applyAuth(RedisStandaloneConfiguration config, MultiRedisProperties.Cluster cluster) {
        if (cluster.getUsername() != null && !cluster.getUsername().isEmpty()) {
            config.setUsername(cluster.getUsername());
        }
        if (cluster.getPassword() != null && !cluster.getPassword().isEmpty()) {
            config.setPassword(cluster.getPassword());
        }
    }

    private void applyAuthFromURI(RedisClusterConfiguration clusterConfig, RedisURI redisURI, MultiRedisProperties.Cluster cluster) {
        // URL credentials take precedence over explicit config only if explicit config is not set
        if (cluster.getUsername() != null) {
            clusterConfig.setUsername(cluster.getUsername());
        } else if (redisURI.getUsername() != null && !redisURI.getUsername().isEmpty()) {
            clusterConfig.setUsername(redisURI.getUsername());
        }
        if (cluster.getPassword() != null && !cluster.getPassword().isEmpty()) {
            clusterConfig.setPassword(cluster.getPassword());
        } else if (redisURI.getPassword() != null && redisURI.getPassword().length > 0) {
            clusterConfig.setPassword(new String(redisURI.getPassword()));
        }
    }

    private void applyAuthFromURI(RedisStandaloneConfiguration config, RedisURI redisURI, MultiRedisProperties.Cluster cluster) {
        if (cluster.getUsername() != null) {
            config.setUsername(cluster.getUsername());
        } else if (redisURI.getUsername() != null && !redisURI.getUsername().isEmpty()) {
            config.setUsername(redisURI.getUsername());
        }
        if (cluster.getPassword() != null && !cluster.getPassword().isEmpty()) {
            config.setPassword(cluster.getPassword());
        } else if (redisURI.getPassword() != null && redisURI.getPassword().length > 0) {
            config.setPassword(new String(redisURI.getPassword()));
        }
    }

    private LettuceConnectionFactory buildClusterConnectionFactory(
            RedisClusterConfiguration clusterConfig, MultiRedisProperties.Cluster cluster) {
        ClusterTopologyRefreshOptions topologyRefreshOptions = buildClusterTopologyRefreshOptions(cluster);
        MultiRedisProperties.Pool pool = resolvePoolConfig(cluster);
        ReadFrom readFrom = cluster.getCluster().getReadFrom();

        if (pool != null) {
            GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = buildPoolConfig(pool);
            LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                    LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
            applyClientTimeout(builder, cluster);
            if (topologyRefreshOptions != null) {
                builder.clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
            }
            if (readFrom != null) {
                builder.readFrom(readFrom);
            }
            return new LettuceConnectionFactory(clusterConfig, builder.build());
        } else {
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                    LettuceClientConfiguration.builder();
            applyClientTimeout(clientBuilder, cluster);
            if (topologyRefreshOptions != null) {
                clientBuilder.clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
            }
            if (readFrom != null) {
                clientBuilder.readFrom(readFrom);
            }
            return new LettuceConnectionFactory(clusterConfig, clientBuilder.build());
        }
    }

    private LettuceConnectionFactory buildStandaloneConnectionFactory(
            RedisStandaloneConfiguration config, MultiRedisProperties.Cluster cluster) {
        MultiRedisProperties.Pool pool = resolvePoolConfig(cluster);
        if (pool != null) {
            GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = buildPoolConfig(pool);
            LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                    LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
            applyClientTimeout(builder, cluster);
            return new LettuceConnectionFactory(config, builder.build());
        } else {
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                    LettuceClientConfiguration.builder();
            applyClientTimeout(clientBuilder, cluster);
            return new LettuceConnectionFactory(config, clientBuilder.build());
        }
    }

    private void applyClientTimeout(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder, MultiRedisProperties.Cluster cluster) {
        if (cluster.getTimeout() != null) {
            builder.commandTimeout(cluster.getTimeout());
        }
    }

    private GenericObjectPoolConfig<StatefulConnection<?, ?>> buildPoolConfig(MultiRedisProperties.Pool pool) {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        if (pool.getMaxWait() != null) {
            poolConfig.setMaxWait(pool.getMaxWait());
        }
        return poolConfig;
    }

    private ClusterTopologyRefreshOptions buildClusterTopologyRefreshOptions(MultiRedisProperties.Cluster cluster) {
        MultiRedisProperties.Lettuce lettuce = cluster.getLettuce();
        if (lettuce == null || lettuce.getCluster() == null) {
            return null;
        }
        MultiRedisProperties.Refresh refresh = lettuce.getCluster().getRefresh();
        if (refresh == null || (!refresh.isAdaptive() && refresh.getPeriod() == null)) {
            return null;
        }
        ClusterTopologyRefreshOptions.Builder builder = ClusterTopologyRefreshOptions.builder();
        if (refresh.isAdaptive()) {
            builder.enableAllAdaptiveRefreshTriggers();
        }
        if (refresh.getPeriod() != null) {
            builder.enablePeriodicRefresh(refresh.getPeriod());
        }
        return builder.build();
    }

    private MultiRedisProperties.Pool resolvePoolConfig(MultiRedisProperties.Cluster cluster) {
        if (cluster.getLettuce() != null && cluster.getLettuce().getPool() != null) {
            return cluster.getLettuce().getPool();
        }
        return null;
    }
}
