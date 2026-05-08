# TigerGraph Metrics Reference

This document provides a comprehensive reference for all TigerGraph metrics exposed by the TigerGraph Operator. These metrics are collected from TigerGraph clusters and made available to Prometheus for monitoring and alerting.

## Table of Contents

- [TigerGraph Metrics Reference](#tigergraph-metrics-reference)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [CPU Metrics](#cpu-metrics)
    - [tigergraph\_cpu\_available](#tigergraph_cpu_available)
    - [tigergraph\_cpu\_logical\_cores\_total](#tigergraph_cpu_logical_cores_total)
    - [tigergraph\_cpu\_usage](#tigergraph_cpu_usage)
  - [Memory Metrics](#memory-metrics)
    - [tigergraph\_memory\_available](#tigergraph_memory_available)
    - [tigergraph\_memory\_total](#tigergraph_memory_total)
    - [tigergraph\_memory\_usage](#tigergraph_memory_usage)
    - [tigergraph\_memory\_usage\_percent](#tigergraph_memory_usage_percent)
  - [Disk Metrics](#disk-metrics)
    - [tigergraph\_diskspace\_usage](#tigergraph_diskspace_usage)
    - [tigergraph\_diskspace\_free](#tigergraph_diskspace_free)
    - [tigergraph\_disk\_inode\_used](#tigergraph_disk_inode_used)
    - [tigergraph\_disk\_inode\_free](#tigergraph_disk_inode_free)
    - [tigergraph\_disk\_iops](#tigergraph_disk_iops)
    - [tigergraph\_disk\_io\_time](#tigergraph_disk_io_time)
    - [tigergraph\_disk\_read\_bytes / tigergraph\_disk\_write\_bytes](#tigergraph_disk_read_bytes--tigergraph_disk_write_bytes)
    - [tigergraph\_disk\_read\_count / tigergraph\_disk\_write\_count](#tigergraph_disk_read_count--tigergraph_disk_write_count)
    - [tigergraph\_disk\_read\_time / tigergraph\_disk\_write\_time](#tigergraph_disk_read_time--tigergraph_disk_write_time)
  - [Network Metrics](#network-metrics)
    - [tigergraph\_network\_connections](#tigergraph_network_connections)
    - [tigergraph\_network\_traffic](#tigergraph_network_traffic)
  - [Service Metrics](#service-metrics)
    - [tigergraph\_service\_status](#tigergraph_service_status)
  - [License Metrics](#license-metrics)
    - [tigergraph\_license\_days\_left](#tigergraph_license_days_left)
  - [Query Performance Metrics](#query-performance-metrics)
    - [tigergraph\_qps](#tigergraph_qps)
    - [tigergraph\_endpoint\_latency](#tigergraph_endpoint_latency)
    - [tigergraph\_endpoint\_completed](#tigergraph_endpoint_completed)
    - [tigergraph\_endpoint\_timeout](#tigergraph_endpoint_timeout)
  - [Metric Labels](#metric-labels)
    - [Common Labels](#common-labels)
    - [Service-Specific Labels](#service-specific-labels)
    - [Path-Specific Labels](#path-specific-labels)
  - [Prometheus Query Examples](#prometheus-query-examples)
    - [CPU Usage Queries](#cpu-usage-queries)
    - [Memory Usage Queries](#memory-usage-queries)
    - [Disk Usage Queries](#disk-usage-queries)
    - [Service Health Queries](#service-health-queries)
    - [License Monitoring Queries](#license-monitoring-queries)
    - [Performance Queries](#performance-queries)
  - [Alerting Recommendations](#alerting-recommendations)
    - [Critical Alerts](#critical-alerts)
      - [Service Health Alerts](#service-health-alerts)
      - [Resource Usage Alerts](#resource-usage-alerts)
      - [License Alerts](#license-alerts)
      - [Performance Alerts](#performance-alerts)
    - [Warning Alerts](#warning-alerts)
      - [Resource Usage Alerts(Warning)](#resource-usage-alertswarning)
      - [Service Health Alerts (Warning)](#service-health-alerts-warning)
      - [Performance Alerts (Warning)](#performance-alerts-warning)
      - [License Alerts (Warning)](#license-alerts-warning)
      - [System Alerts](#system-alerts)
      - [Service-Specific Alerts](#service-specific-alerts)
    - [Alert Configuration Best Practices](#alert-configuration-best-practices)

## Overview

TigerGraph exposes a comprehensive set of metrics that provide insights into:

- **System Resources**: CPU, memory, and disk usage
- **Service Health**: Status and availability of TigerGraph services
- **Performance**: Query latency, throughput, and endpoint statistics
- **License Management**: License expiration tracking [Starting from TigerGraph 4.3.0]
- **Network Activity**: Connection counts and traffic patterns

All metrics are prefixed with `tigergraph_` and include labels for cluster identification, namespace, host, and service information.

## CPU Metrics

### tigergraph_cpu_available

**Type**: `gauge`  
**Description**: Percentage of available CPU capacity on each node.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host (e.g., m1, m2, m3, m4)
- `namespace`: Kubernetes namespace
- `service_name`: Service name (empty for node-level metrics)

**Example**:

```promql
tigergraph_cpu_available{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name=""} 32.19993591308594
```

**Usage**: Monitor CPU availability to ensure adequate resources for workload processing.

### tigergraph_cpu_logical_cores_total

**Type**: `gauge`  
**Description**: Total number of logical CPU cores available on each node.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (empty for node-level metrics)

**Example**:

```txt
tigergraph_cpu_logical_cores_total{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name=""} 3
```

**Usage**: Track CPU capacity and resource allocation across cluster nodes.

### tigergraph_cpu_usage

**Type**: `gauge`  
**Description**: Percentage of CPU usage by component. Empty service_name indicates node-level usage.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (empty for node-level, specific service names for component-level)

**Service Names**:

- `ADMIN`: Administrative service
- `CTRL`: Control service
- `DICT`: Dictionary service
- `ETCD`: Etcd service
- `EXE`: Execution service
- `GPE`: Graph Processing Engine
- `GSE`: Graph Storage Engine
- `GSQL`: GSQL service
- `GUI`: Web interface
- `IFM`: Interface Manager
- `KAFKA`: Kafka service
- `KAFKACONN`: Kafka Connector
- `KAFKASTRM-LL`: Kafka Stream Low Latency
- `NGINX`: Nginx web server
- `RESTPP`: REST++ API service
- `ZK`: ZooKeeper service

**Example**:

```txt
tigergraph_cpu_usage{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name="RESTPP"} 6.143939018249512
```

**Usage**: Monitor CPU usage patterns across different TigerGraph services to identify performance bottlenecks.

## Memory Metrics

### tigergraph_memory_available

**Type**: `gauge`  
**Description**: Memory available in megabytes for programs to allocate. Returns -1 for unlimited memory.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (empty for node-level metrics)

**Example**:

```txt
tigergraph_memory_available{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name=""} 5563
```

**Usage**: Monitor available memory to prevent out-of-memory conditions.

### tigergraph_memory_total

**Type**: `gauge`  
**Description**: Total memory of the node in megabytes. Returns -1 for unlimited memory.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (empty for node-level metrics)

**Example**:

```txt
tigergraph_memory_total{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name=""} 8192
```

**Usage**: Track total memory capacity across cluster nodes.

### tigergraph_memory_usage

**Type**: `gauge`  
**Description**: Memory usage in megabytes by component. Empty service_name indicates node-level usage.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (see CPU metrics for service list)

**Example**:

```txt
tigergraph_memory_usage{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name="RESTPP"} 198
```

**Usage**: Monitor memory consumption patterns across TigerGraph services.

### tigergraph_memory_usage_percent

**Type**: `gauge`  
**Description**: Memory usage percentage of the node. Returns -1 for unlimited memory.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (empty for node-level metrics)

**Example**:

```txt
tigergraph_memory_usage_percent{cluster_name="test-cluster",host_id="m1",namespace="tigergraph",service_name=""} 32.082271575927734
```

**Usage**: Monitor memory utilization percentage to identify memory pressure.

## Disk Metrics

### tigergraph_diskspace_usage

**Type**: `gauge`  
**Description**: Disk space usage in megabytes by directory.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `mount_point`: Mount point of the filesystem
- `path`: Directory path
- `path_name`: Human-readable name for the path

**Path Names**:

- `Home`: Home directory
- `Gstore`: Graph storage directory
- `Kafka`: Kafka data directory
- `Log`: Log directory

**Example**:

```txt
tigergraph_diskspace_usage{cluster_name="test-cluster",host_id="m1",mount_point="/home/tigergraph/tigergraph/data",namespace="tigergraph",path="/home/tigergraph/tigergraph/data/gstore",path_name="Gstore"} 64.58203125
```

**Usage**: Monitor disk usage across different TigerGraph data directories.

### tigergraph_diskspace_free

**Type**: `gauge`  
**Description**: Free disk space in megabytes by directory.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_diskspace_free{cluster_name="test-cluster",host_id="m1",mount_point="/home/tigergraph/tigergraph/data",namespace="tigergraph",path="/home/tigergraph/tigergraph/data/gstore",path_name="Gstore"} 9746.2578125
```

**Usage**: Monitor available disk space to prevent storage exhaustion.

### tigergraph_disk_inode_used

**Type**: `gauge`  
**Description**: Disk inode usage by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_inode_used{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 154341
```

**Usage**: Monitor inode usage to prevent filesystem exhaustion.

### tigergraph_disk_inode_free

**Type**: `gauge`  
**Description**: Free disk inodes by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_inode_free{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 6.104379e+06
```

**Usage**: Monitor available inodes to prevent filesystem exhaustion.

### tigergraph_disk_iops

**Type**: `gauge`  
**Description**: Disk IOPS (Input/Output Operations Per Second) by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_iops{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 0
```

**Usage**: Monitor disk I/O performance and identify I/O bottlenecks.

### tigergraph_disk_io_time

**Type**: `gauge`  
**Description**: Total amount of time spent doing I/Os in hours by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_io_time{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 0
```

**Usage**: Monitor disk I/O time to identify performance issues.

### tigergraph_disk_read_bytes / tigergraph_disk_write_bytes

**Type**: `gauge`  
**Description**: Disk read/write bytes in megabytes by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_read_bytes{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 0
```

**Usage**: Monitor disk I/O patterns and data transfer rates.

### tigergraph_disk_read_count / tigergraph_disk_write_count

**Type**: `gauge`  
**Description**: Total number of read/write operations completed successfully by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_read_count{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 0
```

**Usage**: Monitor disk operation counts and I/O patterns.

### tigergraph_disk_read_time / tigergraph_disk_write_time

**Type**: `gauge`  
**Description**: Disk read/write time in hours by filesystem.

**Labels**: Same as `tigergraph_diskspace_usage`

**Example**:

```txt
tigergraph_disk_read_time{cluster_name="test-cluster",host_id="m1",mount_point="/",namespace="tigergraph",path="/home/tigergraph",path_name="Home"} 0
```

**Usage**: Monitor disk I/O latency and performance.

## Network Metrics

### tigergraph_network_connections

**Type**: `gauge`  
**Description**: Number of open TCP connections on each host.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace

**Example**:

```txt
tigergraph_network_connections{cluster_name="test-cluster",host_id="m1",namespace="tigergraph"} 598
```

**Usage**: Monitor network connection counts to identify connection leaks or high connection usage.

### tigergraph_network_traffic

**Type**: `gauge`  
**Description**: Network traffic in bytes since the service started.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `direction`: Traffic direction (incoming/outgoing)

**Example**:

```txt
tigergraph_network_traffic{cluster_name="test-cluster",direction="incoming",host_id="m1",namespace="tigergraph"} 1.406860466e+09
```

**Usage**: Monitor network traffic patterns and data transfer volumes.

## Service Metrics

### tigergraph_service_status

**Type**: `gauge`  
**Description**: TigerGraph service status. Empty partition or replica means no partition or replica for that service.

**Status Values**:

- `6`: Online/Running
- `9`: Warmup
- `12`: Readonly
- `15`: Starting
- `18`: Paused
- `21`: Stopping
- `24`: Offline
- `27`: Down
- `3`: Unknown

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `namespace`: Kubernetes namespace
- `partition`: Partition identifier (empty if not applicable)
- `replica`: Replica identifier (empty if not applicable)
- `service_name`: Service name (see CPU metrics for service list)

**Example**:

```txt
tigergraph_service_status{cluster_name="test-cluster",namespace="tigergraph",partition="",replica="1",service_name="RESTPP"} 6
```

**Usage**: Monitor service health and availability across the cluster.

## License Metrics

### tigergraph_license_days_left

**Type**: `gauge`  
**Description**: Days until license expiration. Any fraction of a day is omitted. Returns -1 for expired or invalid licenses.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `namespace`: Kubernetes namespace

**Example**:

```txt
tigergraph_license_days_left{cluster_name="test-cluster",namespace="tigergraph"} 11
```

**Usage**: Monitor license expiration to prevent service disruptions.

**Values**:

- Positive integer: Days remaining
- `0`: License expires today
- `-1`: License expired or invalid

## Query Performance Metrics

### tigergraph_qps

**Type**: `gauge`  
**Description**: Number of requests per second by endpoint.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `endpoint`: API endpoint path
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (typically RESTPP)

**Example**:

```txt
tigergraph_qps{cluster_name="test-cluster",endpoint="POST /restppkafkaloader/{graph_name}",host_id="m3",namespace="tigergraph",service_name="RESTPP"} 0.016663888469338417
```

**Usage**: Monitor query throughput and API performance.

### tigergraph_endpoint_latency

**Type**: `gauge`  
**Description**: Latency of completed requests in milliseconds. Includes max, min, and average latency.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `endpoint`: API endpoint path
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (typically RESTPP)
- `statistic`: Type of latency (max_latency, min_latency, average_latency)

**Example**:

```txt
tigergraph_endpoint_latency{cluster_name="test-cluster",endpoint="POST /restppkafkaloader/{graph_name}",host_id="m3",namespace="tigergraph",service_name="RESTPP",statistic="average_latency"} 1
```

**Usage**: Monitor query performance and identify slow endpoints.

### tigergraph_endpoint_completed

**Type**: `gauge`  
**Description**: Number of completed requests in the past period (configured by RESTPP.Factory.StatsIntervalSec) by endpoint.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `endpoint`: API endpoint path
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (typically RESTPP)

**Example**:

```txt
tigergraph_endpoint_completed{cluster_name="test-cluster",endpoint="POST /restppkafkaloader/{graph_name}",host_id="m3",namespace="tigergraph",service_name="RESTPP"} 1
```

**Usage**: Monitor request completion rates and API usage patterns.

### tigergraph_endpoint_timeout

**Type**: `gauge`  
**Description**: Number of requests not returning before the system-configured timeout limit.

**Labels**:

- `cluster_name`: Name of the TigerGraph cluster
- `endpoint`: API endpoint path
- `host_id`: Identifier of the host
- `namespace`: Kubernetes namespace
- `service_name`: Service name (typically RESTPP)

**Example**:

```txt
tigergraph_endpoint_timeout{cluster_name="test-cluster",endpoint="POST /restppkafkaloader/{graph_name}",host_id="m3",namespace="tigergraph",service_name="RESTPP"} 0
```

**Usage**: Monitor request timeouts to identify performance issues.

## Metric Labels

### Common Labels

All TigerGraph metrics include these common labels:

- **`cluster_name`**: Identifies the TigerGraph cluster
- **`namespace`**: Kubernetes namespace where the cluster is deployed
- **`host_id`**: Identifies the specific host/node (e.g., m1, m2, m3, m4)

### Service-Specific Labels

Metrics that track individual services include:

- **`service_name`**: Name of the TigerGraph service component
- **`partition`**: Partition identifier for distributed services
- **`replica`**: Replica identifier for replicated services

### Path-Specific Labels

Disk-related metrics include:

- **`mount_point`**: Filesystem mount point
- **`path`**: Directory path being monitored
- **`path_name`**: Human-readable name for the path

## Prometheus Query Examples

### CPU Usage Queries

```promql
# Average CPU usage across all nodes
avg(tigergraph_cpu_usage{service_name=""})

# CPU usage by specific service
tigergraph_cpu_usage{service_name="RESTPP"}

# Top 5 services by CPU usage
topk(5, tigergraph_cpu_usage{service_name!=""})
```

### Memory Usage Queries

```promql
# Memory usage percentage
tigergraph_memory_usage_percent

# Available memory in GB
tigergraph_memory_available / 1024

# Memory usage by service
tigergraph_memory_usage{service_name!=""}
```

### Disk Usage Queries

```promql
# Disk usage percentage by path
(tigergraph_diskspace_usage / (tigergraph_diskspace_usage + tigergraph_diskspace_free)) * 100

# Free disk space in GB
tigergraph_diskspace_free / 1024

# Disk usage by path name
tigergraph_diskspace_usage{path_name="Gstore"}
```

### Service Health Queries

```promql
# Services that are not online
tigergraph_service_status != 6

# Count of online services
count(tigergraph_service_status == 6)

# Service status by service name
tigergraph_service_status{service_name="RESTPP"}
```

### License Monitoring Queries

```promql
# License days remaining
tigergraph_license_days_left

# License expiring soon (less than 30 days)
tigergraph_license_days_left < 30 and tigergraph_license_days_left > 0

# License expired or invalid
tigergraph_license_days_left <= 0
```

### Performance Queries

```promql
# Average query latency
avg(tigergraph_endpoint_latency{statistic="average_latency"})

# High latency queries (> 1000ms)
tigergraph_endpoint_latency{statistic="average_latency"} > 1000

# Request rate per second
rate(tigergraph_endpoint_completed[5m])
```

## Alerting Recommendations

This section provides comprehensive alerting recommendations based on the predefined Prometheus rules in the TigerGraph Operator. These alerts are designed to help you maintain optimal performance and reliability of your TigerGraph clusters.

### Critical Alerts

#### Service Health Alerts

1. **TigerGraph Service Down**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 27
   ```

   - **Severity**: Critical
   - **Duration**: 1m
   - **Description**: Service is completely down and not responding

2. **TigerGraph Service Offline**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 24
   ```

   - **Severity**: Critical
   - **Duration**: 1m
   - **Description**: Service is offline and not available

3. **TigerGraph Service Stopping**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 21
   ```

   - **Severity**: Critical
   - **Duration**: 1m
   - **Description**: Service is in the process of stopping

4. **TigerGraph Service Paused**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 18
   ```

   - **Severity**: Critical
   - **Duration**: 1m
   - **Description**: Service is paused and not processing requests

5. **TigerGraph Service Unknown Status**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 3
   ```

   - **Severity**: Critical
   - **Duration**: 1m
   - **Description**: Service status is unknown or undetermined

#### Resource Usage Alerts

1. **Critical CPU Usage**:

   ```promql
   max(tigergraph_cpu_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) > 90
   ```

   - **Severity**: Critical
   - **Duration**: 3m
   - **Description**: CPU usage is critically high (>90%)

2. **Critical Memory Usage**:

   ```promql
   (max(tigergraph_memory_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) / max(tigergraph_memory_total{service_name=""}) by (cluster_name, namespace, service_name, host_id)) * 100 > 90
   ```

   - **Severity**: Critical
   - **Duration**: 3m
   - **Description**: Memory usage is critically high (>90%)

3. **Critical Disk Usage**:

   ```promql
   (max by(host_id, mount_point, path)(tigergraph_diskspace_usage) / (max by(host_id, mount_point, path)(tigergraph_diskspace_usage) + max by(host_id, mount_point, path) (tigergraph_diskspace_free))) * 100 > 90
   ```

   - **Severity**: Critical
   - **Duration**: 3m
   - **Description**: Disk usage is critically high (>90%)

#### License Alerts

1. **TigerGraph License Expiring Critical**:

   ```promql
   min(tigergraph_license_days_left) by (cluster_name, namespace) <= 7 and min(tigergraph_license_days_left) by (cluster_name, namespace) > 0
   ```

   - **Severity**: Critical
   - **Duration**: 1m
   - **Description**: License will expire in 7 days or less

2. **TigerGraph License Expired**:

    ```promql
    min(tigergraph_license_days_left) by (cluster_name, namespace) == 0
    ```

    - **Severity**: Critical
    - **Duration**: 1m
    - **Description**: License has expired today

3. **TigerGraph License Invalid**:

    ```promql
    min(tigergraph_license_days_left) by (cluster_name, namespace) == -1
    ```

    - **Severity**: Critical
    - **Duration**: 1m
    - **Description**: License is invalid or corrupted

#### Performance Alerts

1. **Critical Endpoint Latency**:

    ```promql
    max(tigergraph_endpoint_latency{statistic="average_latency"}) by (namespace,cluster_name, endpoint, exported_endpoint, service_name) > 10000
    ```

    - **Severity**: Critical
    - **Duration**: 3m
    - **Description**: Average endpoint latency is critically high (>10 seconds)

### Warning Alerts

#### Resource Usage Alerts(Warning)

1. **High CPU Usage**:

   ```promql
   max(tigergraph_cpu_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) > 80
   ```

   - **Severity**: Warning
   - **Duration**: 5m
   - **Description**: CPU usage is high (>80%)

2. **High Memory Usage**:

   ```promql
   (max(tigergraph_memory_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) / max(tigergraph_memory_total{service_name=""}) by (cluster_name, namespace, service_name, host_id)) * 100 > 80
   ```

   - **Severity**: Warning
   - **Duration**: 5m
   - **Description**: Memory usage is high (>80%)

3. **Low Memory Available**:

   ```promql
   max(tigergraph_memory_available{}) by (cluster_name, namespace, host_id) < 1000
   ```

   - **Severity**: Warning
   - **Duration**: 5m
   - **Description**: Available memory is low (<1GB)

4. **High Disk Usage**:

   ```promql
   (max by(host_id, mount_point, path)(tigergraph_diskspace_usage) / (max by(host_id, mount_point, path)(tigergraph_diskspace_usage) + max by(host_id, mount_point, path) (tigergraph_diskspace_free))) * 100 > 80
   ```

   - **Severity**: Warning
   - **Duration**: 5m
   - **Description**: Disk usage is high (>80%)

5. **Low Disk Space**:

   ```promql
   max(tigergraph_diskspace_free) by (cluster_name, namespace, path_name, host_id, path) < 1000
   ```

   - **Severity**: Warning
   - **Duration**: 5m
   - **Description**: Free disk space is low (<1GB)

6. **Low Disk Inodes**:

   ```promql
   max(tigergraph_disk_inode_free) by (cluster_name, namespace, host_id, path_name, path) < 100000
   ```

   - **Severity**: Warning
   - **Duration**: 5m
   - **Description**: Free disk inodes are low (<100,000)

#### Service Health Alerts (Warning)

1. **TigerGraph Service Starting**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 15
   ```

   - **Severity**: Warning
   - **Duration**: 2m
   - **Description**: Service is starting up

2. **TigerGraph Service Readonly**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 12
   ```

   - **Severity**: Warning
   - **Duration**: 1m
   - **Description**: Service is in readonly mode

3. **TigerGraph Service Warmup**:

   ```promql
   max(tigergraph_service_status) by (cluster_name, namespace, service_name, host_id) == 9
   ```

   - **Severity**: Warning
   - **Duration**: 2m
   - **Description**: Service is in warmup state

#### Performance Alerts (Warning)

1. **High Endpoint Latency**:

    ```promql
    max(tigergraph_endpoint_latency{statistic="average_latency"}) by (namespace,cluster_name, endpoint, exported_endpoint, service_name) > 5000
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: Average endpoint latency is high (>5 seconds)

2. **High QPS**:

    ```promql
    max(tigergraph_qps) by (namespace,cluster_name, endpoint, exported_endpoint, service_name, host_id) > 100
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: High queries per second detected

3. **Endpoint Timeout**:

    ```promql
    max(tigergraph_endpoint_timeout) by (namespace,cluster_name, endpoint, exported_endpoint, service_name, host_id) > 0
    ```

    - **Severity**: Warning
    - **Duration**: 1m
    - **Description**: Endpoint timeouts detected

#### License Alerts (Warning)

1. **TigerGraph License Expiring Soon**:

    ```promql
    min(tigergraph_license_days_left) by (cluster_name, namespace) <= 30 and min(tigergraph_license_days_left) by (cluster_name, namespace) > 7
    ```

    - **Severity**: Warning
    - **Duration**: 1m
    - **Description**: License will expire in 30 days or less

#### System Alerts

1. **Low CPU Available**:

    ```promql
    max(tigergraph_cpu_available) by (namespace,cluster_name, host_id) < 10
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: Available CPU is low (<10%)

2. **High Network Connections**:

    ```promql
    max(tigergraph_network_connections) by (cluster_name, namespace, host_id) > 2000
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: Number of open TCP connections is high

3. **High Disk I/O**:

    ```promql
    max(tigergraph_disk_iops) by (namespace,cluster_name, path_name, host_id, path, mount_point) > 1000
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: High disk I/O operations per second

4. **High Disk I/O Time**:

    ```promql
    max(tigergraph_disk_io_time) by (namespace,cluster_name, path_name, host_id, path,mount_point) > 0.1
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: High disk I/O time (>0.1 hours)

#### Service-Specific Alerts

1. **TigerGraph Service High CPU Usage**:

    ```promql
    max(tigergraph_cpu_usage{service_name!=""}) by (cluster_name, namespace, service_name, host_id) > 70
    ```

    - **Severity**: Warning
    - **Duration**: 5m
    - **Description**: High CPU usage for specific TigerGraph service

### Alert Configuration Best Practices

1. **Alert Grouping**: Group related alerts by service type (CPU, Memory, Disk, etc.) for better organization.

2. **Alert Severity Levels**:
   - **Critical**: Immediate action required (service down, license expired)
   - **Warning**: Attention needed but not immediately critical

3. **Alert Duration**: Use appropriate durations to avoid false positives:
   - **1m**: For critical service status changes
   - **3m**: For critical resource usage
   - **5m**: For warning conditions

4. **Alert Labels**: Include relevant labels in alert descriptions:
   - `cluster_name`: Identify which cluster is affected
   - `namespace`: Identify the Kubernetes namespace
   - `host_id`: Identify the specific host
   - `service_name`: Identify the specific service (when applicable)

5. **Alert Annotations**: Provide clear, actionable descriptions:
   - Include current values in descriptions
   - Specify which host/cluster is affected
   - Provide context about the impact

6. **Recording Rules**: Use the predefined recording rules for complex calculations:
   - `tigergraph:cpu_usage_percentage`
   - `tigergraph:memory_usage_percentage`
   - `tigergraph:disk_usage_percentage`
   - `tigergraph:endpoint_latency_avg`
   - `tigergraph:service_online_count`

This comprehensive alerting strategy ensures proactive monitoring and helps maintain optimal performance and reliability of your TigerGraph clusters.
