# TigerGraph Operator Monitoring Guide

Since version 1.5.0, the TigerGraph Operator includes support for monitoring. This guide will help you deploy Prometheus and Grafana for observability monitoring, and configure the TigerGraph Monitor CR to automatically set up Grafana dashboards.

Starting from version 1.7.0, TigerGraph Operator supports automatically exposing Operator metrics to Prometheus when the monitoring option is enabled during installation. Additionally, it supports customizing PrometheusRules and AlertManagerConfig through the TigerGraph Monitor CR, ServiceMonitor, PrometheusRule, and AlertManagerConfig selectors.

## Table of Contents

- [TigerGraph Operator Monitoring Guide](#tigergraph-operator-monitoring-guide)
  - [Table of Contents](#table-of-contents)
  - [Install Monitoring Components](#install-monitoring-components)
    - [Install Prometheus, Grafana and AlertManager](#install-prometheus-grafana-and-alertmanager)
      - [Basic Installation](#basic-installation)
      - [Custom Configuration Installation](#custom-configuration-installation)
    - [Verify Installation](#verify-installation)
  - [Expose TigerGraph Operator Metrics to Prometheus](#expose-tigergraph-operator-metrics-to-prometheus)
    - [Enable Operator Metrics During Installation](#enable-operator-metrics-during-installation)
    - [Enable Operator Metrics During Upgrade](#enable-operator-metrics-during-upgrade)
    - [Verify Operator Metrics Exposure](#verify-operator-metrics-exposure)
    - [Grafana Dashboard and Prometheus Alert Rules for TigerGraph Operator​](#grafana-dashboard-and-prometheus-alert-rules-for-tigergraph-operator)
  - [Manage TigerGraph Monitor](#manage-tigergraph-monitor)
    - [Key Configuration Fields](#key-configuration-fields)
    - [Manage TigerGraph Monitor using kubectl-tg plugin](#manage-tigergraph-monitor-using-kubectl-tg-plugin)
      - [Basic Monitoring Configuration](#basic-monitoring-configuration)
      - [Advanced Monitoring Configuration](#advanced-monitoring-configuration)
    - [Manage TigerGraph Monitor using CR](#manage-tigergraph-monitor-using-cr)
      - [Basic Configuration](#basic-configuration)
      - [Complete Configuration Example](#complete-configuration-example)
  - [Advanced Configuration](#advanced-configuration)
    - [Service Monitor Labels](#service-monitor-labels)
    - [Prometheus Rules Selector](#prometheus-rules-selector)
    - [Prometheus Rules](#prometheus-rules)
    - [AlertManager Config Selector](#alertmanager-config-selector)
    - [AlertManager Configuration](#alertmanager-configuration)
    - [TLS Configuration](#tls-configuration)
  - [Access Monitoring Interface](#access-monitoring-interface)
  - [Troubleshooting](#troubleshooting)
    - [Create TigerGraph monitor CR successfully but with warning events in TigerGraph monitor CR status](#create-tigergraph-monitor-cr-successfully-but-with-warning-events-in-tigergraph-monitor-cr-status)
    - [Check the serviceMonitorSelector, ruleSelector, and alertmanagerConfigSelector](#check-the-servicemonitorselector-ruleselector-and-alertmanagerconfigselector)
  - [TigerGraph Metrics Reference](#tigergraph-metrics-reference)
  - [Uninstall Monitoring Components](#uninstall-monitoring-components)
    - [Uninstall Prometheus and Grafana](#uninstall-prometheus-and-grafana)
    - [Clean Up CRDs](#clean-up-crds)
    - [Clean Up Persistent Storage](#clean-up-persistent-storage)

## Install Monitoring Components

### Install Prometheus, Grafana and AlertManager

#### Basic Installation

```bash
# Set variables
export MONITORING_NAMESPACE="monitoring-stack"
export RELEASE_NAME="monitoring-stack"

# Create namespace
kubectl create namespace $MONITORING_NAMESPACE

# Install using default configuration
kubectl tg monitoring-stack create \
  --kube-prometheus-stack-release-name $RELEASE_NAME \
  --namespace $MONITORING_NAMESPACE
```

#### Custom Configuration Installation

```bash
# Create custom values file
cat > monitoring-values.yaml << EOF
prometheus:
  enabled: true
  service:
    type: LoadBalancer
    port: 9090
    targetPort: 9090
    portName: prometheus-service
  prometheusSpec:    
    storageSpec: 
      volumeClaimTemplate:
        spec:
          accessModes:
            - "ReadWriteOnce"
          resources:
            requests:
              storage: 50Gi
    serviceMonitorSelector:
      ## Example which selects ServiceMonitors with label "prometheus" set to "somelabel"
      matchLabels:
        prometheus.io/monitor: "true"
    ruleSelector:
      matchLabels:
        prometheus.io/rule: "true"

grafana:
  enabled: true
  adminPassword: admin
  service:
    type: LoadBalancer
    port: 8081
    targetPort: 3000
    portName: grafana-service
  persistence:
    enabled: true
    type: pvc
    accessModes:
      - ReadWriteOnce
    size: 5Gi

alertmanager:
  enabled: true
  service:
    type: LoadBalancer
    port: 9093
    targetPort: 9093
  alertmanagerSpec:
    storage:
      volumeClaimTemplate:
        spec:
          accessModes:
            - "ReadWriteOnce"
          resources:
            requests:
              storage: 5Gi
    alertmanagerConfigSelector:
      matchLabels:
        prometheus.io/alertmanager-config: "true"
EOF

# Install with custom configuration
kubectl tg monitoring-stack create \
  --kube-prometheus-stack-release-name $RELEASE_NAME \
  --kube-prometheus-stack-values monitoring-values.yaml \
  --namespace $MONITORING_NAMESPACE
```

### Verify Installation

```bash
# Check if all pods are running
kubectl get pods -n $MONITORING_NAMESPACE

# Check services
kubectl get svc -n $MONITORING_NAMESPACE

# Verify CRDs are installed
kubectl get crd | grep monitoring.coreos.com
```

## Expose TigerGraph Operator Metrics to Prometheus

Starting from version 1.7.0, you can enable TigerGraph Operator metrics exposure to Prometheus during Operator installation. This allows you to monitor the Operator itself alongside your TigerGraph clusters.

> [!IMPORTANT]
> When enabling monitoring during Operator installation, you must specify the ServiceMonitor selector; otherwise, Prometheus will not detect the ServiceMonitor resources created by the Operator. For guidance on checking Prometheus’ ServiceMonitor selector, see the [Troubleshooting section](#check-the-servicemonitorselector-ruleselector-and-alertmanagerconfigselector).

### Enable Operator Metrics During Installation

```bash
# Install Operator with monitoring enabled
kubectl tg init \
  --namespace tigergraph \
  --monitoring-enabled true \
  --monitoring-service-monitor-selector "prometheus.io/monitor=true"
```

### Enable Operator Metrics During Upgrade

```bash
# Upgrade existing Operator with monitoring enabled
kubectl tg upgrade \
  --namespace tigergraph \
  --monitoring-enabled true \
  --monitoring-service-monitor-selector "prometheus.io/monitor=true"
```

### Verify Operator Metrics Exposure

After enabling Operator metrics, verify that the Service Monitor is created:

```bash
# Check if Service Monitor is created
kubectl get servicemonitor -n tigergraph

# Check Service Monitor details
kubectl describe servicemonitor tigergraph-operator-controller-manager-metrics-monitor -n tigergraph

# Verify metrics endpoint status in Prometheus
# query the service address of Prometheus
kubectl get svc -n $MONITORING_NAMESPACE
# Then visit http://${PROMETHEUS_LBS_ADDRESS}:9090/targets
```

### Grafana Dashboard and Prometheus Alert Rules for TigerGraph Operator​

When you enable monitoring during installation, the TigerGraph Operator automatically deploys a default Grafana dashboard named `​TigerGraph Operator Metrics Dashboard`.

While Prometheus alert rules are not applied automatically during installation, we provide a default set of rules for reference. You can customize the following example configuration to suit your requirements:

[TigerGraph-Operator-alert-rules](../10-samples/monitoring/tigergraph-operator-alert-rules.yaml)

## Manage TigerGraph Monitor

### Key Configuration Fields

- monitoredClusters: Specify the clusters you want to monitor. If left empty, all clusters created in the current namespace will be monitored.

- serviceMonitorLabels: Define the selector labels for the ServiceMonitor. If not specified, the TigerGraph Operator will attempt to detect them automatically.

- tlsConfig: TLS configuration to use when scraping the target.

- ruleSelectorLabels: Define the selector labels for the PrometheusRule. If not specified, the TigerGraph Operator will attempt to detect them automatically.

- alertmanagerConfigLabels: Define the selector labels for the AlertmanagerConfig. If not specified, the TigerGraph Operator will attempt to detect them automatically.

- releaseName: Deprecated as of version 1.7.0. Use serviceMonitorLabels instead.

- prometheusRule: PrometheusRule contains specification parameters for a Rule.

- alertmanagerConfig: AlertmanagerConfig is a specification of the desired behavior of the Alertmanager configuration.

For detailed configuration of these sub-fields, please see the [API Reference](../08-reference/api-reference.md).

### Manage TigerGraph Monitor using kubectl-tg plugin

You can manage a TigerGraph Monitor CR by subcommand kubectl tg monitor of kubectl-tg plugin.

```bash
$ kubectl tg monitor --help
Manage TigerGraph monitor
 
Examples:
  # create a TigerGraph monitor with name and namespace
  kubectl tg monitor create --name tigergraph-monitor -n tigergraph
  # create a TigerGraph monitor with name and namespace and service monitor labels
  kubectl tg monitor create --name tigergraph-monitor -n tigergraph --service-monitor-labels prometheus.io/monitor=true,prometheus.io/scrape=true
  # create a TigerGraph monitor with TLS configuration
  kubectl tg monitor create --name tigergraph-monitor -n tigergraph --tls-config tls-config.yaml
  # update a TigerGraph monitor
  kubectl tg monitor update --name tigergraph-monitor -r new-release-name -n tigergraph
  # delete a TigerGraph monitor with name and namespace
  kubectl tg monitor delete --name tigergraph-monitor -n tigergraph

Usage:
  kubectl tg monitor [create|update|delete] [OPTIONS]

Options:
  -n|--namespace :    set namespace, if not set, use the default namespace in context
  --name :            specify name of monitor
  --monitored-clusters: 
                      specify the clusters to be monitored, if not set, monitor all clusters. Separate multiple clusters with commas, e.g. cluster1,cluster2. Set it to null if you want to remove it.
  -r|--kube-prometheus-stack-release-name : 
                      specify release name of kube-prometheus-stack deployment. Deprecated, please use --service-monitor-labels instead.
  --service-monitor-labels :
                      specify the labels of service monitor, your input should be like 'prometheus.io/monitor=true,prometheus.io/scrape=true'.
                      Set it to null if you want to empty the labels.
  --prometheus-rule-labels :
                      specify the labels of prometheus rule, your input should be like 'prometheus.io/monitor=true,prometheus.io/scrape=true'.
                      Set it to null if you want to empty the labels.
  --prometheus-rule:
                      give a YAML file to specify the prometheus rules.
  --alertmanager-config-labels :
                      specify the labels of alertmanager config, your input should be like 'prometheus.io/monitor=true,prometheus.io/scrape=true'.
                      Set it to null if you want to empty the labels.
  --alertmanager-config:
                      give a YAML file to specify the alertmanager configs.
  --tls-config:
                      give a YAML file to specify the TLS configuration for ServiceMonitor endpoints.
```

#### Basic Monitoring Configuration

```bash
export TG_MONITOR_NAME=tigergraph-monitor
export namespace=tigergraph
# Create basic monitoring
kubectl tg monitor create \
  --name ${TG_MONITOR_NAME} \
  --namespace ${NAMESPACE} \
  --service-monitor-labels "prometheus.io/monitor=true"
```

#### Advanced Monitoring Configuration

Prepare a YAML file that includes the definitions for your `prometheusRule`, `alertmanagerConfig` and `tlsConfig`. Those YAML files will be passed to the `--prometheus-rule`, `--alertmanager-config` and `--tls-config` options.

Below is an illustrative example of a prometheusRule, alertmanagerConfig and tlsConfig YAML files:

[prometheus-rules.yaml](../10-samples/monitoring/prometheus-rules.yaml)

```yaml
prometheusRule:
  groups:
  - name: tigergraph.cpu.alerts
    rules:
      - alert: TigerGraphHighCPUUsage
        expr:  max(tigergraph_cpu_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) > 80
        for: 5m
        labels:
          severity: warning
          service: tigergraph
        annotations:
          summary: "High CPU Usage Detected"
          description: "CPU usage on host {{ $labels.host_id }} of cluster {{ $labels.cluster_name }} in namespace {{ $labels.namespace }} is high ({{ $value }}%)"

      - alert: TigerGraphCriticalCPUUsage
        expr: max(tigergraph_cpu_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) > 90
        for: 3m
        labels:
          severity: critical
          service: tigergraph
        annotations:
          summary: "Critical CPU Usage Detected"
          description: "CPU usage on host {{ $labels.host_id }} of cluster {{ $labels.cluster_name }} in namespace {{ $labels.namespace }} is critically high ({{ $value }}%)"
```

alertmanager-configs.yaml

```yaml
alertmanagerConfig:
  route:
    groupBy: ["job", "alertname"]
    groupWait: 30s
    groupInterval: 5m
    repeatInterval: 1m
    receiver: "slack-receiver"
    routes:
      - receiver: "slack-receiver"
        continue: true
  receivers:
    - name: "slack-receiver"
      slackConfigs:
        - sendResolved: true
          apiURL:
            name: slack-webhook-url
            key: webhook-url
          channel: "#operator-monitoring-test"
          color: '{{ if eq .Status "firing" }}danger{{ else }}good{{ end }}'
          text: |-
            {{ range .Alerts }}
              *Alert:* {{ .Annotations.summary }} - `{{ .Labels.severity }}`
              *Description:* {{ .Annotations.description }}
              *Details:*
              {{ range .Labels.SortedPairs }} • *{{ .Name }}:* `{{ .Value }}`
              {{ end }}
            {{ end }}
```

tls-config.yaml

```yaml
tlsConfig:
  ca:
    secret:
      name: tigergraph-metrics-server-cert
      key: ca.crt
  cert:
    secret:
      name: tigergraph-metrics-server-cert
      key: tls.crt
  keySecret:
    name: tigergraph-metrics-server-cert
    key: tls.key
  insecureSkipVerify: false
```

> [!IMPORTANT]
> When configuring alertmanagerConfig, you must also ​manually create a Kubernetes Secret​ to enable authentication for external notification services such as ​Email​ or ​Slack.
> 
> This Secret typically contains sensitive credentials (e.g., API tokens, SMTP credentials) required for Alertmanager to send notifications to these services. Without the Secret, Alertmanager won't be able to establish connections to external providers.

Secret for Email:

```YAML
apiVersion: v1
kind: Secret
metadata:
  name: alertmanager-auth-secret  # Name of the Secret
type: Opaque
data:
  password: YOUR_PASSWORD_BASE64_ENCODED
```

Secret for Slack:

```YAML
apiVersion: v1
kind: Secret
metadata:
  name: slack-webhook-url
type: Opaque
stringData:
  webhook-url: https://hooks.slack.com/services/YOUR_SLACK_WEBHOOK_URL
```

Create TigerGraph monitor CR using kubectl tg plugin:

```bash
kubectl tg monitor create \
  --name ${TG_MONITOR_NAME} \
  --namespace ${NAMESPACE} \
  --monitored-clusters "cluster1,cluster2" \
  --service-monitor-labels "prometheus.io/monitor=true" \
  --prometheus-rule-labels "prometheus.io/rules=true" \
  --prometheus-rule prometheus-rules.yaml \
  --alertmanager-config-labels "alertmanager.io/config=true" \
  --alertmanager-config alertmanager-config.yaml \
  --tls-config tls-config.yaml
```

When the TigerGraph Monitor CR is successfully created, it automatically configures Prometheus to scrape the TigerGraph metrics endpoints specified in the `ServiceMonitor` CR. Additionally, it sets up a default Grafana dashboard based on the configuration defined in a `ConfigMap`.

> [!NOTE]
> By default, the TigerGraph Monitor CR monitors all TigerGraph clusters in the current namespace. To monitor specific TigerGraph clusters, specify them by the option `--monitored-clusters`.

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the options `--service-monitor-labels`, `--prometheus-rule-labels` and `--alertmanager-config`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

Update TigerGraph monitor CR using kubectl tg plugin:

```bash
kubectl tg monitor update \
  --name ${TG_MONITOR_NAME} \
  --namespace ${NAMESPACE} \
  --monitored-clusters "cluster1,cluster2" \
  --service-monitor-labels "prometheus.io/monitor=true" \
  --prometheus-rule-labels "prometheus.io/rules=true" \
  --prometheus-rule prometheus-rules.yaml \
  --alertmanager-config-labels "alertmanager.io/config=true" \
  --alertmanager-config alertmanager-config.yaml \
  --tls-config tls-config.yaml
```

Delete a TigerGraph monitor:

```bash
kubectl tg monitor delete --name ${TG_MONITOR_NAME} -n ${NAMESPACE}
```

### Manage TigerGraph Monitor using CR

#### Basic Configuration

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphMonitor
metadata:
  name: tigergraph-monitor
  namespace: tigergraph
spec:
  monitoredClusters:
  - test-cluster1
  - test-cluster2
  serviceMonitorLabels:
    prometheus.io/monitor: "true"
```

> [!NOTE]
> By default, the TigerGraph Monitor CR monitors all TigerGraph clusters in the current namespace. To monitor specific TigerGraph clusters, specify them by the option `--monitored-clusters`.

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the option `--service-monitor-labels`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

#### Complete Configuration Example

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphMonitor
metadata:
  name: tigergraph-monitor
  namespace: tigergraph
spec:
  # Specify clusters to monitor (optional, monitors all if not specified)
  monitoredClusters:
  - test-cluster1
  - test-cluster2
  
  # Service Monitor labels for Prometheus discovery
  serviceMonitorLabels:
    prometheus.io/monitor: "true"
    prometheus.io/scrape: "true"
    app.kubernetes.io/component: "monitoring"
  
  # Prometheus Rule labels
  ruleSelectorLabels:
    prometheus.io/rules: "true"
    app.kubernetes.io/component: "monitoring"
  
  # Prometheus Rules configuration
  prometheusRule:
    groups:
    - name: tigergraph.cpu.alerts
      rules:
        - alert: TigerGraphHighCPUUsage
          expr:  max(tigergraph_cpu_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) > 80
          for: 5m
          labels:
            severity: warning
            service: tigergraph
          annotations:
            summary: "High CPU Usage Detected"
            description: "CPU usage on host {{ $labels.host_id }} of cluster {{ $labels.cluster_name }} in namespace {{ $labels.namespace }} is high ({{ $value }}%)"

        - alert: TigerGraphCriticalCPUUsage
          expr: max(tigergraph_cpu_usage{service_name=""}) by (cluster_name, namespace, service_name, host_id) > 90
          for: 3m
          labels:
            severity: critical
            service: tigergraph
          annotations:
            summary: "Critical CPU Usage Detected"
            description: "CPU usage on host {{ $labels.host_id }} of cluster {{ $labels.cluster_name }} in namespace {{ $labels.namespace }} is critically high ({{ $value }}%)"

  
  # Alertmanager Configuration labels
  alertmanagerConfigLabels:
    alertmanager.io/config: "true"
    app.kubernetes.io/component: "monitoring"
  
  # Alertmanager Configuration
  alertmanagerConfig:
    route:
      groupBy: ["job", "alertname"]
      groupWait: 30s
      groupInterval: 5m
      repeatInterval: 1m
      receiver: "slack-receiver"
      routes:
        - receiver: "slack-receiver"
          continue: true
    receivers:
      - name: "slack-receiver"
        slackConfigs:
          - sendResolved: true
            apiURL:
              name: slack-webhook-url
              key: webhook-url
            channel: "#operator-monitoring-test"
            color: '{{ if eq .Status "firing" }}danger{{ else }}good{{ end }}'
            text: |-
              {{ range .Alerts }}
                *Alert:* {{ .Annotations.summary }} - `{{ .Labels.severity }}`
                *Description:* {{ .Annotations.description }}
                *Details:*
                {{ range .Labels.SortedPairs }} • *{{ .Name }}:* `{{ .Value }}`
                {{ end }}
              {{ end }}
  
  # TLS Configuration for ServiceMonitor endpoints
  tlsConfig:
    ca:
      secret:
        name: tigergraph-metrics-server-cert
        key: ca.crt
    cert:
      secret:
        name: tigergraph-metrics-server-cert
        key: tls.crt
    keySecret:
      name: tigergraph-metrics-server-cert
      key: tls.key
    insecureSkipVerify: false
```

## Advanced Configuration

### Service Monitor Labels

Service Monitor labels are used by Prometheus to discover and scrape metrics from TigerGraph clusters.

```yaml
serviceMonitorLabels:
  prometheus.io/monitor: "true"
  prometheus.io/scrape: "true"
  app.kubernetes.io/component: "monitoring"
```

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the field `serviceMonitorLabels`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

### Prometheus Rules Selector

Prometheus Rules Selector Labels: Define the selector labels for the PrometheusRule. If not specified, the TigerGraph Operator will attempt to detect them automatically.

```yaml
ruleSelectorLabels:
  prometheus.io/rules: "true"
  app.kubernetes.io/component: "monitoring"
```

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the field `ruleSelectorLabels`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

### Prometheus Rules

Prometheus rules define alerting and recording rules for monitoring TigerGraph clusters.

```yaml
prometheusRule:
  groups:
  - name: tigergraph.rules
    rules:
    # CPU usage alert
    - alert: TigerGraphHighCPUUsage
      expr: tigergraph_cpu_usage > 80
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High CPU usage detected"
        description: "TigerGraph cluster {{ $labels.cluster }} has high CPU usage"
    
    # Memory usage alert
    - alert: TigerGraphHighMemoryUsage
      expr: tigergraph_memory_usage > 85
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High memory usage detected"
        description: "TigerGraph cluster {{ $labels.cluster }} has high memory usage"
    
    # Query latency alert
    - alert: TigerGraphHighQueryLatency
      expr: tigergraph_query_latency_seconds > 10
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "High query latency detected"
        description: "TigerGraph cluster {{ $labels.cluster }} has high query latency"
```

A default set of Prometheus alerting rules is provided based on key TigerGraph metrics. You can customize this configuration to meet your specific requirements.

[TigerGraph-Prometheus-alert-rules-example](../10-samples/monitoring/tigergraph-alert-rules.yaml)

### AlertManager Config Selector

AlertManager Config Selector Labels: Define the selector labels for the AlertManager Config. If not specified, the TigerGraph Operator will attempt to detect them automatically.

```yaml
alertmanagerConfigLabels:
  alertmanager.io/config: "true"
  app.kubernetes.io/component: "monitoring"
```

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the field `alertmanagerConfigLabels`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

### AlertManager Configuration

AlertManager configuration defines how alerts are routed and sent.

```yaml
alertmanagerConfig:
  route:
    groupBy: ["job", "alertname"]
    groupWait: 30s
    groupInterval: 5m
    repeatInterval: 1m
    receiver: "slack-receiver"
    routes:
      - receiver: "slack-receiver"
        continue: true
  receivers:
    - name: "slack-receiver"
      slackConfigs:
        - sendResolved: true
          apiURL:
            name: slack-webhook-url
            key: webhook-url
          channel: "#operator-monitoring-test"
          color: '{{ if eq .Status "firing" }}danger{{ else }}good{{ end }}'
          text: |-
            {{ range .Alerts }}
              *Alert:* {{ .Annotations.summary }} - `{{ .Labels.severity }}`
              *Description:* {{ .Annotations.description }}
              *Details:*
              {{ range .Labels.SortedPairs }} • *{{ .Name }}:* `{{ .Value }}`
              {{ end }}
            {{ end }}
```

> [!IMPORTANT]
> When configuring alertmanagerConfig, you must also ​manually create a Kubernetes Secret​ to enable authentication for external notification services such as ​Email​ or ​Slack.
>
> This Secret typically contains sensitive credentials (e.g., API tokens, SMTP credentials) required for Alertmanager to send notifications to these services. Without the Secret, Alertmanager won't be able to establish connections to external providers.

Secret for Email:

```YAML
apiVersion: v1
kind: Secret
metadata:
  name: alertmanager-auth-secret  # Name of the Secret
type: Opaque
data:
  password: YOUR_PASSWORD_BASE64_ENCODED
```

Secret for Slack:

```YAML
apiVersion: v1
kind: Secret
metadata:
  name: slack-webhook-url
type: Opaque
stringData:
  webhook-url: https://hooks.slack.com/services/YOUR_SLACK_WEBHOOK_URL
```

You can use the following example YAML configurations to set up AlertmanagerConfig for Email or Slack alert delivery:

- [Alertmanager-config-Email](../10-samples/monitoring/alertmanager-config-email.yaml)
- [Alertmanager-config-Slack](../10-samples/monitoring/alertmanager-config-slack.yaml)

### TLS Configuration

TLS configuration enables secure communication between Prometheus and TigerGraph clusters.

```yaml
tlsConfig:
  ca:
    secret:
      name: tigergraph-metrics-server-cert
      key: ca.crt
  cert:
    secret:
      name: tigergraph-metrics-server-cert
      key: tls.crt
  keySecret:
    name: tigergraph-metrics-server-cert
    key: tls.key
  insecureSkipVerify: false
```

You can easily manage your SSL certificate files with a Kubernetes Secret using cert-manager, the YAML configuration example is below:

[Generate-SSL-Certificate-with-cert-manager](../10-samples/monitoring/tigergraph-certificate-with-certmanager.yaml)

To skip SSL verification in test environments, you can apply the following configuration:

```yaml
tlsConfig:
  insecureSkipVerify: true
```

## Access Monitoring Interface

By default, Prometheus and Grafana provisioned by kube-prometheus-stack are exposed via a LoadBalancer. To find the external IPs for Prometheus and Grafana, run the following command:

```bash
kubectl get services -n ${MONITORING_NAMESPACE}
```

Look for the external IP and port number of Prometheus and Grafana in the output. An example output is below:

```bash
kubectl get services -n ${MONITORING_NAMESPACE}

NAME                                        TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)          AGE
NAME                                        TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)                         AGE
alertmanager-operated                       ClusterIP      None             <none>           9093/TCP,9094/TCP,9094/UDP      78m
prometheus-operated                         ClusterIP      None             <none>           9090/TCP                        78m
prometheus-stack-grafana                    LoadBalancer   34.118.226.123   104.197.160.82   8081:32555/TCP                  78m
prometheus-stack-kube-prom-alertmanager     LoadBalancer   34.118.234.203   34.136.148.38    9093:32590/TCP,8080:31369/TCP   78m
prometheus-stack-kube-prom-operator         ClusterIP      34.118.228.250   <none>           443/TCP                         78m
prometheus-stack-kube-prom-prometheus       LoadBalancer   34.118.239.17    34.10.96.171     9090:32425/TCP,8080:30638/TCP   78m
prometheus-stack-kube-state-metrics         ClusterIP      34.118.228.145   <none>           8080/TCP                        78m
prometheus-stack-prometheus-node-exporter   ClusterIP      34.118.227.115   <none>           9100/TCP                        78m
```

Take the above output as an example, to access the monitoring tools, open a web browser and navigate to the following endpoints:

- ​Grafana Dashboard: http://104.197.160.82:8081
- Prometheus Web UI: http://34.10.96.171:9090
- AlertManager UI: http://34.136.148.38:9093

The default Grafana dashboard for TigerGraph cluster and TigerGraph Operator is named `TigerGraph Dashboard` and `TigerGraph Kubernetes Controller Runtime Metrics` under `General` folder.

> [!NOTE]
> You can edit the existing dashboard or create your own directly through the Grafana UI. Any changes made via the UI will be persisted by the Grafana service.

## Troubleshooting

### Create TigerGraph monitor CR successfully but with warning events in TigerGraph monitor CR status

If you encounter any problems after creating the TigerGraph monitor CR successfully, please check the TigerGraph monitor CR status using the following command:

```bash
kubectl describe tgmonitor ${TG_MONITOR_NAME} -n ${NAMESPACE}
```

if you encounter warning event: `Required CRDs are not installed, err: customresourcedefinitions.apiextensions.k8s.io "servicemonitors.monitoring.coreos.com" not found`

The status of the TigerGraph monitor CR is as follows:

```bash
kubectl describe tgmonitor tigergraph-monitor -n tigergraph

Name:         tigergraph-monitor
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>
API Version:  graphdb.tigergraph.com/v1alpha1
Kind:         TigerGraphMonitor
Metadata:
  Creation Timestamp:  2025-01-19T13:49:52Z
  Finalizers:
    tigergraph.com/tgmonitor-protection
  Generation:        1
  Resource Version:  15835
  UID:               7812c950-1644-4fec-b4d0-158d99b3c66b
Spec:
  Monitored Clusters:
    test-cluster
Status:
  Conditions:
    Last Transition Time:  2025-01-19T13:49:52Z
    Message:               Required CRDs are not installed
    Reason:                MissingCRD
    Status:                False
    Type:                  Create
Events:
  Type     Reason      Age    From               Message
  ----     ------      ----   ----               -------
  Warning  MissingCRD  2m21s  TigerGraphMonitor  Required CRDs are not installed, err: customresourcedefinitions.apiextensions.k8s.io "servicemonitors.monitoring.coreos.com" not found
```

This warning event indicates that you don't not yet have the kube-prometheus-stack installed, please refer to the section [Install Prometheus and Grafana](#install-prometheus-and-grafana) to install it first.

if you encounter warning event: `Monitored cluster is not present, err: TigerGraph.graphdb.tigergraph.com "test-cluster" not found`

The status of the TigerGraph monitor CR is as follows:

```bash
kubectl describe tgmonitor tigergraph-monitor -n tigergraph

Name:         tigergraph-monitor
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>
API Version:  graphdb.tigergraph.com/v1alpha1
Kind:         TigerGraphMonitor
Metadata:
  Creation Timestamp:  2025-01-19T13:49:52Z
  Finalizers:
    tigergraph.com/tgmonitor-protection
  Generation:        2
  Resource Version:  31989
  UID:               7812c950-1644-4fec-b4d0-158d99b3c66b
Spec:
  Monitored Clusters:
    test-cluster1
Status:
  Conditions:
    Last Transition Time:  2025-01-19T14:11:43Z
    Message:               Monitored cluster is not present
    Reason:                MissingCluster
    Status:                False
    Type:                  Create
Events:
  Type     Reason          Age   From               Message
  ----     ------          ----  ----               -------
  Warning  MissingCluster  45s   TigerGraphMonitor  Monitored cluster is not present, err: TigerGraph.graphdb.tigergraph.com "test-cluster" not found
```

This warning event indicates that you have specified an incorrect TigerGraph cluster name to be monitored or that the TigerGraph cluster has been deleted. To resolve this issue, update the `spec.monitoredClusters` field with the correct cluster name.

Once the issue has been resolved, the status of the TigerGraph Monitor CR will transition to **Normal**.

The status of the TigerGraph monitor CR is as follows:

```bash
kubectl describe tgmonitor tigergraph-monitor -n tigergraph
Name:         tigergraph-monitor
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>
API Version:  graphdb.tigergraph.com/v1alpha1
Kind:         TigerGraphMonitor
Metadata:
  Creation Timestamp:  2025-01-20T04:17:55Z
  Finalizers:
    tigergraph.com/tgmonitor-protection
  Generation:        3
  Resource Version:  19471
  UID:               47436460-904b-4960-9502-366c3895b9a1
Spec:
  Monitored Clusters:
    test-cluster
  Release Name:  monitoring-stack
Status:
  Conditions:
    Last Transition Time:  2025-01-20T04:26:45Z
    Message:               Reconciliation succeeded
    Reason:                ReconcileSucceeded
    Status:                True
    Type:                  Normal
  Dashboard:               189d56d283ca2fd09738d04718801236
  Monitored Clusters:
    test-cluster
  Release Name:  monitoring-stack
Events:
  Type     Reason                    Age                   From               Message
  ----     ------                    ----                  ----               -------
  Warning  MissingCRD                8m52s                 TigerGraphMonitor  Required CRDs are not installed, err: customresourcedefinitions.apiextensions.k8s.io "servicemonitors.monitoring.coreos.com" not found
  Warning  MissingCluster            4m8s (x4 over 7m30s)  TigerGraphMonitor  Monitored cluster is not present, err: TigerGraph.graphdb.tigergraph.com "test-cluster" not found
  Warning  MissingCluster            40s                   TigerGraphMonitor  Monitored cluster is not present, err: TigerGraph.graphdb.tigergraph.com "test-cluster2" not found
  Normal   EnsuringMonitorResources  2s                    TigerGraphMonitor  Start ensuring monitoring resources
  Normal   ReconcileSucceeded        2s                    TigerGraphMonitor  Reconciliation succeeded
```

### Check the serviceMonitorSelector, ruleSelector, and alertmanagerConfigSelector

You may encounter issues where the following Prometheus Custom Resources are created successfully but are not loaded into Prometheus or AlertManager:

- **ServiceMonitor**: Defines which services to scrape for metrics
- **PrometheusRule**: Defines alerting and recording rules
- **AlertmanagerConfig**: Defines alert routing and notification settings

To troubleshoot this issue, you can check the selectors of these resources and verify if they are configured correctly:

```bash
# Check ServiceMonitor selector in Prometheus
kubectl get prometheus ${prometheus-instance-name} -n ${prometheus-installed-namespace} -o yaml | yq .spec.serviceMonitorSelector

# Check PrometheusRule selector in Prometheus
kubectl get prometheus ${prometheus-instance-name} -n ${prometheus-installed-namespace} -o yaml | yq .spec.ruleSelector

# Check AlertmanagerConfig selector in Alertmanager
kubectl get alertmanager ${alertmanager-instance-name} -n ${prometheus-installed-namespace} -o yaml | yq .spec.alertmanagerConfigSelector
```

**Common Issues and Solutions:**

1. **Selector Mismatch**: Ensure the labels on your ServiceMonitor, PrometheusRule, or AlertmanagerConfig match the selectors configured in Prometheus/Alertmanager.

2. **Namespace Issues**: Verify that the resources are in the correct namespace that Prometheus/Alertmanager is configured to watch.

**Example Verification:**

```bash
# Check what ServiceMonitors exist
kubectl get servicemonitor --all-namespaces

# Check the labels on a specific ServiceMonitor
kubectl get servicemonitor my-servicemonitor -n my-namespace -o yaml | yq .metadata.labels

# check the labels on a specific PrometheusRule

# check the labels on a specific AlertmanagerConfig

# Verify Prometheus is watching the correct namespace
kubectl get prometheus my-prometheus -n my-namespace -o yaml | yq .spec.serviceMonitorNamespaceSelector
```

## Uninstall Monitoring Components

### Uninstall Prometheus and Grafana

```bash
# Uninstall monitoring stack
kubectl tg monitoring-stack delete \
  --kube-prometheus-stack-release-name monitoring-stack \
  --namespace monitoring-stack
```

### Clean Up CRDs

```bash
# Delete monitoring CRDs (optional, affects other monitoring setups)
kubectl get crd -o name | grep monitoring.coreos.com | xargs kubectl delete
```

### Clean Up Persistent Storage

```bash
# Delete PVCs (be careful, this will delete all monitoring data)
kubectl delete pvc -n monitoring-stack --all

# Delete namespace
kubectl delete namespace monitoring-stack
```

> [!WARNING]
> Uninstalling monitoring components will delete all monitoring data and configurations. Make sure to backup any important data before proceeding.

## TigerGraph Metrics Reference

For a comprehensive reference of all TigerGraph metrics, including detailed descriptions, labels, and usage examples, see the [TigerGraph Metrics Reference](tigergraph-metrics-reference.md) document.

This reference covers:

- **CPU Metrics**: CPU usage, availability, and core counts
- **Memory Metrics**: Memory usage, availability, and utilization percentages
- **Disk Metrics**: Disk usage, I/O operations, and filesystem statistics
- **Network Metrics**: Connection counts and traffic patterns
- **Service Metrics**: Service health and status indicators
- **License Metrics**: License expiration tracking
- **Query Performance Metrics**: Latency, throughput, and completion rates

The metrics reference also includes Prometheus query examples and alerting recommendations to help you build effective monitoring dashboards and alert rules.
