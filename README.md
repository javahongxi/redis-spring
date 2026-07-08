# multi-redis-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/org.hongxi/multi-redis-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.hongxi/multi-redis-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot Starter for connecting to multiple Redis instances/clusters from a single application. Supports two modes: **Mode 1 - Builder + Annotation** (code-controlled) and **Mode 2 - Auto-register** (zero-code with YAML configuration) for creating multiple `RedisTemplate` / `StringRedisTemplate` / `ReactiveRedisTemplate` / `ReactiveStringRedisTemplate` instances with different cluster configurations.

**Auto-detection**: When using official Spring Boot Redis configuration format (`spring.data.redis.host/port` or `spring.data.redis.cluster.nodes`) or multi-cluster format (`spring.data.redis.clusters.*`), Auto-register mode is automatically activated — no need to set `auto-register: true`.

## Features

- Multiple Redis cluster configurations in a single application
- **Mode 1 - Builder + Annotation** (code-controlled):
  - Inject `RedisTemplateBuilder` to manually create `RedisTemplate`, `StringRedisTemplate`, `ReactiveRedisTemplate` and `ReactiveStringRedisTemplate` beans
  - Use `@RedisCluster("name")` annotation to inject templates directly into fields
- **Mode 2 - Auto-register** (zero-code): automatically register beans via `ImportBeanDefinitionRegistrar` with YAML serializer configuration. **Auto-activated** when Redis configuration is detected.
- Standalone and Redis Cluster mode support
- Lettuce connection pool support (commons-pool2)
- Cluster topology auto-refresh
- **Official Spring Boot Redis configuration format compatibility** — switch from official starter without changing config
- Automatic exclusion of Spring Boot's default Redis auto-configurations

## Official Format Compatibility

This starter is compatible with the official Spring Boot Redis configuration format. If you're already using the official starter in production, you can switch to this starter **without changing your configuration**:

### Standalone Mode (Official Format)

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: yourpassword
      timeout: 2s
```

This is equivalent to:

```yaml
spring:
  data:
    redis:
      clusters:
        default:
          host: localhost
          port: 6379
          password: yourpassword
          timeout: 2s
```

### Cluster Mode (Official Format)

```yaml
spring:
  data:
    redis:
      cluster:
        nodes: localhost:7001,localhost:7002,localhost:7003
        max-redirects: 3
      password: yourpassword
```

This is equivalent to:

```yaml
spring:
  data:
    redis:
      clusters:
        default:
          cluster:
            nodes: localhost:7001,localhost:7002,localhost:7003
            max-redirects: 3
          password: yourpassword
```

When using official format, the cluster is named `default`, so beans are named `defaultRedisTemplate`, `defaultStringRedisTemplate`, etc.

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>org.hongxi</groupId>
    <artifactId>multi-redis-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Configuration

**Multi-cluster format** (recommended for multiple Redis instances):

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

**Official format** (zero-config migration from Spring Boot official starter):

```yaml
spring:
  data:
    redis:
      host: localhost              # → defaultRedisTemplate
      port: 6379
```

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

Optional per-cluster settings: `url`, `username`, `password`, `database`, `timeout`, `connect-timeout`, `serializer.*` (key/value/hash-key/hash-value: `java`|`json`|`string`|`byteArray`), `cluster.read-from` (read from replica, e.g. `REPLICA_PREFERRED`), `lettuce.pool.*`, `lettuce.cluster.refresh.*`.

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
    public RedisTemplate<String, Object> userRedisTemplate(RedisTemplateBuilder builder) {
        return builder.cluster("user").build();
    }

    @Bean
    public StringRedisTemplate orderStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("order");
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> orderReactiveRedisTemplate(RedisTemplateBuilder builder) {
        return builder.reactiveTemplate("order");
    }

    @Bean
    public ReactiveStringRedisTemplate orderReactiveStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.reactiveStringTemplate("order");
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
        .serializers(RedisSerializer.json())
        .build();
}
```

#### Option B: @RedisCluster Annotation

Use `@RedisCluster("name")` annotation to inject `RedisTemplate` instances directly into fields. This is the simplest approach:

```java
import org.hongxi.redis.multi.annotation.RedisCluster;

@Service
public class OrderService {

    @RedisCluster("order")
    private RedisTemplate<String, Object> orderRedisTemplate;

    @RedisCluster("order")
    private StringRedisTemplate orderStringRedisTemplate;

    public void processOrder(String orderId) {
        orderRedisTemplate.opsForValue().set("order:" + orderId, orderData);
    }
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
- `{clusterName}ReactiveRedisTemplate` — e.g. `orderReactiveRedisTemplate`
- `{clusterName}ReactiveStringRedisTemplate` — e.g. `orderReactiveStringRedisTemplate`

No code needed, just inject directly:

```java
@RestController
public class MyController {

    private final RedisTemplate<String, Object> orderRedisTemplate;
    private final StringRedisTemplate userStringRedisTemplate;

    public MyController(RedisTemplate<String, Object> orderRedisTemplate,
                        StringRedisTemplate userStringRedisTemplate) {
        this.orderRedisTemplate = orderRedisTemplate;
        this.userStringRedisTemplate = userStringRedisTemplate;
    }
}
```

#### YAML Serializer Configuration (Auto Register Mode)

Configure serializers in YAML for auto-register mode:

```yaml
spring:
  data:
    redis:
      clusters:
        order:
          host: localhost
          port: 6379
          serializer:
            key: string      # string | json | java | byteArray
            value: json      # default: java (same as official)
            hash-key: string
            hash-value: json
```

Supported serializer types:
- `java` - `JdkSerializationRedisSerializer` (official default)
- `json` - `GenericJackson2JsonRedisSerializer`
- `string` - `StringRedisSerializer`
- `byteArray` - `ByteArrayRedisSerializer`

> **Note**: Spring Boot's default Redis auto-configurations (`RedisAutoConfiguration`,
> `RedisReactiveAutoConfiguration`, `RedisRepositoriesAutoConfiguration`) are automatically
> excluded by `MultiRedisAutoConfigurationImportFilter` when this starter is on the classpath.

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
brew install redis

# Start default Redis on port 6379
brew services start redis

# Start order Redis on port 6380
redis-server --port 6380 --daemonize yes --logfile /tmp/redis-6380.log

# Start user Redis on port 6381
redis-server --port 6381 --daemonize yes --logfile /tmp/redis-6381.log

# Stop
redis-cli -p 6380 shutdown
redis-cli -p 6381 shutdown
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

The sample is a non-web app that automatically verifies all Redis connections on startup.
Verification is split into separate runners:

**1. Connection Verification** (`ConnectionSampleRunner`)

Verifies connectivity for each Redis instance:
- **Config** — shows the expected target from connection factory configuration
- **Server** — queries `INFO server` to prove the actual Redis instance being connected to
  - Standalone: shows `tcp_port` and `redis_version`
  - Cluster: shows each node's port from node-prefixed INFO keys (e.g. `127.0.0.1:7001.tcp_port`)

**2. String Read/Write** (`StringSampleRunner`)

Performs set/get/delete round-trips with `StringRedisTemplate` for all clusters.

**3. Object Read/Write** (`ObjectSampleRunner`)

Performs set/get/delete round-trips with `RedisTemplate<String, Object>` for clusters with JSON serializer.

**4. Annotation Object** (`AnnotationObjectSampleRunner`, Builder mode only)

Demonstrates `@RedisCluster` annotation injection with `RedisTemplate`.

**5. Annotation String** (`AnnotationStringSampleRunner`, Builder mode only)

Demonstrates `@RedisCluster` annotation injection with `StringRedisTemplate`.

**6. Reactive Object** (`ReactiveObjectSampleRunner`)

Reactive read/write with `ReactiveRedisTemplate<String, Object>`.

**7. Reactive String** (`ReactiveStringSampleRunner`)

Reactive read/write with `ReactiveStringRedisTemplate`.

```
========== Multi-Redis Connection Verification ==========
(default cluster is from official format, coexists with multi-cluster format)
[default] Config -> localhost:6379
[default] Server -> tcp_port=6379, redis_version=8.0.1
[order]   Config -> localhost:6380
[order]   Server -> tcp_port=6380, redis_version=8.0.1
[user]    Config -> localhost:6381
[user]    Server -> tcp_port=6381, redis_version=8.0.1
[cache]   Config -> CLUSTER nodes=[localhost:7001, localhost:7002, localhost:7003]
[cache]   Server -> CLUSTER nodes={127.0.0.1:7001=7001, 127.0.0.1:7002=7002, 127.0.0.1:7003=7003}, redis_version=8.0.1
[session] Config -> CLUSTER nodes=[localhost:7011, localhost:7012, localhost:7013]
[session] Server -> CLUSTER nodes={127.0.0.1:7011=7011, 127.0.0.1:7012=7012, 127.0.0.1:7013=7013}, redis_version=8.0.1
========== Connection verification complete ==========

========== Multi-Redis Read/Write Verification ==========
[order]   Read/Write OK: set=User[name=order-user, age=20, ...], get=User[name=order-user, age=20, ...]
[user]    Read/Write OK: set=hello-user-..., get=hello-user-...
[cache]   Read/Write OK: set=User[name=cache-user, age=20, ...], get=User[name=cache-user, age=20, ...]
[session] Read/Write OK: set=hello-session-..., get=hello-session-...
========== All read/write verifications passed! ==========
```

The `Server` line comes directly from the Redis `INFO server` response, providing definitive proof that each template is connected to the correct Redis instance.

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

**No.** This starter is fully compatible with the official Spring Boot Redis configuration format. Just replace the dependency:

```xml
<!-- Remove -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Add -->
<dependency>
    <groupId>org.hongxi</groupId>
    <artifactId>multi-redis-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

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
