package org.hongxi.redis.multi;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Automatically excludes Spring Boot's default Redis auto-configurations
 * when multi-redis starter is on the classpath.
 * <p>
 * This saves users from having to manually exclude them via
 * {@code @SpringBootApplication(exclude = {...})}.
 *
 * @author javahongxi
 */
public class MultiRedisAutoConfigurationImportFilter implements AutoConfigurationImportFilter {

    private static final Set<String> EXCLUDED_CLASSES = new HashSet<>(Arrays.asList(
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    ));

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] match = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            match[i] = !EXCLUDED_CLASSES.contains(autoConfigurationClasses[i]);
        }
        return match;
    }
}
