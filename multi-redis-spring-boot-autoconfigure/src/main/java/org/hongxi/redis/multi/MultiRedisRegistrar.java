package org.hongxi.redis.multi;

import io.lettuce.core.api.StatefulConnection;
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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Automatically registers {@link RedisTemplate} beans for each cluster
 * defined in {@code spring.redis.clusters}.
 * <p>
 * Enabled when {@code spring.redis.auto-register=true}.
 * <p>
 * For each cluster entry, a {@link LettuceConnectionFactory} and a
 * {@link RedisTemplate} bean are registered with the naming convention:
 * <ul>
 *   <li>{@code {clusterName}RedisConnectionFactory}</li>
 *   <li>{@code {clusterName}RedisTemplate}</li>
 * </ul>
 *
 * @author javahongxi
 * @see MultiRedisAutoConfiguration
 */
public class MultiRedisRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        String autoRegister = environment.getProperty("spring.redis.auto-register", "false");
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

            // Register RedisTemplate bean using RedisTemplateFactoryBean
            String templateBeanName = clusterName + "RedisTemplate";
            GenericBeanDefinition templateDef = new GenericBeanDefinition();
            templateDef.setBeanClass(RedisTemplateFactoryBean.class);
            templateDef.getConstructorArgumentValues().addGenericArgumentValue(factoryBeanName);
            registry.registerBeanDefinition(templateBeanName, templateDef);
        }
    }

    private Map<String, ClusterConfig> resolveClusters() {
        Map<String, ClusterConfig> clusters = new LinkedHashMap<>();
        if (!(environment instanceof ConfigurableEnvironment)) {
            return clusters;
        }

        ConfigurableEnvironment ce = (ConfigurableEnvironment) environment;
        Set<String> clusterNames = new LinkedHashSet<>();
        for (PropertySource<?> ps : ce.getPropertySources()) {
            if (ps instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> eps = (EnumerablePropertySource<?>) ps;
                for (String name : eps.getPropertyNames()) {
                    if (name.startsWith("spring.redis.clusters.")) {
                        String rest = name.substring("spring.redis.clusters.".length());
                        int dot = rest.indexOf('.');
                        if (dot > 0) {
                            clusterNames.add(rest.substring(0, dot));
                        }
                    }
                }
            }
        }

        for (String clusterName : clusterNames) {
            String prefix = "spring.redis.clusters." + clusterName + ".";
            ClusterConfig cc = new ClusterConfig();
            cc.host = environment.getProperty(prefix + "host", "localhost");
            cc.port = Integer.parseInt(environment.getProperty(prefix + "port", "6379"));
            cc.password = environment.getProperty(prefix + "password");
            cc.database = Integer.parseInt(environment.getProperty(prefix + "database", "0"));
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
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(config.host);
        redisConfig.setPort(config.port);
        redisConfig.setDatabase(config.database);
        if (config.password != null && !config.password.isEmpty()) {
            redisConfig.setPassword(config.password);
        }

        LettuceConnectionFactory factory;
        if (config.poolEnabled) {
            GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(config.poolMaxActive);
            poolConfig.setMaxIdle(config.poolMaxIdle);
            poolConfig.setMinIdle(config.poolMinIdle);
            if (config.poolMaxWait != null) {
                poolConfig.setMaxWait(config.poolMaxWait);
            }
            LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                    LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
            if (config.timeout != null) {
                builder.commandTimeout(config.timeout);
            }
            factory = new LettuceConnectionFactory(redisConfig, builder.build());
        } else {
            factory = new LettuceConnectionFactory(redisConfig);
            if (config.timeout != null) {
                factory.setTimeout(config.timeout.toMillis());
            }
        }
        factory.afterPropertiesSet();
        return factory;
    }

    static RedisTemplate<String, Object> createRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    private static class ClusterConfig {
        String host = "localhost";
        int port = 6379;
        String password;
        int database = 0;
        Duration timeout;
        boolean poolEnabled = false;
        int poolMaxActive = 8;
        int poolMaxIdle = 8;
        int poolMinIdle = 0;
        Duration poolMaxWait;
    }
}
