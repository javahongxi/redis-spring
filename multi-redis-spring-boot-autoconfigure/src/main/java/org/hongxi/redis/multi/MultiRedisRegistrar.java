package org.hongxi.redis.multi;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Automatically registers {@link RedisTemplate} beans for each cluster
 * defined in {@code spring.data.redis.clusters}.
 * <p>
 * Enabled when {@code spring.data.redis.auto-register=true}.
 * <p>
 * For each cluster entry, a {@link LettuceConnectionFactory},
 * a {@link RedisTemplate} and a {@link StringRedisTemplate} bean
 * are registered with the naming convention:
 * <ul>
 *   <li>{@code {clusterName}RedisConnectionFactory}</li>
 *   <li>{@code {clusterName}RedisTemplate}</li>
 *   <li>{@code {clusterName}StringRedisTemplate}</li>
 * </ul>
 *
 * @author javahongxi
 * @see MultiRedisAutoConfiguration
 */
public class MultiRedisRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(MultiRedisRegistrar.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        String autoRegister = environment.getProperty("spring.data.redis.auto-register", "false");
        if (!Boolean.parseBoolean(autoRegister)) {
            return;
        }

        Map<String, ClusterConfig> clusters = resolveClusters();
        if (clusters.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ClusterConfig> entry : clusters.entrySet()) {
            String clusterName = entry.getKey();
            ClusterConfig config = entry.getValue();

            // Register LettuceConnectionFactory bean
            String factoryBeanName = clusterName + "RedisConnectionFactory";
            GenericBeanDefinition factoryDef = new GenericBeanDefinition();
            factoryDef.setBeanClass(LettuceConnectionFactory.class);
            factoryDef.setInstanceSupplier(() -> createConnectionFactory(config));
            registry.registerBeanDefinition(factoryBeanName, factoryDef);

            // Register RedisTemplate bean
            String templateBeanName = clusterName + "RedisTemplate";
            GenericBeanDefinition templateDef = new GenericBeanDefinition();
            templateDef.setBeanClass(RedisTemplateFactoryBean.class);
            templateDef.getConstructorArgumentValues().addGenericArgumentValue(factoryBeanName);
            registry.registerBeanDefinition(templateBeanName, templateDef);

            // Register StringRedisTemplate bean
            String stringTemplateBeanName = clusterName + "StringRedisTemplate";
            GenericBeanDefinition stringTemplateDef = new GenericBeanDefinition();
            stringTemplateDef.setBeanClass(StringRedisTemplateFactoryBean.class);
            stringTemplateDef.getConstructorArgumentValues().addGenericArgumentValue(factoryBeanName);
            registry.registerBeanDefinition(stringTemplateBeanName, stringTemplateDef);
        }
    }

    private Map<String, ClusterConfig> resolveClusters() {
        Map<String, ClusterConfig> clusters = new LinkedHashMap<>();
        if (!(environment instanceof ConfigurableEnvironment ce)) {
            return clusters;
        }
        Set<String> clusterNames = new LinkedHashSet<>();
        // Collect cluster.nodes entries from property names directly
        // Use TreeMap to sort by property name (ensures correct index order: [0], [1], [2]...)
        Map<String, List<String>> clusterNodesMap = new LinkedHashMap<>();

        for (PropertySource<?> ps : ce.getPropertySources()) {
            if (ps instanceof EnumerablePropertySource<?> eps) {
                TreeMap<String, String> sortedProps = new TreeMap<>();
                for (String name : eps.getPropertyNames()) {
                    if (name.startsWith("spring.data.redis.clusters.")) {
                        String rest = name.substring("spring.data.redis.clusters.".length());
                        int dot = rest.indexOf('.');
                        if (dot > 0) {
                            String clusterName = rest.substring(0, dot);
                            clusterNames.add(clusterName);
                            // Collect nodes: spring.data.redis.clusters.{name}.cluster.nodes[0], [1], etc.
                            String nodesPrefix = "spring.data.redis.clusters." + clusterName + ".cluster.nodes";
                            if (name.equals(nodesPrefix) || name.startsWith(nodesPrefix + "[")) {
                                sortedProps.put(name, String.valueOf(eps.getProperty(name)));
                            }
                        }
                    }
                }
                // Build ordered nodes list per cluster from sorted properties
                for (Map.Entry<String, String> entry : sortedProps.entrySet()) {
                    String propName = entry.getKey();
                    String cName = propName.substring("spring.data.redis.clusters.".length());
                    int dot = cName.indexOf('.');
                    if (dot > 0) {
                        cName = cName.substring(0, dot);
                    }
                    clusterNodesMap.computeIfAbsent(cName, k -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }
        }

        for (String clusterName : clusterNames) {
            String prefix = "spring.data.redis.clusters." + clusterName + ".";
            ClusterConfig cc = new ClusterConfig();

            // Check if cluster mode (nodes configured)
            List<String> nodes = clusterNodesMap.get(clusterName);
            log.debug("[multi-redis] Cluster '{}': cluster.nodes={}", clusterName, nodes);
            if (nodes != null && !nodes.isEmpty()) {
                // Cluster mode
                cc.clusterMode = true;
                cc.clusterNodes = nodes;
                log.info("[multi-redis] Cluster '{}' -> CLUSTER mode, nodes={}", clusterName, nodes);
                String maxRedirects = environment.getProperty(prefix + "cluster.max-redirects");
                if (maxRedirects != null) {
                    cc.clusterMaxRedirects = Integer.parseInt(maxRedirects);
                }
                // Lettuce cluster refresh config
                String refreshAdaptive = environment.getProperty(prefix + "lettuce.cluster.refresh.adaptive");
                if (refreshAdaptive != null) {
                    cc.clusterAdaptive = Boolean.parseBoolean(refreshAdaptive);
                }
                String refreshPeriod = environment.getProperty(prefix + "lettuce.cluster.refresh.period");
                if (refreshPeriod != null) {
                    cc.clusterRefreshPeriod = parseDuration(refreshPeriod);
                }
            } else {
                // Standalone mode
                cc.host = environment.getProperty(prefix + "host", "localhost");
                cc.port = Integer.parseInt(environment.getProperty(prefix + "port", "6379"));
                cc.database = Integer.parseInt(environment.getProperty(prefix + "database", "0"));
                log.info("[multi-redis] Cluster '{}' -> STANDALONE mode, host={}, port={}", clusterName, cc.host, cc.port);
            }

            // Common config
            cc.password = environment.getProperty(prefix + "password");
            String timeoutStr = environment.getProperty(prefix + "timeout");
            if (timeoutStr != null) {
                cc.timeout = parseDuration(timeoutStr);
            }

            // Pool config
            String poolPrefix = prefix + "lettuce.pool.";
            String maxActive = environment.getProperty(poolPrefix + "max-active");
            if (maxActive != null) {
                cc.poolEnabled = true;
                cc.poolMaxActive = Integer.parseInt(maxActive);
                cc.poolMaxIdle = Integer.parseInt(environment.getProperty(poolPrefix + "max-idle", "8"));
                cc.poolMinIdle = Integer.parseInt(environment.getProperty(poolPrefix + "min-idle", "0"));
                String maxWait = environment.getProperty(poolPrefix + "max-wait");
                if (maxWait != null) {
                    cc.poolMaxWait = parseDuration(maxWait);
                }
            }
            clusters.put(clusterName, cc);
        }
        return clusters;
    }

    private Duration parseDuration(String value) {
        if (value.startsWith("PT") || value.startsWith("pt")) {
            return Duration.parse(value);
        }
        if (value.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
        }
        if (value.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        return Duration.ofSeconds(Long.parseLong(value));
    }

    static LettuceConnectionFactory createConnectionFactory(ClusterConfig config) {
        LettuceConnectionFactory factory;
        if (config.clusterMode) {
            // Redis Cluster mode
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(config.clusterNodes);
            if (config.clusterMaxRedirects != null) {
                clusterConfig.setMaxRedirects(config.clusterMaxRedirects);
            }
            if (config.password != null && !config.password.isEmpty()) {
                clusterConfig.setPassword(config.password);
            }
            factory = createLettuceConnectionFactory(clusterConfig, config);
        } else {
            // Standalone mode
            RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
            redisConfig.setHostName(config.host);
            redisConfig.setPort(config.port);
            redisConfig.setDatabase(config.database);
            if (config.password != null && !config.password.isEmpty()) {
                redisConfig.setPassword(config.password);
            }
            factory = createLettuceConnectionFactory(redisConfig, config);
        }
        factory.afterPropertiesSet();
        return factory;
    }

    private static LettuceConnectionFactory createLettuceConnectionFactory(
            RedisClusterConfiguration clusterConfig, ClusterConfig config) {
        // Build cluster topology refresh options
        ClusterTopologyRefreshOptions topologyRefreshOptions = buildClusterTopologyRefreshOptions(config);

        if (config.poolEnabled) {
            GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = buildPoolConfig(config);
            LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                    LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
            if (config.timeout != null) {
                builder.commandTimeout(config.timeout);
            }
            if (topologyRefreshOptions != null) {
                builder.clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
            }
            return new LettuceConnectionFactory(clusterConfig, builder.build());
        } else {
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                    LettuceClientConfiguration.builder();
            if (config.timeout != null) {
                clientBuilder.commandTimeout(config.timeout);
            }
            if (topologyRefreshOptions != null) {
                clientBuilder.clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(topologyRefreshOptions).build());
            }
            return new LettuceConnectionFactory(clusterConfig, clientBuilder.build());
        }
    }

    private static LettuceConnectionFactory createLettuceConnectionFactory(
            RedisStandaloneConfiguration redisConfig, ClusterConfig config) {
        if (config.poolEnabled) {
            GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = buildPoolConfig(config);
            LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                    LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
            if (config.timeout != null) {
                builder.commandTimeout(config.timeout);
            }
            return new LettuceConnectionFactory(redisConfig, builder.build());
        } else {
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                    LettuceClientConfiguration.builder();
            if (config.timeout != null) {
                clientBuilder.commandTimeout(config.timeout);
            }
            return new LettuceConnectionFactory(redisConfig, clientBuilder.build());
        }
    }

    private static GenericObjectPoolConfig<StatefulConnection<?, ?>> buildPoolConfig(ClusterConfig config) {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(config.poolMaxActive);
        poolConfig.setMaxIdle(config.poolMaxIdle);
        poolConfig.setMinIdle(config.poolMinIdle);
        if (config.poolMaxWait != null) {
            poolConfig.setMaxWait(config.poolMaxWait);
        }
        return poolConfig;
    }

    private static ClusterTopologyRefreshOptions buildClusterTopologyRefreshOptions(ClusterConfig config) {
        if (!config.clusterAdaptive && config.clusterRefreshPeriod == null) {
            return null;
        }
        ClusterTopologyRefreshOptions.Builder builder = ClusterTopologyRefreshOptions.builder();
        if (config.clusterAdaptive) {
            builder.enableAllAdaptiveRefreshTriggers();
        }
        if (config.clusterRefreshPeriod != null) {
            builder.enablePeriodicRefresh(config.clusterRefreshPeriod);
        }
        return builder.build();
    }

    static RedisTemplate<String, Object> createRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet();
        return template;
    }

    static StringRedisTemplate createStringRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

    private static class ClusterConfig {
        // Standalone mode
        String host = "localhost";
        int port = 6379;
        int database = 0;

        // Common
        String password;
        Duration timeout;

        // Cluster mode
        boolean clusterMode = false;
        List<String> clusterNodes;
        Integer clusterMaxRedirects;
        boolean clusterAdaptive = false;
        Duration clusterRefreshPeriod;

        // Pool config
        boolean poolEnabled = false;
        int poolMaxActive = 8;
        int poolMaxIdle = 8;
        int poolMinIdle = 0;
        Duration poolMaxWait;
    }
}
