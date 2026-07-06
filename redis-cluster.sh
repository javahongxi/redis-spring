#!/bin/bash
#
# Redis Cluster local management script
# Usage: ./redis-cluster.sh start|stop|status|restart [cluster_name]
#
# Examples:
#   ./redis-cluster.sh start              # Start all clusters (cache on 7001-7006, session on 7011-7016)
#   ./redis-cluster.sh start cache        # Start cluster named 'cache' on 7001-7006
#   ./redis-cluster.sh start session      # Start cluster named 'session' on 7011-7016
#   ./redis-cluster.sh stop all           # Stop all clusters
#

BASE_DIR="/tmp/redis-cluster"

# All cluster names
ALL_CLUSTERS="cache session"

# Cluster configurations: name -> ports
get_ports() {
    local name="$1"
    if [ -z "$name" ]; then
        name="cache"
    fi
    case "$name" in
        cache)   echo "7001 7002 7003 7004 7005 7006" ;;
        session) echo "7011 7012 7013 7014 7015 7016" ;;
        *)       echo "" ;;
    esac
}

start_cluster() {
    local name="$1"
    local ports_str=$(get_ports "$name")
    local ports=($ports_str)

    if [ -z "$ports_str" ]; then
        echo "[ERROR] Unknown cluster: $name"
        echo "Available clusters: $ALL_CLUSTERS"
        return 1
    fi

    echo "=== Starting Redis Cluster '$name' on ports ${ports[*]} ==="

    # Create data directories
    for port in "${ports[@]}"; do
        mkdir -p "$BASE_DIR/$name/$port"
    done

    # Start Redis instances
    for port in "${ports[@]}"; do
        if redis-cli -p "$port" ping &>/dev/null; then
            echo "[SKIP] Redis on port $port is already running"
        else
            redis-server --port "$port" \
                --cluster-enabled yes \
                --cluster-config-file "$BASE_DIR/$name/$port/nodes.conf" \
                --daemonize yes \
                --logfile "$BASE_DIR/$name/$port/redis.log" \
                --dir "$BASE_DIR/$name/$port"
            echo "[OK] Started Redis on port $port"
        fi
    done

    # Wait for instances to be ready
    sleep 2

    # Create cluster if not already created
    if ! redis-cli -p "${ports[0]}" cluster info 2>/dev/null | grep -q "cluster_state:ok"; then
        echo "Creating Redis Cluster '$name'..."
        redis-cli --cluster create \
            "${ports[@]/#/127.0.0.1:}" \
            --cluster-replicas 1 \
            --cluster-yes
        echo "[OK] Redis Cluster '$name' created"
    else
        echo "[SKIP] Redis Cluster '$name' already exists"
    fi

    echo "[OK] Cluster '$name' is ready! Connect: redis-cli -c -p ${ports[0]}"
    echo ""
}

stop_cluster() {
    local name="$1"
    local ports_str=$(get_ports "$name")
    local ports=($ports_str)

    if [ -z "$ports_str" ]; then
        echo "[ERROR] Unknown cluster: $name"
        return 1
    fi

    echo "=== Stopping Redis Cluster '$name' ==="
    for port in "${ports[@]}"; do
        if redis-cli -p "$port" ping &>/dev/null; then
            redis-cli -p "$port" shutdown nosave
            echo "[OK] Stopped Redis on port $port"
        else
            echo "[SKIP] Redis on port $port is not running"
        fi
    done
}

status_cluster() {
    local name="$1"
    local ports_str=$(get_ports "$name")
    local ports=($ports_str)

    if [ -z "$ports_str" ]; then
        return 1
    fi

    echo "=== Cluster '$name' ==="
    local all_running=true
    for port in "${ports[@]}"; do
        if redis-cli -p "$port" ping &>/dev/null; then
            echo "  [RUNNING] port $port"
        else
            echo "  [STOPPED] port $port"
            all_running=false
        fi
    done
    if $all_running && redis-cli -p "${ports[0]}" cluster info &>/dev/null; then
        redis-cli -p "${ports[0]}" cluster info 2>/dev/null | grep -E "cluster_state|cluster_size" | sed 's/^/  /'
    fi
    echo ""
}

start() {
    local name="$1"
    if [ -z "$name" ] || [ "$name" = "all" ]; then
        for cluster_name in $ALL_CLUSTERS; do
            start_cluster "$cluster_name"
        done
    else
        start_cluster "$name"
    fi
}

stop() {
    local name="$1"
    if [ "$name" = "all" ] || [ -z "$name" ]; then
        for cluster_name in $ALL_CLUSTERS; do
            stop_cluster "$cluster_name"
        done
    else
        stop_cluster "$name"
    fi
    echo "Done."
}

status() {
    local name="$1"
    echo "=============================="
    echo "  Redis Cluster Status"
    echo "=============================="
    echo ""
    if [ -z "$name" ] || [ "$name" = "all" ]; then
        for cluster_name in $ALL_CLUSTERS; do
            status_cluster "$cluster_name"
        done
    else
        status_cluster "$name"
    fi
}

case "$1" in
    start)
        start "$2"
        ;;
    stop)
        stop "$2"
        ;;
    status)
        status "$2"
        ;;
    restart)
        stop "$2"
        sleep 1
        start "$2"
        ;;
    *)
        echo "Usage: $0 {start|stop|status|restart} [cluster_name|all]"
        echo ""
        echo "Available clusters:"
        for name in $ALL_CLUSTERS; do
            echo "  $name -> ports: $(get_ports $name)"
        done
        echo ""
        echo "Examples:"
        echo "  $0 start              # Start all clusters"
        echo "  $0 start session      # Start session cluster only"
        echo "  $0 stop all           # Stop all clusters"
        exit 1
        ;;
esac
