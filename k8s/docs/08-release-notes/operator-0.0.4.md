# Operator 0.0.4 Release notes

## Overview

**Operator 0.0.4** has been released in conjunction with **TigerGraph 3.9.0**.

In this release, **Operator 0.0.4** brings various enhancements, including support for operator updating, operator upgrading, cluster backup, and cluster restore.

### kubectl plugin installation

To install the kubectl plugin for Operator 0.0.4, please use the following command:

```bash
curl https://dl.tigergraph.com/k8s/0.0.4/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### CRD upgrading

Upgrade the Custom Resource Definition for Operator 0.0.4 using the following command:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/0.0.4/tg-operator-crd.yaml
```

### Operator upgrading

Upgrade the Operator to version 0.0.4 with the following command:

```bash
kubectl tg upgrade --namespace ${NAMESPACE_OF_OPERATOR} --operator-version 0.0.4
```

## New features

- Operator Updating: Operator 0.0.4 now supports updating the operator, ensuring you stay up to date with the latest enhancements.

- Operator Upgrading: Seamlessly upgrade your Operator to version 0.0.4 and beyond.

- Support for Namespaced-Scope and Cluster-Scope Operator: Enjoy the flexibility of namespaced-scope and cluster-scope operators.

- Cluster Backup: Perform both one-time and scheduled backups of your TigerGraph cluster.

- Cluster Restore: Easily restore your TigerGraph cluster as needed.

## Improvements

- Security Fixes for TG K8s Docker Image:

  - Removed the use of sudo from the TG docker image for enhanced security.
Customized private SSH key files for added security.
  - Enhancements in Expansion and Shrinking: Improved support for expansion and shrinking, including a robust failure recovery process.

- Security Vulnerabilities Fixes in K8s Operator: Addressed security vulnerabilities in the K8s operator.

- kubectl tg Use AWS Secret Name: Improved security by using AWS secret names instead of directly passing strings in the options.

## Bugfixes

- Fixed issues causing GSE crashes when the number of pods exceeded 32.

- Updated the nginx template during installation upgrades.

- Configured LBS (Load Balancer Service) to forward RESTPP requests to the corresponding cluster when multiple clusters exist in the same namespace.
