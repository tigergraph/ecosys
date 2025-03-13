# Upgrade the TigerGraph Cluster Using the TigerGraph Operator

This guide will walk you through upgrading the TigerGraph Cluster using the TigerGraph Operator.

- [Upgrade the TigerGraph Cluster Using the TigerGraph Operator](#upgrade-the-tigergraph-cluster-using-the-tigergraph-operator)
  - [Upgrade the TigerGraph Cluster](#upgrade-the-tigergraph-cluster)
  - [Upgrade pre-check for TigerGraph upgrading](#upgrade-pre-check-for-tigergraph-upgrading)
  - [Maintenance Release Upgrade support](#maintenance-release-upgrade-support)
  - [Troubleshooting](#troubleshooting)
    - [How to proceed with the upgrade process if the upgrade pre-check job fails due to incorrect image or downgrade error](#how-to-proceed-with-the-upgrade-process-if-the-upgrade-pre-check-job-fails-due-to-incorrect-image-or-downgrade-error)
    - [How to proceed with the upgrade process if the upgrade pre-check fails Due to insufficient ephemeral local storage](#how-to-proceed-with-the-upgrade-process-if-the-upgrade-pre-check-fails-due-to-insufficient-ephemeral-local-storage)

## Upgrade the TigerGraph Cluster

To upgrade the TigerGraph cluster using the TigerGraph Operator, you can use the kubectl-tg plugin or update the TigerGraph Cluster CR to complete the upgrade seamlessly.

Upgrade the TigerGraph cluster using kubectl-tg plugin:

```bash
kubectl tg update --cluster-name ${TG_CLUSTER_NAME} --version ${TG_UPGRADE_VERSION} -n ${NAMESPACE}
```

Upgrade the TigerGraph cluster by updating the field `spec.image` in TigerGraph CR:

```YAML
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.2.0
  imagePullPolicy: IfNotPresent
  ha: 2
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 6
  resources:
    limits:
      cpu: "6"
      memory: 12Gi
    requests:
      cpu: "6"
      memory: 12Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      resources:
        requests:
          storage: 100G
      storageClassName: standard
```

## Upgrade pre-check for TigerGraph upgrading

While the cluster upgrade process is straightforward, there have been significant improvements introduced with TigerGraph Operator version 1.5.0.

Before version 1.5.0, the TigerGraph cluster became unavailable as soon as the upgrade process began. Regardless of whether the cluster was in the RollingUpgrade or UpgradePost status, the cluster remained inaccessible until the upgrade process was fully completed and the cluster returned to the Normal status.

From version 1.5.0 of the Operator and version 4.2.0 of TigerGraph onward, a pre-check job is introduced as part of the upgrade process. This pre-check job uses the image of the upgraded TigerGraph version to:

- Copy the pre-check script and binaries from the upgraded image.
- Execute a pre-check to validate the upgrade.

If the pre-check fails due to incorrect image or downgrade error, it does not impact the running cluster, ensuring minimal risk during the upgrade preparation phase.

> [!WARNING]
> When configuring the `spec.resources.limits.ephemeral-storage` field, please ensure that its value is not less than 20Gi. This is necessary because the upgrade pre-check will copy extra files into m1, which will require 5Gi of storage. Starting from version 1.5.0, the TigerGraph webhook will verify that this field is set to at least 20Gi if it is configured.

## Maintenance Release Upgrade support

A maintenance release upgrade refers to upgrades from version x.y.z-a to x.y.w-b, where:

w > z (mandatory)
b > a (optional, only enforced when w == z)

Starting from Operator 1.5.0 and TigerGraph 4.2.0, for clusters with a High Availability (HA) factor of at least 2, the upgrade process for maintenance releases is designed to keep the system online with minimal service disruption. We call this process `Rolling upgrade for maintenance releases`, which is an online upgrade process.
The online upgrade process follows the same strategy as online scale/up down. To know the details of the process, please refer to [Overview of the Scale Up/Down Process](./scale-up-and-down.md#overview-of-the-scale-updown-process)

> [!IMPORTANT]
> Please note that the minimum versions required for a Maintenance Release Upgrade are TigerGraph Operator 1.5.0 and TigerGraph 4.2.0.
> Both conditions must be met simultaneously. If these requirements are not satisfied, the upgrade process will behave as it does in earlier versions.

## Troubleshooting

### How to proceed with the upgrade process if the upgrade pre-check job fails due to incorrect image or downgrade error

During the upgrade process, the cluster status will be updated to "Upgrade: False" if the upgrade pre-check job fails.

```bash
kubectl get tg -n tigergraph

NAME           REPLICAS   CLUSTER-SIZE   CLUSTER-HA   CLUSTER-VERSION                            SERVICE-TYPE   REGION-AWARENESS   CONDITION-TYPE   CONDITION-STATUS   AGE
test-cluster   4          4              2            tigergraph/tigergraph-k8s:4.2.0   LoadBalancer                      UpgradePre       False              33m
```

You can check the pod status of the upgrade pre-check job by running the following command:

```bash
kubectl get pods -n tigergraph

NAME                                                      READY   STATUS      RESTARTS   AGE
test-cluster-0                                            1/1     Running     0          29m
test-cluster-1                                            1/1     Running     0          31m
test-cluster-2                                            1/1     Running     0          32m
test-cluster-3                                            1/1     Running     0          34m
test-cluster-init-job-dklgc                               0/1     Completed   0          29m
test-cluster-upgrade-pre-job-4hnrw                        0/1     Error       0          61s
test-cluster-upgrade-pre-job-8m4rb                        0/1     Error       0          29s
test-cluster-upgrade-pre-job-x2bsk                        0/1     Error       0          50s
tigergraph-operator-controller-manager-66c7c475c4-9gd9m   2/2     Running     0          39m
```

If you find that the Pod status is ERROR, you can check the pod logs by running the following command:

```bash
kubectl logs test-cluster-upgrade-pre-job-4hnrw -n tigergraph

Warning: Permanently added '[test-cluster-0.test-cluster-internal-service.tigergraph]:10022,[10.64.2.11]:10022' (ECDSA) to the list of known hosts.
current version is 4.2.0 and want to downgrade to 4.1.2
error: downgrading is forbidden
```

From the error logs, the upgrade pre-check job failed due to a downgrade error.

To resolve this issue, you can either update the upgrade image to the correct version to proceed with the upgrade process or reset the image to the previous version.

Another possible failure occurs if the upgrade image is configured to a non-existing version. In this case, the pod for the pre-check job will enter an ErrImagePull or ImagePullBackOff state. To address this, you can correct the upgrade image or reset it to a valid version.

The example outputs are as follows:

```bash
$ kubectl get pods -n tigergraph -w
NAME                                                      READY   STATUS         RESTARTS   AGE
test-cluster-0                                            1/1     Running        0          36m
test-cluster-1                                            1/1     Running        0          38m
test-cluster-2                                            1/1     Running        0          40m
test-cluster-3                                            1/1     Running        0          41m
test-cluster-init-job-dklgc                               0/1     Completed      0          36m
test-cluster-upgrade-pre-job-m5pbk                        0/1     ErrImagePull   0          7s
tigergraph-operator-controller-manager-66c7c475c4-9gd9m   2/2     Running        0          46m
test-cluster-upgrade-pre-job-m5pbk                        0/1     ImagePullBackOff   0          16s
test-cluster-upgrade-pre-job-m5pbk                        0/1     ErrImagePull       0          32s
test-cluster-upgrade-pre-job-m5pbk                        0/1     ImagePullBackOff   0          43s
```

### How to proceed with the upgrade process if the upgrade pre-check fails Due to insufficient ephemeral local storage

Starting from TigerGraph Operator version 1.5.0, it's not allowed to set the filed `spec.resources.limit.ephemeral-storage` less than 20Gi, however, you may install TigerGraph cluster using Operator version before 1.5.0. In this case, if the `spec.resources.limit.ephemeral-storage` value is less than 20Gi, the upgrade pre-check job will fail because it needs more ephemeral storage space to copy the binaries of the upgrade version to the running cluster.

If you configure a small value for `spec.resources.limits.ephemeral-storage`, the upgrade pre-check job will fail with the following errors:

```bash
kubectl logs test-cluster-upgrade-pre-job-dpmlc -n tigergraph -f
Warning: Permanently added '[test-cluster-0.test-cluster-internal-service.tigergraph]:10022' (ED25519) to the list of known hosts.
It will upgrade from 4.1.2 to 4.2.0
client_input_hostkeys: received duplicated ssh-rsa host key
[Thu Jan 23 12:42:49 UTC 2025] info: start removing soft links to avoid scp errors
[Thu Jan 23 12:42:50 UTC 2025] start copying files to run pre upgrade check
client_input_hostkeys: received duplicated ssh-rsa host key
[Thu Jan 23 12:44:12 UTC 2025] info: finish copying files to run pre upgrade check
[Thu Jan 23 12:44:12 UTC 2025] info: Start creating a file as a flag to avoid duplicate copies
client_input_hostkeys: received duplicated ssh-rsa host key
[Thu Jan 23 12:44:13 UTC 2025] info: created a file as a sign to avoid duplicate copies successfully
client_input_hostkeys: received duplicated ssh-rsa host key
Hostname of pod is:
test-cluster-0
[Thu Jan 23 12:44:13 UTC 2025] info: JAVA_PATH is 4.2.0
[Thu Jan 23 12:44:13 UTC 2025] info: JAVA_DIR is /home/tigergraph/tigergraph/app/4.2.0/.syspre/usr/lib/jvm/jdk-17.0.12+7
[Thu Jan 23 12:44:13 UTC 2025] start executing pre upgrade check 4.1.2 4.2.0
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
tput: No value for $TERM and no -T specified
[PROGRESS]: 12:44:13 Running pre_upgrade_check_infra.sh 
[NOTE    ]: The nginx template will be updated to the new version. The current template will be backed up at: /home/tigergraph/tigergraph/app/4.1.2/nginx/nginx.conf.template.old 
[PROGRESS]: 12:44:13 Finished running pre_upgrade_check_infra.sh successfully 
[PROGRESS]: 12:44:13 Running pre_upgrade_check_engine.sh 
[PROGRESS]: 12:44:13 Finished running pre_upgrade_check_engine.sh successfully 
[PROGRESS]: 12:44:13 Running pre_upgrade_check_gsql.sh 
[PROGRESS]: 12:44:13 Checking gsql catalog directory file /home/tigergraph/tigergraph/app ... 
[PROGRESS]: 12:44:13 Pull dict ... 
Connection to test-cluster-0.test-cluster-internal-service.tigergraph closed by remote host.
[Thu Jan 23 12:44:15 UTC 2025] error: failed to perform pre-upgrade check
```

The SSH connection to m1 disconnects because m1 was killed and restarted by Kubernetes as the pod ephemeral local storage usage exceeds the total limit. To diagnose this issue, you can check the events when this error occurs. The relevant events are as follows:

```bash
kubectl get events --sort-by='.metadata.creationTimestamp' -A

tigergraph   85s         Warning   Evicted                       pod/test-cluster-0                                             Pod ephemeral local storage usage exceeds the total limit of containers 6Gi.
tigergraph   2m5s        Normal    Killing                       pod/test-cluster-0                                             Stopping container tigergraph
```

When this error occurs, you need to update `spec.resources.limits.ephemeral-storage` to a larger value. The recommended value is 20Gi, which is the required minimum starting from version 1.5.0. Additionally, you must reset the cluster version to previous version before you update the value of field `spec.resources.limits.ephemeral-storage`.

```YAML
resources:
  limits:
    ephemeral-storage: "20Gi"
```
