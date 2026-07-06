package org.hongxi.redis.multi;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * {@link FactoryBean} that creates a {@link RedisTemplate} backed by
 * a named {@link LettuceConnectionFactory} bean.
 * <p>
 * Used by {@link MultiRedisRegistrar} in auto-register mode to wire
 * each cluster's connection factory to its corresponding template.
 *
 * @author javahongxi
 * @see MultiRedisRegistrar
 */
public class RedisTemplateFactoryBean implements FactoryBean<RedisTemplate<String, Object>>, BeanFactoryAware {

    private final String connectionFactoryBeanName;
    private BeanFactory beanFactory;

    public RedisTemplateFactoryBean(String connectionFactoryBeanName) {
        this.connectionFactoryBeanName = connectionFactoryBeanName;
    }

    @Override
    public RedisTemplate<String, Object> getObject() {
        LettuceConnectionFactory factory = beanFactory.getBean(connectionFactoryBeanName, LettuceConnectionFactory.class);
        return MultiRedisRegistrar.createRedisTemplate(factory);
    }

    @Override
    public Class<?> getObjectType() {
        return RedisTemplate.class;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
