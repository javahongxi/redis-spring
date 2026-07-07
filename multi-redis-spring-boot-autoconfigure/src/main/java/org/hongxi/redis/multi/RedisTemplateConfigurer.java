package org.hongxi.redis.multi;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Configurer for building {@link RedisTemplate} with custom serializers.
 * <p>
 * This class provides a fluent API for configuring serializers on a RedisTemplate.
 * It is returned by {@link RedisTemplateBuilder#cluster(String)} and allows
 * chain-style configuration before building the final template.
 * <p>
 * Example usage:
 * <pre>
 * &#64;Bean
 * public RedisTemplate&lt;String, Object&gt; orderRedisTemplate(RedisTemplateBuilder builder) {
 *     return builder.cluster("order")
 *         .keySerializer(RedisSerializer.string())
 *         .valueSerializer(new GenericJackson2JsonRedisSerializer())
 *         .hashKeySerializer(RedisSerializer.string())
 *         .hashValueSerializer(new GenericJackson2JsonRedisSerializer())
 *         .build();
 * }
 * </pre>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author javahongxi
 * @see RedisTemplateBuilder
 */
public class RedisTemplateConfigurer<K, V> {

    private final RedisTemplate<K, V> template;

    RedisTemplateConfigurer(RedisTemplate<K, V> template) {
        this.template = template;
    }

    /**
     * Set the key serializer.
     *
     * @param serializer the key serializer
     * @return this configurer for chaining
     */
    public RedisTemplateConfigurer<K, V> keySerializer(RedisSerializer<?> serializer) {
        template.setKeySerializer(serializer);
        return this;
    }

    /**
     * Set the value serializer.
     *
     * @param serializer the value serializer
     * @return this configurer for chaining
     */
    public RedisTemplateConfigurer<K, V> valueSerializer(RedisSerializer<?> serializer) {
        template.setValueSerializer(serializer);
        return this;
    }

    /**
     * Set the hash key serializer.
     *
     * @param serializer the hash key serializer
     * @return this configurer for chaining
     */
    public RedisTemplateConfigurer<K, V> hashKeySerializer(RedisSerializer<?> serializer) {
        template.setHashKeySerializer(serializer);
        return this;
    }

    /**
     * Set the hash value serializer.
     *
     * @param serializer the hash value serializer
     * @return this configurer for chaining
     */
    public RedisTemplateConfigurer<K, V> hashValueSerializer(RedisSerializer<?> serializer) {
        template.setHashValueSerializer(serializer);
        return this;
    }

    /**
     * Set all serializers at once using the same serializer.
     *
     * @param serializer the serializer to use for all fields
     * @return this configurer for chaining
     */
    public RedisTemplateConfigurer<K, V> serializers(RedisSerializer<?> serializer) {
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        return this;
    }

    /**
     * Build and return the configured {@link RedisTemplate}.
     * <p>
     * This method calls {@code afterPropertiesSet()} on the template to finalize initialization.
     *
     * @return the configured RedisTemplate
     */
    public RedisTemplate<K, V> build() {
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Get the underlying RedisTemplate without calling afterPropertiesSet().
     * <p>
     * Use this if you need to perform additional configuration before initialization.
     * Remember to call {@code afterPropertiesSet()} manually when done.
     *
     * @return the underlying RedisTemplate
     */
    public RedisTemplate<K, V> getTemplate() {
        return template;
    }
}
