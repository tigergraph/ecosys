# Operator 0.0.6 Release notes

## Overview

**Operator 0.0.6** has been released in conjunction with the **TigerGraph 3.9.1 update**.

In this release, **Operator 0.0.6** introduces support for node and pod affinity configuration, along with the ability to customize init containers and sidecar containers. Notably, after upgrading to Operator version 0.0.6, creating a service account when deploying the TigerGraph cluster in a different namespace is no longer mandatory.

### kubectl plugin installation

To install the kubectl plugin for Operator 0.0.6, please execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/0.0.6/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### CRD upgrading

To upgrade the Custom Resource Definitions (CRD) for Operator 0.0.6, use the following command:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/0.0.6/tg-operator-crd.yaml
```

### Operator upgrading

After upgrading the Operator, the TG cluster will undergo a rolling update due to tg-log volume mounting changes.

To upgrade the Operator to version 0.0.6, please execute the following command:

```bash
kubectl tg upgrade --namespace ${NAMESPACE_OF_OPERATOR} --operator-version 0.0.6
```

## New features

- Added support for Node selector, Pod Affinity, and Toleration.
- Introduced the ability to include customized init containers and sidecar containers.
- Optional configuration for the service account name of TG pod when managing the TG cluster using cluster-scoped Operator.

## Improvements

No specific improvements have been made in this release.

## Bugfixes

No known bugs have been addressed in this release.
