package org.hongxi.redis.multi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting RedisTemplate instances bound to specific Redis clusters.
 * <p>
 * Usage:
 * <pre>
 * &#64;RedisCluster("order")
 * private RedisTemplate&lt;String, Object&gt; orderRedisTemplate;
 *
 * &#64;RedisCluster("order")
 * private StringRedisTemplate orderStringRedisTemplate;
 * </pre>
 * <p>
 * The cluster name must match a cluster defined in {@code spring.data.redis.clusters.{name}}.
 * When using official Spring Boot Redis configuration format, the cluster name is "default".
 *
 * @author javahongxi
 * @see org.hongxi.redis.multi.MultiRedisProperties
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCluster {

    /**
     * The name of the Redis cluster as defined in configuration.
     * <p>
     * For multi-redis format: {@code spring.data.redis.clusters.{value}}
     * <br>
     * For official format: use "default"
     *
     * @return the cluster name
     */
    String value();
}
