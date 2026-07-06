package org.hongxi.redis.multi;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
     * Select a cluster by name and return a configured RedisTemplate.
     *
     * @param clusterName the cluster name defined in spring.redis.clusters.{name}
     * @return a new RedisTemplate connected to the specified cluster
     */
    public RedisTemplate<String, Object> cluster(String clusterName) {
        MultiRedisProperties.Cluster cluster = properties.getClusters().get(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException(
                    "Redis cluster '" + clusterName + "' not found. Available: "
                            + properties.getClusters().keySet());
        }
        return build(clusterName, cluster);
    }

    private RedisTemplate<String, Object> build(String clusterName, MultiRedisProperties.Cluster cluster) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(cluster.getHost());
        config.setPort(cluster.getPort());
        config.setDatabase(cluster.getDatabase());
        if (cluster.getPassword() != null && !cluster.getPassword().isEmpty()) {
            config.setPassword(cluster.getPassword());
        }

        LettuceConnectionFactory factory = createConnectionFactory(clusterName, cluster, config);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    private LettuceConnectionFactory createConnectionFactory(String clusterName,
                                                             MultiRedisProperties.Cluster cluster,
                                                             RedisStandaloneConfiguration config) {
        return connectionFactoryCache.computeIfAbsent(clusterName, name -> {
            LettuceConnectionFactory factory;
            MultiRedisProperties.Pool pool = resolvePoolConfig(cluster);
            if (pool != null) {
                GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
                poolConfig.setMaxTotal(pool.getMaxActive());
                poolConfig.setMaxIdle(pool.getMaxIdle());
                poolConfig.setMinIdle(pool.getMinIdle());
                if (pool.getMaxWait() != null) {
                    poolConfig.setMaxWait(pool.getMaxWait());
                }
                LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                        LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
                if (cluster.getTimeout() != null) {
                    builder.commandTimeout(cluster.getTimeout());
                }
                factory = new LettuceConnectionFactory(config, builder.build());
            } else {
                factory = new LettuceConnectionFactory(config);
                if (cluster.getTimeout() != null) {
                    factory.setTimeout(cluster.getTimeout().toMillis());
                }
            }
            factory.afterPropertiesSet();
            return factory;
        });
    }

    private MultiRedisProperties.Pool resolvePoolConfig(MultiRedisProperties.Cluster cluster) {
        if (cluster.getLettuce() != null && cluster.getLettuce().getPool() != null) {
            return cluster.getLettuce().getPool();
        }
        return null;
    }
}
