# Enable TigerGraph Operator monitoring with Prometheus and Grafana

Since version 1.5.0, the TigerGraph Operator includes support for monitoring. This guide demonstrates how to deploy Prometheus and Grafana for observability, as well as how to enable the default TigerGraph Monitor CR to automatically set up the default Grafana dashboard for enhanced visualization.

- [Enable TigerGraph Operator monitoring with Prometheus and Grafana](#enable-tigergraph-operator-monitoring-with-prometheus-and-grafana)
  - [Install Prometheus and Grafana](#install-prometheus-and-grafana)
  - [Manage TigerGraph Monitor](#manage-tigergraph-monitor)
    - [Manage TigerGraph Monitor using kubectl-tg plugin](#manage-tigergraph-monitor-using-kubectl-tg-plugin)
      - [Create a TigerGraph monitor](#create-a-tigergraph-monitor)
      - [Update a TigerGraph monitor](#update-a-tigergraph-monitor)
      - [Delete a TigerGraph monitor](#delete-a-tigergraph-monitor)
    - [Manage TigerGraph Monitor using CR](#manage-tigergraph-monitor-using-cr)
  - [Access Grafana dashboard](#access-grafana-dashboard)
  - [Uninstall Prometheus and Grafana](#uninstall-prometheus-and-grafana)
  - [Troubleshooting](#troubleshooting)
    - [Create TigerGraph monitor CR successfully but with warning events in TigerGraph monitor CR status](#create-tigergraph-monitor-cr-successfully-but-with-warning-events-in-tigergraph-monitor-cr-status)

## Install Prometheus and Grafana

It is recommended to install Prometheus and Grafana using the [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack). You can achieve this by using the sub command `kubectl tg monitoring-stack` of kubectl-tg plugin. Alternatively, you can install the kube-prometheus-stack using Helm by following the [official documentation](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack#kube-prometheus-stack).

> [!IMPORTANT]
> Currently, it is not possible to install the kube-prometheus-stack Helm chart on OpenShift. As a result, the kubectl-tg plugin cannot be used to install it.
> If your OpenShift cluster does not yet have the kube-prometheus-stack installed, please refer to the [OpenShift official documentation](https://docs.openshift.com/container-platform/4.8/monitoring/configuring-the-monitoring-stack.html) for detailed instructions on configuring the monitoring stack.

The following example steps will install kube-prometheus-stack using the kubectl-tg plugin:

```bash
kubectl tg monitoring-stack --help
Manage kube-prometheus-stack deployment

Examples:
  # create a kube-prometheus-stack deployment with name and namespace
  kubectl tg monitoring-stack create -r monitoring-stack -n monitoring-stack
  # create a kube-prometheus-stack deployment with customized values.yaml
  kubectl tg monitoring-stack create -r monitoring-stack -f values.yaml -n monitoring-stack
  # update a kube-prometheus-stack deployment with values.yaml
  kubectl tg monitoring-stack update -r monitoring-stack -f values.yaml -n monitoring-stack
  # delete a kube-prometheus-stack deployment with name and namespace
  kubectl tg monitoring-stack delete -r monitoring-stack -n monitoring-stack

Usage:
  kubectl tg monitoring-stack [create|update|delete] [OPTIONS]

Options:
  -n|--namespace :    set namespace, if not set, use the default namespace in context
  -r|--kube-prometheus-stack-release-name : 
                      specify release name of kube-prometheus-stack deployment
  -f|--kube-prometheus-stack-values : 
                      specify values.yaml of kube-prometheus-stack deployment. If not set, use the default values.yaml
```

> [!NOTE]
> For TigerGraph Operator 1.5.0, the kubectl-tg plugin installs kube-prometheus-stack version 68.2.1 by default. The minimum supported version of kube-prometheus-stack is 45.25.0.
> If you prefer to install a specific version of kube-prometheus-stack between 45.25.0 and 68.2.1, use the Helm command to install it manually by following the [official documentation](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack#kube-prometheus-stack).

Using the following command to create a kube-prometheus-stack deployment with release name, namespace and customize configuration of kube-prometheus-stack.

```bash
export PATH_TO_VALUES_YAML="values.yaml"
export RELEASE_NAME="monitoring-stack"
export MONITORING_NAMESPACE="monitoring-stack"

kubectl tg monitoring-stack create -f ${PATH_TO_VALUES_YAML} -r ${RELEASE_NAME} -n ${MONITORING_NAMESPACE}
```

> [!NOTE]
> It is recommended to install Prometheus and Grafana in a separate namespace from the TigerGraph clusters to ensure cleaner and more organized management.

The customized configuration of kube-prometheus-stack YAML resource example is following as:

```YAML
# The values.yaml file for kube-prometheus-stack configures a comprehensive suite of monitoring services 
# to deliver observability insights. 
# It enables both Prometheus and Grafana, exposing them externally via LoadBalancers.

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
  enabled: false
```

> [!NOTE]
> For the external services of Prometheus and Grafana, please configure them according to your specific requirements. Additionally, ensure that you allocate an appropriate storage size to accommodate your monitoring data effectively.

After successfully installing the kube-prometheus-stack, you can check its status by running the following command:

```bash
kubectl --namespace ${MONITORING_NAMESPACE} get pods -l "release=${RELEASE_NAME}"

NAME                                                   READY   STATUS    RESTARTS   AGE
monitoring-stack-kube-prom-operator-bcf69c8c9-tx4lw    1/1     Running   0          2m23s
monitoring-stack-kube-state-metrics-6cf9d56576-ckmf5   1/1     Running   0          2m23s
monitoring-stack-prometheus-node-exporter-xhn4p        1/1     Running   0          2m23s
monitoring-stack-prometheus-node-exporter-zc92p        1/1     Running   0          2m23s
monitoring-stack-prometheus-node-exporter-zhfg5        1/1     Running   0          2m23s
```

You can also use the command `kubectl tg monitoring-stack update` or `kubectl tg monitoring-stack delete` to update or delete the kube-prometheus-stack. For detailed usage instructions, please refer to the command's help documentation.

## Manage TigerGraph Monitor

To support monitoring in the TigerGraph Operator, a new controller, called the TigerGraph Monitoring Controller, has been introduced. This controller manages the lifecycle of all monitoring-related CRs, including `ServiceMonitor`, `ConfigMap`, and others.

### Manage TigerGraph Monitor using kubectl-tg plugin

You can manage a TigerGraph Monitor CR by subcommand `kubectl tg monitor` of kubectl-tg plugin.

```bash
kubectl tg monitor --help
Manage TigerGraph monitor
 
Examples:
  # create a TigerGraph monitor with name and namespace
  kubectl tg monitor create --name tigergraph-monitor -n tigergraph
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
                      specify release name of kube-prometheus-stack deployment
```

#### Create a TigerGraph monitor

```bash
kubectl tg monitor create --name ${TG_MONITOR_NAME} -n ${NAMESPACE}
```

When the TigerGraph Monitor CR is successfully created, it automatically configures Prometheus to scrape the TigerGraph metrics endpoints specified in the `ServiceMonitor` CR. Additionally, it sets up a default Grafana dashboard based on the configuration defined in a `ConfigMap`.

> [!NOTE]
> By default, the TigerGraph Monitor CR monitors all TigerGraph clusters in the current namespace. To monitor specific TigerGraph clusters, specify them by the option `--monitored-clusters`

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the release name of the kube-prometheus-stack by the option `-r|--kube-prometheus-stack-release-name`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

#### Update a TigerGraph monitor

```bash
kubectl tg monitor update --name ${TG_MONITOR_NAME} --monitored-clusters ${MONITORED_CLUSTERS} -r ${RELEASE_NAME} -n ${NAMESPACE}
```

#### Delete a TigerGraph monitor

```bash
kubectl tg monitor delete --name ${TG_MONITOR_NAME} -n ${NAMESPACE}
```

### Manage TigerGraph Monitor using CR

You can use the following example TigerGraph Monitor CR YAML resource to manage the TigerGraph Monitor:

```YAML
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphMonitor
metadata:
  name: ${TG_MONITOR_NAME}
  namespace: ${NAMESPACE}
spec:
  monitoredClusters:
  - test-cluster1
  - test-cluster2
  releaseName: ${RELEASE_NAME}

```

> [!NOTE]
> If you don't specify `spec.monitoredClusters`, the TigerGraph Monitor CR monitors all TigerGraph clusters in the current namespace.

> [!WARNING]
> If the TigerGraph Operator is namespace-scoped, you must manually specify the release name of the kube-prometheus-stack in the field `spec.releaseName`. This ensures the monitoring controller can correctly identify it, especially when the kube-prometheus-stack is deployed in a different namespace from the TigerGraph clusters.

## Access Grafana dashboard

By default, Prometheus and Grafana provisioned by kube-prometheus-stack are exposed via a LoadBalancer. To find the external IPs for Prometheus and Grafana, run the following command:

```bash
kubectl get services -n ${MONITORING_NAMESPACE}
```

Look for the external IP and port number of Prometheus and Grafana in the output. An example output is below:

```bash
kubectl get services -n ${MONITORING_NAMESPACE}

NAME                                        TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)          AGE
monitoring-stack-grafana                    LoadBalancer   34.118.225.63    104.198.48.129   8081:32767/TCP   10m
monitoring-stack-kube-prom-operator         ClusterIP      34.118.231.29    <none>           443/TCP          10m
monitoring-stack-kube-prom-prometheus       LoadBalancer   34.118.226.87    34.28.30.58      9090:31174/TCP   10m
monitoring-stack-kube-state-metrics         ClusterIP      34.118.231.76    <none>           8080/TCP         10m
monitoring-stack-prometheus-node-exporter   ClusterIP      34.118.231.219   <none>           9100/TCP         10m
prometheus-operated                         ClusterIP      None             <none>           9090/TCP         10m
```

Take the above output as an example: you can access the Grafana dashboard by visiting https://104.198.48.129:8081 and the Prometheus web UI by visiting http://34.28.30.58:9090 in a web browser.

The default Grafana dashboard is named `TigerGraph Dashboard` under `General` folder.

> [!NOTE]
> You can edit the existing dashboard or create your own directly through the Grafana UI. Any changes made via the UI will be persisted by the Grafana service.

## Uninstall Prometheus and Grafana

To uninstall Prometheus and Grafana, run the following command:

```bash
kubectl tg monitoring-stack delete -r ${RELEASE_NAME} -n ${MONITORING_NAMESPACE}
```

This removes all the Kubernetes components associated with the chart and deletes the release.

CRDs created by this chart are not removed by default and should be manually cleaned up:

```bash
kubectl delete crd alertmanagerconfigs.monitoring.coreos.com
kubectl delete crd alertmanagers.monitoring.coreos.com
kubectl delete crd podmonitors.monitoring.coreos.com
kubectl delete crd probes.monitoring.coreos.com
kubectl delete crd prometheusagents.monitoring.coreos.com
kubectl delete crd prometheuses.monitoring.coreos.com
kubectl delete crd prometheusrules.monitoring.coreos.com
kubectl delete crd scrapeconfigs.monitoring.coreos.com
kubectl delete crd servicemonitors.monitoring.coreos.com
kubectl delete crd thanosrulers.monitoring.coreos.com
```

Additional, if you specify the persistent volume claim for the Prometheus and Grafana, you also need to delete the persistent volume manually. You can use the following command to figure out the persistent volume claim name:

```bash
kubectl get pvc --namespace ${MONITORING_NAMESPACE}
```

Example output:

```bash
NAME                                                                                                     STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
monitoring-stack-grafana                                                                                 Bound    pvc-80abe8dc-8925-4e58-80fc-104f1c31f6f9   5Gi        RWO            standard-rwo   <unset>                 3h31m
prometheus-monitoring-stack-kube-prom-prometheus-db-prometheus-monitoring-stack-kube-prom-prometheus-0   Bound    pvc-c60a27e5-b524-494d-b2a5-917bd433147c   50Gi       RWO            standard-rwo   <unset>                 3h31m
```

Delete the persistent volume claim of the Prometheus and Grafana with the following command:

> [!IMPORTANT]
> Please ensure that you no longer need the data from Prometheus and Grafana before deleting the persistent volume claims.

```bash
kubectl delete pvc ${PVC_NAME} --namespace ${MONITORING_NAMESPACE}
```

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
