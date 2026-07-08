# multi-redis-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/org.hongxi/multi-redis-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.hongxi/multi-redis-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot Starter for connecting to multiple Redis instances/clusters from a single application. Supports two modes: **Mode 1 - Builder + Annotation** (code-controlled) and **Mode 2 - Auto-register** (zero-code with YAML configuration) for creating multiple `RedisTemplate` / `StringRedisTemplate` instances with different cluster configurations.

**Auto-detection**: When using official Spring Boot Redis configuration format (`spring.data.redis.host/port` or `spring.data.redis.cluster.nodes`) or multi-cluster format (`spring.data.redis.clusters.*`), Auto-register mode is automatically activated — no need to set `auto-register: true`.

## Features

- Multiple Redis cluster configurations in a single application
- **Mode 1 - Builder + Annotation** (code-controlled):
  - Inject `RedisTemplateBuilder` to manually create `RedisTemplate` and `StringRedisTemplate` beans
  - Use `@RedisCluster("name")` annotation to inject templates directly into fields
- **Mode 2 - Auto-register** (zero-code): auto-register beans with YAML serializer configuration. **Auto-activated** when Redis configuration is detected.
- Standalone and Redis Cluster mode support
- **Official Spring Boot Redis configuration format compatibility** — switch from official starter without changing config
- Automatic exclusion of Spring Boot's default `RedisAutoConfiguration`

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.hongxi</groupId>
    <artifactId>multi-redis-spring-boot-starter</artifactId>
    <version>1.0.3</version>
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

Official cluster format also works: `spring.data.redis.cluster.nodes` is auto-detected as the `default` cluster.

**Coexistence** (official format + multi-cluster format, both work together):

```yaml
spring:
  data:
    redis:
      host: localhost              # → defaultRedisTemplate (6379)
      port: 6379
      clusters:
        order:                     # → orderRedisTemplate (6380)
          host: localhost
          port: 6380
        user:                      # → userRedisTemplate (6381)
          host: localhost
          port: 6381
```

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

    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("user").build();
    }
}
```

##### Custom Serializers

Use chain-style configuration to customize serializers:

```java
@Bean
public RedisTemplate<String, Object> orderRedisTemplate(RedisTemplateBuilder builder) {
    return builder.cluster("order")
        .keySerializer(RedisSerializer.string())
        .valueSerializer(RedisSerializer.json())
        .hashKeySerializer(RedisSerializer.string())
        .hashValueSerializer(RedisSerializer.json())
        .build();
}

// Or set all serializers at once
@Bean
public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
    return builder.cluster("user")
        .serializers(RedisSerializer.byteArray())
        .build();
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

Auto-register mode is **automatically activated** when:
- Official Spring Boot Redis format is detected (`spring.data.redis.host/port` or `spring.data.redis.cluster.nodes`)
- Multi-cluster format is detected (`spring.data.redis.clusters.*`)

You can also explicitly enable it with `spring.data.redis.auto-register=true`.

Beans will be automatically registered with the naming convention:
- `{clusterName}RedisConnectionFactory` — e.g. `orderRedisConnectionFactory`
- `{clusterName}RedisTemplate` — e.g. `orderRedisTemplate`
- `{clusterName}StringRedisTemplate` — e.g. `orderStringRedisTemplate`

No code needed, just inject directly:

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

> **Note**: Spring Boot's default `RedisAutoConfiguration` is automatically excluded by 
> `MultiRedisAutoConfigurationImportFilter` when this starter is on the classpath.

## Sample

The `multi-redis-spring-boot-sample` module demonstrates both standalone and Redis Cluster usage with 5 Redis targets (official format + multi-cluster format mixed):

| Name    | Mode       | Connection            |
|---------|------------|-----------------------|
| default | Standalone | localhost:6379        |
| order   | Standalone | localhost:6380        |
| user    | Standalone | localhost:6381        |
| cache   | Cluster    | localhost:7001-7003   |
| session | Cluster    | localhost:7011-7013   |

### Prerequisites

#### Standalone Redis (default, order & user)

```bash
# Start default Redis on port 6379
brew services start redis

# Start order Redis on port 6380
redis-server --port 6380 --daemonize yes --logfile /tmp/redis-6380.log
# Start user Redis on port 6381
redis-server --port 6381 --daemonize yes --logfile /tmp/redis-6381.log
```

#### Redis Cluster (cache & session)

Use the provided `redis-cluster.sh` script:

```bash
# Start all clusters (default, no argument needed)
./redis-cluster.sh start

# Start specific cluster
./redis-cluster.sh start cache      # 7001-7006 (3 masters + 3 replicas)
./redis-cluster.sh start session    # 7011-7016 (3 masters + 3 replicas)

# Check status
./redis-cluster.sh status

# Stop clusters
./redis-cluster.sh stop all
```

### Run

```bash
cd multi-redis-spring-boot-sample
mvn spring-boot:run
```

## Why Not Official Starter?

| Feature                    | Official `spring-boot-starter-data-redis` | `multi-redis-spring-boot-starter` |
|----------------------------|-------------------------------------------|-----------------------------------|
| Multiple Redis clusters    | ❌ Single instance/cluster only            | ✅ Multiple clusters in one app    |
| Official format compatible | ✅                                         | ✅ Zero-config migration           |
| Auto-register beans        | ✅ Single `redisTemplate`                  | ✅ Multiple `{name}RedisTemplate`  |
| Serializer configuration   | ❌ Code only                               | ✅ YAML configuration              |

**When to use this starter:**
- Your application needs to connect to **multiple Redis instances/clusters**
- You want **YAML-based serializer configuration** instead of code
- You're migrating from official starter and want **zero-config compatibility**

**When to stick with official starter:**
- Your application only needs **one Redis instance/cluster**

## FAQ

### Q: Do I need to change my configuration when switching from official starter?

**No.** This starter is fully compatible with the official Spring Boot Redis configuration format.

Your existing `spring.data.redis.*` configuration will work as-is.

### Q: What's the difference between Mode 1 and Mode 2?

|                     | Mode 1 (Builder)         | Mode 2 (Auto-register)       |
|---------------------|--------------------------|------------------------------|
| **Control**         | Code-controlled          | YAML-controlled              |
| **When to use**     | Need custom bean logic   | Zero-code preference         |
| **Activation**      | No Redis config detected | Redis config auto-detected   |
| **Bean naming**     | You decide               | `{clusterName}RedisTemplate` |
| **`@RedisCluster`** | ✅ Supported              | ❌ Not available              |

### Q: Can I use both modes at the same time?

**No.** The two modes are mutually exclusive. If Redis configuration is detected, Auto-register mode activates and Builder mode is disabled.

### Q: What happens if I have both official format and multi-cluster format?

**Both are used!** The official format is added as a `default` cluster, coexisting with your multi-cluster configuration:

```yaml
spring:
  data:
    redis:
      host: localhost          # → defaultRedisTemplate
      port: 6379
      clusters:
        order:                 # → orderRedisTemplate
          host: localhost
          port: 6380
```

This allows gradual migration: keep your existing official config while adding new clusters.

### Q: How do I inject a specific cluster's template?

**Mode 1:** Use `@RedisCluster("order")` annotation or inject `RedisTemplateBuilder`.

**Mode 2:** Use `@Qualifier("orderRedisTemplate")` or constructor injection with matching parameter name:

```java
// Constructor injection with matching name
public MyService(RedisTemplate<String, Object> orderRedisTemplate) {
    this.orderRedisTemplate = orderRedisTemplate;
}
```

## License

Apache License 2.0

&copy; [hongxi.org](http://hongxi.org)
