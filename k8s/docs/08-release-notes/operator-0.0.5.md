# Operator 0.0.5 Release notes

## Overview

**Operator version 0.0.5** has been released in conjunction with **TigerGraph 3.9.1**. This release introduces several enhancements, bugfixes, and the ability to configure resource limits for both Operator and the TigerGraph cluster.

### kubectl plugin installation

To install the kubectl plugin for Operator 0.0.5, please execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/0.0.5/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

### CRD upgrading

To upgrade the Custom Resource Definitions (CRD) for Operator 0.0.5, use the following command:

```bash
kubectl apply -f https://dl.tigergraph.com/k8s/0.0.5/tg-operator-crd.yaml
```

### Operator upgrading

Upgrade your Operator to version 0.0.5 with the following command:

```bash
kubectl tg upgrade --namespace ${NAMESPACE_OF_OPERATOR} --operator-version 0.0.5
```

## New features

- Resource Limit Configuration: Operator 0.0.5 now allows you to configure resource limits for both the Operator itself and the TigerGraph cluster using the kubectl-tg plugin.

## Improvements

- Automated Backup Recovery: Operator now supports automatic backup recovery, simplifying data restoration processes.

- Enhanced Cluster Initialization: Improvements have been made to the cluster initialization process, enhancing stability and usability.

- Improved kubectl tg Commands: Various enhancements have been made to kubectl tg commands, making them more user-friendly.

## Bugfixes

- Upgrade Issue: Fixed an issue that caused problems when upgrading from version 3.7.0 to 3.9.0.

- Job Name Length: Added a hint when the job name exceeds the RFC 1123 character limit.

- Fixed an issue where expansion or shrink operations on an empty queue would skip pausing GPE.
