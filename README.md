# multi-redis-spring-boot-starter

Spring Boot Starter for connecting to multiple Redis instances/clusters from a single application. Supports **Builder pattern** and **Auto-register** mode for creating multiple `RedisTemplate` / `StringRedisTemplate` instances with different cluster configurations.

## Features

- Multiple Redis cluster configurations in a single application
- **Mode 1 - Builder pattern** (inspired by `ChatClient.Builder`): manually create `RedisTemplate` / `StringRedisTemplate` beans
- **Mode 2 - Auto-register**: automatically register beans via `ImportBeanDefinitionRegistrar`
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
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configuration

```yaml
spring:
  data:
    redis:
      auto-register: true          # set false to use Builder mode
      clusters:
        order:                     # Standalone → orderRedisTemplate
          host: localhost
          port: 6379
        user:                      # Standalone → userRedisTemplate
          host: localhost
          port: 6380
        cache:                     # Redis Cluster → cacheRedisTemplate
          cluster:
            nodes: localhost:7001,localhost:7002,localhost:7003
        session:                   # Redis Cluster → sessionRedisTemplate
          cluster:
            nodes: localhost:7011,localhost:7012,localhost:7013
```

Optional per-cluster settings: `url`, `username`, `password`, `database`, `timeout`, `connect-timeout`, `cluster.read-from` (read from replica, e.g. `REPLICA_PREFERRED`), `lettuce.pool.*`, `lettuce.cluster.refresh.*`.

### Mode 1 - Builder Pattern

Manually define `RedisTemplate` and `StringRedisTemplate` beans using `RedisTemplateBuilder`:

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

    @Bean
    public StringRedisTemplate orderStringRedisTemplate(RedisTemplateBuilder builder) {
        return builder.stringTemplate("order");
    }
}
```

### Mode 2 - Auto Register

Set `spring.data.redis.auto-register=true`, beans will be automatically registered with the naming convention:
- `{clusterName}RedisConnectionFactory` — e.g. `orderRedisConnectionFactory`
- `{clusterName}RedisTemplate` — e.g. `orderRedisTemplate`
- `{clusterName}StringRedisTemplate` — e.g. `orderStringRedisTemplate`

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

> **Note**: Spring Boot's default Redis auto-configurations (`RedisAutoConfiguration`,
> `RedisReactiveAutoConfiguration`, `RedisRepositoriesAutoConfiguration`) are automatically
> excluded by `MultiRedisAutoConfigurationImportFilter` when this starter is on the classpath.

## Sample

The `multi-redis-spring-boot-sample` module demonstrates both standalone and Redis Cluster usage with 4 Redis targets:

| Name    | Mode       | Connection            |
|---------|------------|-----------------------|
| order   | Standalone | localhost:6379        |
| user    | Standalone | localhost:6380        |
| cache   | Cluster    | localhost:7001-7003   |
| session | Cluster    | localhost:7011-7013   |

### Prerequisites

#### Standalone Redis (order & user)

```bash
brew install redis

# Start default Redis on port 6379
brew services start redis

# Start second Redis instance on port 6380
redis-server --port 6380 --daemonize yes --logfile /tmp/redis-6380.log

# Stop
redis-cli -p 6380 shutdown
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
Each connection is verified in three steps:

1. **Config** — shows the expected target from connection factory configuration
2. **Server** — queries `INFO server` to prove the actual Redis instance being connected to
   - Standalone: shows `tcp_port` and `redis_version` from the server's INFO response
   - Cluster: shows each node's port extracted from node-prefixed INFO keys (e.g. `127.0.0.1:7001.tcp_port`)
3. **Read/Write** — performs a set/get/delete round-trip

```
========== Multi-Redis Sample Verification ==========
[order]   Config -> localhost:6379
[order]   Server -> tcp_port=6379, redis_version=8.0.1
[order]   Read/Write OK: set=hello-order-..., get=hello-order-...
[user]    Config -> localhost:6380
[user]    Server -> tcp_port=6380, redis_version=8.0.1
[user]    Read/Write OK: set=hello-user-..., get=hello-user-...
[cache]   Config -> CLUSTER nodes=[localhost:7001, localhost:7002, localhost:7003]
[cache]   Server -> CLUSTER nodes={127.0.0.1:7001=7001, 127.0.0.1:7002=7002, 127.0.0.1:7003=7003}, redis_version=8.0.1
[cache]   Read/Write OK: set=hello-cache-..., get=hello-cache-...
[session] Config -> CLUSTER nodes=[localhost:7011, localhost:7012, localhost:7013]
[session] Server -> CLUSTER nodes={127.0.0.1:7011=7011, 127.0.0.1:7012=7012, 127.0.0.1:7013=7013}, redis_version=8.0.1
[session] Read/Write OK: set=hello-session-..., get=hello-session-...
========== All verifications passed! ==========
```

The `Server` line comes directly from the Redis `INFO server` response, providing definitive proof that each template is connected to the correct Redis instance.

## License

Apache License 2.0

&copy; [hongxi.org](http://hongxi.org)
