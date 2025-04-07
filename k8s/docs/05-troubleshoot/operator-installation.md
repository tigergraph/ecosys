# TigerGraph Operator Installation Troubleshooting

This document outlines common issues and provides solutions for troubleshooting TigerGraph Operator installation in a Kubernetes environment.

- [TigerGraph Operator Installation Troubleshooting](#tigergraph-operator-installation-troubleshooting)
  - [Troubleshooting Steps](#troubleshooting-steps)
  - [Potential failures](#potential-failures)
    - [TigerGraph Operator Installation Failed: Resource Mapping Not Found for "tigergraph-operator-serving-cert"](#tigergraph-operator-installation-failed-resource-mapping-not-found-for-tigergraph-operator-serving-cert)
    - [Operator Pods Are Not Ready, and Their Status Remains in ContainerCreating](#operator-pods-are-not-ready-and-their-status-remains-in-containercreating)

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

### TigerGraph Operator Installation Failed: Resource Mapping Not Found for "tigergraph-operator-serving-cert"

**Issue**:

When installing the TigerGraph Operator, you may encounter the following error if cert-manager is not installed:

  ```bash
  Error: INSTALLATION FAILED: unable to build kubernetes objects from release manifest: [resource mapping not found for name: "tigergraph-operator-serving-cert" namespace: "tigergraph" from "": no matches for kind "Certificate" in version "cert-manager.io/v1"
  ```

**Solution**:

- Verify cert-manager Installation

  Check if cert-manager and its components are deployed and running:

  ```bash
  kubectl get deployment -n cert-manager cert-manager
  kubectl get deployment -n cert-manager cert-manager-cainjector
  kubectl get deployment -n cert-manager cert-manager-webhook
  ```

  Expected output (example):

  ```text
  NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
  cert-manager              1/1     1            1           5m
  cert-manager-cainjector   1/1     1            1           5m
  cert-manager-webhook      1/1     1            1           5m

  ```

- Install cert-manager (If Not Installed)

If cert-manager is missing, install it using the following command:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.13/cert-manager.yaml
```

- Wait for cert-manager to Be Ready

Ensure all cert-manager components are running before proceeding:

```bash
kubectl wait deployment -n cert-manager cert-manager --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-cainjector --for condition=Available=True --timeout=90s
kubectl wait deployment -n cert-manager cert-manager-webhook --for condition=Available=True --timeout=90s
```

Once cert-manager is successfully installed and running, retry the TigerGraph Operator installation.

### Operator Pods Are Not Ready, and Their Status Remains in ContainerCreating

After successfully installing the Operator, you might encounter an issue where the Operator pods are not ready, and their status is stuck in ContainerCreating. You can diagnose and resolve this issue by following these steps:

Step 1: Check the Status of the TigerGraph Operator Pods

Run the following command to check the status of the TigerGraph Operator pod

```bash
kubectl get pods -l control-plane=controller-manager -n tigergraph
```

Example output:

```text
NAME                                                     READY   STATUS              RESTARTS   AGE
tigergraph-operator-controller-manager-667fcb757-wcz22   0/2     ContainerCreating   0          85m
```

Next, describe the specific pod to gather more details about its state:

```bash
kubectl describe pod tigergraph-operator-controller-manager-667fcb757-wcz22 -n tigergraph
```

Example output (excerpt):

```text
Status:           Pending
Conditions:
  Type                        Status
  PodReadyToStartContainers   False 
  Initialized                 True 
  Ready                       False 
  ContainersReady             False 
  PodScheduled                True 
Events:
  Type     Reason       Age                   From     Message
  ----     ------       ----                  ----     -------
  Warning  FailedMount  3m48s (x48 over 85m)  kubelet  MountVolume.SetUp failed for volume "cert" : secret "webhook-server-cert" not found
```

The event log suggests that the secret `webhook-server-cert` was not created successfully, causing the pod to remain in ContainerCreating status.

Step 2: Check the Logs of Cert-Manager

To investigate why the secret `webhook-server-cert` is missing, check the `cert-manager` logs.

First, list the `cert-manager` pods:

```bash
kubectl get pods -n cert-manager
```

Example output:

```text
NAME                                       READY   STATUS    RESTARTS   AGE
cert-manager-94c757fdd-dxk7l               1/1     Running   0          79m
cert-manager-cainjector-7d6c965794-lpmrj   1/1     Running   0          86m
cert-manager-webhook-66fcdcb69b-prvpm      1/1     Running   0          79m
```

Next, check the logs of the `cert-manager` pod:

```bash
kubectl logs cert-manager-94c757fdd-dxk7l -n cert-manager
```

Example log output (excerpt):

```text
E0323 23:57:16.359546       1 requestmanager_controller.go:209] "Multiple matching CertificateRequest resources exist, delete one of them. This is likely an error and should be reported on the issue tracker!" logger="cert-manager.certificates-request-manager" key="tigergraph/tigergraph-operator-serving-cert"
```

The error log indicates that multiple CertificateRequest resources exist for tigergraph-operator-serving-cert, preventing `cert-manager` from issuing a certificate.

Step 3: List Existing CertificateRequests

Check if multiple CertificateRequest resources exist in the TigerGraph namespace:

```bash
kubectl get certificaterequests -n tigergraph
```

Example output:

```text
NAME                                     APPROVED   DENIED   READY   ISSUER                                  REQUESTOR                                         AGE
tigergraph-operator-serving-cert-6zlmv   True                True    tigergraph-operator-selfsigned-issuer   system:serviceaccount:cert-manager:cert-manager   13h
tigergraph-operator-serving-cert-m6bk2   True                True    tigergraph-operator-selfsigned-issuer   system:serviceaccount:cert-manager:cert-manager   13h
tigergraph-operator-serving-cert-tj9w9   True                True    tigergraph-operator-selfsigned-issuer   system:serviceaccount:cert-manager:cert-manager   13h
tigergraph-operator-serving-cert-vb84z   True                True    tigergraph-operator-selfsigned-issuer   system:serviceaccount:cert-manager:cert-manager   13h
```

Step 4: Resolve the CertificateRequest Conflict

If multiple CertificateRequest resources exist, delete the old or duplicate requests to allow cert-manager to issue the certificate properly.

Identify the duplicate CertificateRequests:

```bash
kubectl get certificaterequests -n tigergraph
```

Delete the unnecessary requests:

```bash
kubectl delete certificaterequest <certificate-request-name> -n tigergraph
```

Restart the cert-manager pod to apply the changes:

```bash
kubectl rollout restart deployment cert-manager -n cert-manager
```

Restart the TigerGraph Operator pod:

```bash
kubectl delete pod tigergraph-operator-controller-manager-667fcb757-wcz22 -n tigergraph
```

After performing these steps, the Operator pod should transition to the Running state. You can verify this by rechecking the pod status:

```bash
kubectl get pods -l control-plane=controller-manager -n tigergraph
```

If the issue persists, check the Kubernetes events and logs again for further debugging.
