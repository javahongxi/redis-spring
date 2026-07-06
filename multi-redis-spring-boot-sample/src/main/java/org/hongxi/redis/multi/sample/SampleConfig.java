package org.hongxi.redis.multi.sample;

import org.hongxi.redis.multi.RedisTemplateBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Demonstrates how to create multiple RedisTemplate instances
 * for different Redis clusters using the Builder pattern.
 * <p>
 * <b>Mode 1 - Builder</b> (default): Define beans manually as shown below.
 * <p>
 * <b>Mode 2 - Auto-register</b>: Set {@code spring.data.redis.auto-register=true},
 * beans {@code orderRedisTemplate}, {@code userRedisTemplate}, {@code cacheRedisTemplate},
 * {@code sessionRedisTemplate} and their corresponding {@code StringRedisTemplate}
 * will be automatically registered. No code needed here.
 *
 * @author javahongxi
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.auto-register", havingValue = "false", matchIfMissing = true)
public class SampleConfig {

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("order");
    }

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("user");
    }

    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("cache");
    }

    @Bean
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("session");
    }

    // --- StringRedisTemplate beans ---

    @Bean
    public StringRedisTemplate orderStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("order");
    }

    @Bean
    public StringRedisTemplate userStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("user");
    }

    @Bean
    public StringRedisTemplate cacheStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("cache");
    }

    @Bean
    public StringRedisTemplate sessionStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("session");
    }
}
