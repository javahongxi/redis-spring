# multi-redis-spring-boot-starter

Spring Boot Starter for multiple Redis clusters. Supports **Builder pattern** and **Auto-register** mode for creating multiple `RedisTemplate` instances with different cluster configurations.

## Features

- Multiple Redis cluster configurations in a single application
- **Mode 1 - Builder pattern** (inspired by `ChatClient.Builder`): manually create `RedisTemplate` beans
- **Mode 2 - Auto-register**: automatically register `RedisTemplate` beans via `ImportBeanDefinitionRegistrar`
- Lettuce connection pool support (commons-pool2)
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
    auto-register: true          # Enable auto-register mode (default: false)
    clusters:
      order:
        host: localhost
        port: 6379
        password: order-pass
        database: 0
        timeout: 3s
        lettuce:
          pool:
            max-active: 16
            max-idle: 8
            min-idle: 2
      user:
        host: localhost
        port: 6380
        password: user-pass
        database: 0
        lettuce:
          pool:
            max-active: 8
            max-idle: 4
            min-idle: 1
```

### Mode 1 - Builder Pattern

Manually define `RedisTemplate` beans using `RedisTemplateBuilder`:

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("order");
    }

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("user");
    }
}
```

### Mode 2 - Auto Register

Set `spring.redis.auto-register=true`, beans will be automatically registered with the naming convention:
- `{clusterName}RedisConnectionFactory` — e.g. `orderRedisConnectionFactory`
- `{clusterName}RedisTemplate` — e.g. `orderRedisTemplate`

No code needed, just inject directly:

```java
@RestController
public class MyController {

    private final RedisTemplate<String, Object> orderRedisTemplate;
    private final RedisTemplate<String, Object> userRedisTemplate;

    public MyController(RedisTemplate<String, Object> orderRedisTemplate,
                        RedisTemplate<String, Object> userRedisTemplate) {
        this.orderRedisTemplate = orderRedisTemplate;
        this.userRedisTemplate = userRedisTemplate;
    }
}
```

> **Note**: When using auto-register mode, you may need to exclude Spring Boot's default Redis auto-configuration:
> ```java
> @SpringBootApplication(exclude = {
>     RedisAutoConfiguration.class,
>     RedisReactiveAutoConfiguration.class,
>     RedisRepositoriesAutoConfiguration.class
> })
> ```

## Project Structure

```
multi-redis-spring-boot-starter/
├── multi-redis-spring-boot-autoconfigure/   # Auto-configuration module
├── multi-redis-spring-boot-starter/         # Starter (dependencies only)
└── multi-redis-spring-boot-sample/          # Sample project
```

## License

Apache License 2.0
