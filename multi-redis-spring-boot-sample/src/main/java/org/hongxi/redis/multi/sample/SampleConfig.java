package org.hongxi.redis.multi.sample;

import org.hongxi.redis.multi.RedisTemplateBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Demonstrates how to create multiple RedisTemplate instances
 * for different Redis clusters using the Builder pattern.
 * <p>
 * <b>Mode 1 - Builder</b> (default): Define beans manually as shown below.
 * <p>
 * <b>Mode 2 - Auto-register</b>: Set {@code spring.redis.auto-register=true},
 * beans {@code orderRedisTemplate} and {@code userRedisTemplate} will be
 * automatically registered. No code needed here.
 *
 * @author javahongxi
 */
@Configuration
@ConditionalOnProperty(name = "spring.redis.auto-register", havingValue = "false", matchIfMissing = true)
public class SampleConfig {

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("order");
    }

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("user");
    }
}
