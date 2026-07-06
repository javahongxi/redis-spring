package org.hongxi.redis.multi;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for multiple Redis clusters support.
 * <p>
 * <b>Builder mode</b> (default): Inject {@link RedisTemplateBuilder} to create
 * {@code RedisTemplate} instances manually.
 * <p>
 * <b>Auto-register mode</b>: Set {@code spring.redis.auto-register=true} to
 * automatically register {@code RedisTemplate} beans for each cluster.
 *
 * @author javahongxi
 */
@AutoConfiguration
@EnableConfigurationProperties(MultiRedisProperties.class)
@Import(MultiRedisRegistrar.class)
public class MultiRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplateBuilder redisTemplateBuilder(MultiRedisProperties properties) {
        return new RedisTemplateBuilder(properties);
    }
}
