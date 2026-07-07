package org.hongxi.redis.multi;

import org.hongxi.redis.multi.annotation.RedisCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * BeanPostProcessor that handles {@link RedisCluster} annotation for automatic
 * RedisTemplate injection.
 * <p>
 * This processor scans all beans for fields annotated with {@code @RedisCluster}
 * and injects the appropriate RedisTemplate or StringRedisTemplate instance
 * based on the cluster name specified in the annotation.
 *
 * @author javahongxi
 * @see RedisCluster
 * @see RedisTemplateBuilder
 */
public class RedisClusterBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(RedisClusterBeanPostProcessor.class);

    private final RedisTemplateBuilder builder;

    public RedisClusterBeanPostProcessor(RedisTemplateBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        
        // Process all fields (including private fields)
        ReflectionUtils.doWithFields(clazz, field -> {
            RedisCluster annotation = AnnotationUtils.getAnnotation(field, RedisCluster.class);
            if (annotation != null) {
                injectRedisTemplate(bean, field, annotation);
            }
        }, field -> AnnotationUtils.getAnnotation(field, RedisCluster.class) != null);
        
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private void injectRedisTemplate(Object bean, Field field, RedisCluster annotation) {
        String clusterName = annotation.value();
        Class<?> fieldType = field.getType();
        
        log.debug("[multi-redis] Injecting RedisTemplate for cluster '{}' into field '{}'", 
                clusterName, field.getName());
        
        try {
            Object template;
            
            if (StringRedisTemplate.class.isAssignableFrom(fieldType)) {
                template = builder.stringTemplate(clusterName);
                log.info("[multi-redis] Injected StringRedisTemplate for cluster '{}' into {}.{}", 
                        clusterName, bean.getClass().getSimpleName(), field.getName());
            } else if (RedisTemplate.class.isAssignableFrom(fieldType)) {
                template = builder.cluster(clusterName).build();
                log.info("[multi-redis] Injected RedisTemplate for cluster '{}' into {}.{}", 
                        clusterName, bean.getClass().getSimpleName(), field.getName());
            } else {
                throw new IllegalArgumentException(
                        "Field '" + field.getName() + "' in class '" + bean.getClass().getName() + 
                        "' annotated with @RedisCluster must be of type RedisTemplate or StringRedisTemplate, " +
                        "but found type: " + fieldType.getName());
            }
            
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, template);
            
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Failed to inject RedisTemplate for cluster '" + clusterName + "' into field '" + 
                    field.getName() + "': " + e.getMessage(), e);
        }
    }
}
