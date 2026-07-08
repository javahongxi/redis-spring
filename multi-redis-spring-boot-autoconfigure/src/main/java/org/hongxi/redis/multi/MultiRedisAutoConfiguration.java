package org.hongxi.redis.multi;

import org.hongxi.redis.multi.annotation.ConditionalOnAutoRegisterDisabled;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Autoconfiguration for multiple Redis clusters support.
 * <p>
 * <b>Mode 1 - Builder + Annotation</b> (default): Code-controlled approach.
 * <ul>
 *   <li>Inject {@link RedisTemplateBuilder} to create {@code RedisTemplate} and {@code StringRedisTemplate}
 *       instances manually</li>
 *   <li>Use {@code @RedisCluster("name")} annotation to inject templates directly into fields</li>
 * </ul>
 * <p>
 * <b>Mode 2 - Auto-register</b>: Zero-code approach with YAML configuration.
 * Set {@code spring.data.redis.auto-register=true} to automatically register
 * {@code RedisTemplate} and {@code StringRedisTemplate} beans for each cluster.
 * Serializers can be configured via YAML.
 * <p>
 * These two modes are mutually exclusive. Mode 1 is active by default.
 *
 * @author javahongxi
 */
@AutoConfiguration
@EnableConfigurationProperties(MultiRedisProperties.class)
@Import(MultiRedisRegistrar.class)
public class MultiRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAutoRegisterDisabled
    public RedisTemplateBuilder redisTemplateBuilder(MultiRedisProperties properties, Environment environment) {
        return new RedisTemplateBuilder(properties, environment);
    }

    /**
     * Register RedisClusterBeanPostProcessor to handle @RedisCluster annotation injection.
     * This bean is created when RedisTemplateBuilder is available (either manually defined or auto-configured).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisTemplateBuilder.class)
    public RedisClusterBeanPostProcessor redisClusterBeanPostProcessor(RedisTemplateBuilder builder) {
        return new RedisClusterBeanPostProcessor(builder);
    }
}
