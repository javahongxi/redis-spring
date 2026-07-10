# multi-redis-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/org.hongxi/multi-redis-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.hongxi/multi-redis-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot Starter for connecting to multiple Redis instances/clusters from a single application. Supports two modes: **Mode 1 - Builder + Annotation** (code-controlled) and **Mode 2 - Auto-register** (zero-code with YAML configuration) for creating multiple `RedisTemplate` / `StringRedisTemplate` instances with different cluster configurations.

## Features

- Multiple Redis cluster configurations in a single application
- **Mode 1 - Builder + Annotation** (code-controlled):
  - Inject `RedisTemplateBuilder` to manually create `RedisTemplate` and `StringRedisTemplate` beans
  - Use `@RedisCluster("name")` annotation to inject templates directly into fields
- **Mode 2 - Auto-register** (zero-code): auto-register beans with YAML serializer configuration. **Auto-activated** when Redis configuration is detected.
- Standalone and Redis Cluster mode support
- **Official Spring Boot Redis configuration format compatibility** — switch from official starter without changing config
- Automatic exclusion of Spring Boot's default Redis auto-configurations

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.hongxi</groupId>
    <artifactId>multi-redis-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

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

> **Note**: `auto-register` is auto-detected. Set `spring.data.redis.auto-register=false` to force Builder mode, or `true` to force Auto-register mode.

### Activation Strategy

| Scenario             | Configuration                               | Activated Mode     |
|----------------------|---------------------------------------------|--------------------|
| No Redis config      | _(none)_                                    | Builder mode       |
| Multi-cluster format | `spring.data.redis.clusters.order.host=...` | Auto-register mode |
| Official standalone  | `spring.data.redis.host=127.0.0.1`          | Auto-register mode |
| Official cluster     | `spring.data.redis.cluster.nodes=...`       | Auto-register mode |
| Explicit enable      | `spring.data.redis.auto-register: true`     | Auto-register mode |
| Explicit disable     | `spring.data.redis.auto-register: false`    | Builder mode       |

> **Priority**: Explicit `auto-register` setting > Auto-detection

Optional per-cluster settings: `url`, `username`, `password`, `database`, `timeout`, `connect-timeout`, `cluster.read-from` (read from replica, e.g. `REPLICA_PREFERRED`), `lettuce.pool.*`, `lettuce.cluster.refresh.*`.

### YAML Serializer Configuration

Both modes support configuring serializers in YAML (works for `RedisTemplateBuilder` and auto-registered beans):

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

### Mode 1 - Builder + Annotation (Code-Controlled)

This is the default mode. Use `RedisTemplateBuilder` to manually create beans, or use `@RedisCluster` annotation for direct field injection.

#### Option A: Builder Pattern

Manually define `RedisTemplate` and `StringRedisTemplate` beans using `RedisTemplateBuilder`:

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("order").build();
    }

    @Bean
    public StringRedisTemplate orderStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("order");
    }
}
```

#### Option B: @RedisCluster Annotation

Use `@RedisCluster("name")` annotation to inject `RedisTemplate` instances directly into fields. This is the simplest approach:

```java
@Service
public class OrderService {

    @RedisCluster("order") // org.hongxi.redis.multi.annotation.RedisCluster
    private RedisTemplate<String, Object> orderRedisTemplate;

    @RedisCluster("order")
    private StringRedisTemplate orderStringRedisTemplate;
}
```

When using official Spring Boot Redis configuration format, use `"default"` as the cluster name:

```java
@RedisCluster("default")
private RedisTemplate<String, Object> redisTemplate;
```

> **Note**: `@RedisCluster` annotation only works in Builder mode (Mode 1). It is disabled when Auto-register mode is active.

### Mode 2 - Auto Register (Zero-Code)

Beans are automatically registered as `{clusterName}RedisTemplate` / `{clusterName}StringRedisTemplate`. Just inject directly:

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

## License

Apache License 2.0

&copy; [hongxi.org](http://hongxi.org)
