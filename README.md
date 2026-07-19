# multi-redis-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/org.hongxi/multi-redis-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.hongxi/multi-redis-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot Starter for connecting to multiple Redis instances/clusters from a single application. **Auto-register** mode (zero-code with YAML) is the recommended approach — just add the dependency, write YAML config, and inject `RedisTemplate` by name. For advanced scenarios, **Builder mode** provides full programmatic control via `RedisTemplateBuilder`.

## Features

- Multiple Redis cluster configurations in a single application
- **Auto-register** (zero-code, recommended): auto-register beans with YAML serializer configuration. **Auto-activated** when Redis configuration is detected.
- **Builder mode** (advanced): inject `RedisTemplateBuilder` for full programmatic control over template creation
- Standalone and Redis Cluster mode support
- **Official Spring Boot Redis configuration format compatibility** — switch from official starter without changing config
- Automatic exclusion of Spring Boot's default Redis auto-configurations

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.hongxi</groupId>
    <artifactId>multi-redis-spring-boot-starter</artifactId>
    <version>1.0.5</version>
</dependency>
```

> **Note**: If you are using **Spring Boot 4.x**, you need to add the following dependency explicitly:
> ```xml
> <dependency>
>     <groupId>tools.jackson.core</groupId>
>     <artifactId>jackson-databind</artifactId>
> </dependency>
> ```

### Configuration

**Multi-cluster format**:

```yaml
spring:
  data:
    redis:
      clusters:
        order:                     # Standalone → orderRedisTemplate
          host: localhost
          port: 6380
        user:                      # Standalone → userRedisTemplate
          host: localhost
          port: 6381
        cache:                     # Redis Cluster → cacheRedisTemplate
          cluster:
            nodes: localhost:7001,localhost:7002,localhost:7003
        session:                   # Redis Cluster → sessionRedisTemplate
          cluster:
            nodes: localhost:7011,localhost:7012,localhost:7013
```

**Official format** (zero-config migration — just replace the dependency, no config change needed):

```yaml
spring:
  data:
    redis:
      host: localhost              # → defaultRedisTemplate
      port: 6379
```

Official format (`spring.data.redis.host/port` or `spring.data.redis.cluster.nodes`) also works — auto-detected as the `default` cluster. Both formats can coexist.

### Usage

Beans are automatically registered as `{clusterName}RedisTemplate` / `{clusterName}StringRedisTemplate`. Just inject by name:

```java
@Service
public class OrderService {

    private final RedisTemplate<String, Object> orderRedisTemplate;
    private final StringRedisTemplate userStringRedisTemplate;

    public OrderService(RedisTemplate<String, Object> orderRedisTemplate,
                        StringRedisTemplate userStringRedisTemplate) {
        this.orderRedisTemplate = orderRedisTemplate;
        this.userStringRedisTemplate = userStringRedisTemplate;
    }
}
```

That's it — no `@Configuration` class, no manual bean definition.

### YAML Serializer Configuration

Configure serializers per-cluster in YAML:

```yaml
spring:
  data:
    redis:
      clusters:
        order:
          host: localhost
          port: 6379
          serializer:
            key: string
            value: json
            hash-key: string
            hash-value: json
```

Supported serializer types:
- `java` - `JdkSerializationRedisSerializer` (official default)
- `json` - `GenericJackson2JsonRedisSerializer`
- `string` - `StringRedisSerializer`
- `byteArray` - `ByteArrayRedisSerializer`

Optional per-cluster settings: `url`, `username`, `password`, `database`, `timeout`, `connect-timeout`, `cluster.read-from` (read from replica, e.g. `REPLICA_PREFERRED`), `lettuce.pool.*`, `lettuce.cluster.refresh.*`.

## Advanced Usage

### Builder Mode (Manual Control)

When you need programmatic control (e.g. conditional creation, custom pipeline), inject `RedisTemplateBuilder` to manually define beans. YAML-configured serializers are applied by default — override them programmatically when needed:

```java
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        // Override serializers programmatically
        return builder.cluster("order")
                .keySerializer(RedisSerializer.string())
                .valueSerializer(RedisSerializer.json())
                .hashKeySerializer(RedisSerializer.string())
                .hashValueSerializer(RedisSerializer.json())
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        // Use YAML-configured serializers (see YAML Serializer Configuration above)
        return builder.cluster("user").build();
    }

    @Bean
    public StringRedisTemplate orderStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("order");
    }

    @Bean
    public StringRedisTemplate userStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("user");
    }
}
```

> **Note**: `RedisTemplateBuilder` can be injected into any Spring-managed bean — not just `@Configuration` classes. Use it in `@Service`, `@Component`, or any other bean for maximum flexibility.

### Mode Switching

Auto-register is auto-detected. Set `spring.data.redis.auto-register=false` to force Builder mode, or `true` to force Auto-register mode.

| Scenario             | Configuration                               | Activated Mode     |
|----------------------|---------------------------------------------|--------------------|
| Multi-cluster format | `spring.data.redis.clusters.order.host=...` | Auto-register mode |
| Official standalone  | `spring.data.redis.host=127.0.0.1`          | Auto-register mode |
| Official cluster     | `spring.data.redis.cluster.nodes=...`       | Auto-register mode |
| Explicit enable      | `spring.data.redis.auto-register: true`     | Auto-register mode |
| Explicit disable     | `spring.data.redis.auto-register: false`    | Builder mode       |

> **Priority**: Explicit `auto-register` setting > Auto-detection

## License

Apache License 2.0

&copy; [hongxi.org](http://hongxi.org)
