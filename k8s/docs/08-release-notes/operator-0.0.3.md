# Operator 0.0.3 Release notes

## Overview

**Operator 0.0.3** has been released in conjunction with **TigerGraph 3.8.0**.

This release of Operator 0.0.3 primarily focuses on enabling special cluster operations for TigerGraph on Kubernetes (K8s). These operations include resource updates (CPU and Memory), TigerGraph cluster upgrading, and cluster scaling.

### kubectl plugin installation

To install the kubectl plugin for **Operator 0.0.3**, please execute the following command:

```bash
curl https://dl.tigergraph.com/k8s/0.0.3/kubectl-tg  -o kubectl-tg
sudo install kubectl-tg /usr/local/bin/
```

## New features

- Cluster Resource Update: Operator 0.0.3 introduces the capability to update cluster resources, including CPU and Memory configurations.

- Cluster Upgrading: You can now upgrade your TigerGraph cluster using this release.

- Cluster Expansion: Expand your TigerGraph cluster effortlessly to meet growing demands.

- Cluster Shrinking: When necessary, scale down your TigerGraph cluster efficiently.

## Improvements

- High Availability (HA) Enabled by Default: Operator 0.0.3 now enables High Availability by default, ensuring greater reliability and fault tolerance.

## Bugfixes

- Addressed an issue where the expand command would become stuck when no schema and graph data existed in the TigerGraph cluster. ([CORE-1743](https://graphsql.atlassian.net/browse/CORE-1743), TigerGraph 3.8.0)
