package org.hongxi.redis.multi.sample;

import org.hongxi.redis.multi.annotation.ConditionalOnAutoRegisterDisabled;
import org.hongxi.redis.multi.RedisTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Configuration for <b>Builder mode</b> (auto-register disabled).
 * <p>
 * Demonstrates how to manually define RedisTemplate beans
 * for different Redis clusters using {@link RedisTemplateBuilder}.
 * <p>
 * The clusters to create beans for are dynamically detected from configuration,
 * so this class works across all test scenarios (official/clusters/mixed).
 * <p>
 * Activate Builder mode by adding:
 * <pre>
 * --spring.data.redis.auto-register=false
 * </pre>
 *
 * @author javahongxi
 * @see org.hongxi.redis.multi.RedisTemplateBuilder
 */
@Configuration
@ConditionalOnAutoRegisterDisabled
public class BuilderModeConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(BuilderModeConfig.class);

    private final Environment environment;
    private Set<String> clusterNames;

    public BuilderModeConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        clusterNames = doDetectClusterNames();
        log.info("[sample] Builder mode: detected clusters = {}", clusterNames);
    }

    private boolean hasCluster(String name) {
        return clusterNames.contains(name);
    }

    private Set<String> doDetectClusterNames() {
        Set<String> names = new LinkedHashSet<>();

        // Check multi-cluster format
        if (environment instanceof ConfigurableEnvironment ce) {
            for (PropertySource<?> ps : ce.getPropertySources()) {
                if (ps instanceof EnumerablePropertySource<?> eps) {
                    for (String name : eps.getPropertyNames()) {
                        if (name.startsWith("spring.data.redis.clusters.")) {
                            String rest = name.substring("spring.data.redis.clusters.".length());
                            int dot = rest.indexOf('.');
                            if (dot > 0) {
                                names.add(rest.substring(0, dot));
                            }
                        }
                    }
                }
            }
        }

        // Check official format -> "default"
        if (environment.getProperty("spring.data.redis.host") != null
                || environment.getProperty("spring.data.redis.url") != null
                || environment.getProperty("spring.data.redis.cluster.nodes") != null) {
            names.add("default");
        }

        return Collections.unmodifiableSet(names);
    }

    // --- RedisTemplate beans ---

    @Bean
    public RedisTemplate<String, Object> defaultRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("default")) return null;
        return buildRedisTemplate(builder, "default");
    }

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("order")) return null;
        return buildRedisTemplate(builder, "order");
    }

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("user")) return null;
        return buildRedisTemplate(builder, "user");
    }

    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("cache")) return null;
        return buildRedisTemplate(builder, "cache");
    }

    @Bean
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("session")) return null;
        return buildRedisTemplate(builder, "session");
    }

    // --- StringRedisTemplate beans ---

    @Bean
    public StringRedisTemplate defaultStringRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("default")) return null;
        return builder.stringTemplate("default");
    }

    @Bean
    public StringRedisTemplate orderStringRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("order")) return null;
        return builder.stringTemplate("order");
    }

    @Bean
    public StringRedisTemplate userStringRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("user")) return null;
        return builder.stringTemplate("user");
    }

    @Bean
    public StringRedisTemplate cacheStringRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("cache")) return null;
        return builder.stringTemplate("cache");
    }

    @Bean
    public StringRedisTemplate sessionStringRedisTemplate(RedisTemplateBuilder builder) {
        if (!hasCluster("session")) return null;
        return builder.stringTemplate("session");
    }

    // --- Helper ---

    private RedisTemplate<String, Object> buildRedisTemplate(RedisTemplateBuilder builder, String cluster) {
        return builder.cluster(cluster)
                .keySerializer(RedisSerializer.string())
                .valueSerializer(RedisSerializer.json())
                .hashKeySerializer(RedisSerializer.string())
                .hashValueSerializer(RedisSerializer.json())
                .build();
    }
}
