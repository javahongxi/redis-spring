# multi-redis-spring-boot-starter

Spring Boot Starter for multiple Redis clusters. Supports Builder pattern for creating multiple `RedisTemplate` instances with different cluster configurations.

## Features

- Multiple Redis cluster configurations in a single application
- Builder pattern API (inspired by `ChatClient.Builder`)
- Full compatibility with Spring Boot's official Redis Starter
- Support for Standalone, Sentinel, and Cluster modes
- Auto-configuration with `@ConfigurationProperties`

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.hongxi</groupId>
    <artifactId>multi-redis-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configuration

```yaml
spring:
  redis:
    clusters:
      order:
        host: redis-order:6379
        password: order-pass
      user:
        host: redis-user:6379
        password: user-pass
```

### Usage

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("order").build();
    }

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("user").build();
    }
}
```

## Project Structure

```
multi-redis-spring-boot-starter/
├── multi-redis-spring-boot-autoconfigure/   # Auto-configuration module
├── multi-redis-spring-boot-starter/         # Starter (dependencies only)
└── multi-redis-spring-boot-sample/          # Sample project
```

## License

Apache License 2.0
