# TigerGraph Operator Installation Troubleshooting

This document outlines common issues and provides solutions for troubleshooting TigerGraph Operator installation in a Kubernetes environment.

- [TigerGraph Operator Installation Troubleshooting](#tigergraph-operator-installation-troubleshooting)
  - [Troubleshooting Steps](#troubleshooting-steps)
  - [Potential failures](#potential-failures)

## Troubleshooting Steps

In the following steps, we assume that the TigerGraph Operator has been installed in the `tigergraph` namespace. Please adapt the commands according to your specific setup.

- Verify Operator Installation

  Ensure that the TigerGraph Operator has been successfully installed. If not, install the operator first.

  ```bash
  kubectl get deployment tigergraph-operator-controller-manager -o wide -n tigergraph

  NAME                                     READY   UP-TO-DATE   AVAILABLE   AGE   CONTAINERS                IMAGES                                                                                         SELECTOR
  tigergraph-operator-controller-manager   1/1     1            1           22m   manager,kube-rbac-proxy   docker.io/tigergrah/tigergraph-k8s-operator:0.0.3,gcr.io/kubebuilder/kube-rbac-proxy:v0.8.0   control-plane=controller-manager
  ```

  From the output of the above command, you can figure out that the operator version is 0.0.3, docker.io/tigergrah/tigergraph-k8s-operator:0.0.3, you can also use the following helm command to get the current version of Operator:

  ```bash
  helm list -n tigergraph
  
  NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                   APP VERSION
  tg-operator     tigergraph      1               2023-02-26 13:16:15.701059001 +0000 UTC deployed        tg-operator-0.0.3

  ```

- If the Operator has been installed, Check if the TigerGraph Operator is running normally:

  ```bash
  kubectl get pods -l control-plane=controller-manager -n tigergraph

  NAME                                                     READY   STATUS    RESTARTS   AGE
  tigergraph-operator-controller-manager-c8d65bcd9-t2mkz   2/2     Running   0          12m
  ```

  If the status is `Running` and the `READY` field shows "2/2," it indicates that the Operator deployment is running normally.

- If the Operator is not running normally, follow these steps:
  - Get the Pod Name of the Operator  

    ```bash
    kubectl get pods -l control-plane=controller-manager -n tigergraph
    
    NAME                                                     READY   STATUS    RESTARTS   AGE
    tigergraph-operator-controller-manager-c8d65bcd9-t2mkz   2/2     Running   0          12m
    ```
  
  - Make sure to replace the pod name with the one from the previous command's output:

    ```bash
    kubectl describe pod tigergraph-operator-controller-manager-c8d65bcd9-t2mkz -n tigergraph
    ```
  
  - To identify the root cause of the Operator pod restarting, check its logs:

    ```bash
    kubectl logs tigergraph-operator-controller-manager-c8d65bcd9-t2mkz -f -n tigergraph
    ```

- If you have multiple Operator pods for high availability, you should check logs of these pods to find the leader of operator, and check the logs of leader.

  - Get pod name of Operator

    ```bash
    kubectl get pods -l control-plane=controller-manager -n tigergraph
    NAME                                                      READY   STATUS    RESTARTS        AGE
    tigergraph-operator-controller-manager-869b885466-5qwkp   2/2     Running   0               28h
    tigergraph-operator-controller-manager-869b885466-6cq8w   2/2     Running   0               28h
    tigergraph-operator-controller-manager-869b885466-6jpnq   2/2     Running   0               28h
    ```

  - Check logs of these pods

    ```bash
    kubectl logs tigergraph-operator-controller-manager-869b885466-5qwkp -n tigergraph
    kubectl logs tigergraph-operator-controller-manager-869b885466-6cq8w -n tigergraph
    kubectl logs tigergraph-operator-controller-manager-869b885466-6jpnq -n tigergraph
    ```

  - The logs of a leader should include the following output

    ```bash
    I0509 07:57:53.476671       1 leaderelection.go:248] attempting to acquire leader lease tigergraph/9d6fe668.tigergraph.com...
    2023-05-09T07:57:53.476Z	INFO	starting metrics server	{"path": "/metrics"}
    2023-05-09T07:57:53.476Z	INFO	controller-runtime.webhook.webhooks	starting webhook server
    2023-05-09T07:57:53.476Z	INFO	controller-runtime.certwatcher	Updated current TLS certificate
    2023-05-09T07:57:53.477Z	INFO	controller-runtime.webhook	serving webhook server	{"host": "", "port": 9443}
    2023-05-09T07:57:53.477Z	INFO	controller-runtime.certwatcher	Starting certificate watcher
    I0509 07:57:53.498264       1 leaderelection.go:258] successfully acquired lease tigergraph/9d6fe668.tigergraph.com
    ```

## Potential failures

- Before installing the operator, ensure that cert-manager has been installed. Failure to do so may result in the following error during operator installation:

  ```bash
  Error: INSTALLATION FAILED: unable to build kubernetes objects from release manifest: [resource mapping not found for name: "tigergraph-operator-serving-cert" namespace: "tigergraph" from "": no matches for kind "Certificate" in version "cert-manager.io/v1"
  ```

- To verify that cert-manager is installed and running normally, follow these steps:

  - Check cert-manager Deployments:

    ```bash
    kubectl get deployment -n cert-manager cert-manager
    
    kNAME           READY   UP-TO-DATE   AVAILABLE   AGE
    cert-manager   1/1     1            1           5m27s
    
    kubectl get deployment -n cert-manager cert-manager-cainjector
    
    NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
    cert-manager-cainjector   1/1     1            1           5m27s
    
    kubectl get deployment -n cert-manager cert-manager-webhook
    
    NAME                   READY   UP-TO-DATE   AVAILABLE   AGE
    cert-manager-webhook   1/1     1            1           5m28s

    kubectl get deployment -n cert-manager cert-manager
    kubectl get deployment -n cert-manager cert-manager-cainjector
    kubectl get deployment -n cert-manager cert-manager-webhook
    ```

  - If cert-manager is not installed, install it:

    ```bash
    kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.13/cert-manager.yaml
    ```
  
  - Ensure cert-manager is running normally:

    ```bash
    kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
    kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
    kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
    ```
