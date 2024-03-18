# Use InitContainers,SidecarContainers and CustomVolumes in kubectl-tg

- [Use InitContainers,SidecarContainers and CustomVolumes in kubectl-tg](#use-initcontainerssidecarcontainers-and-customvolumes-in-kubectl-tg)
  - [Basic knowledge](#basic-knowledge)
  - [Usage](#usage)
    - [Creating initContainers,sidecarContainers and customVolumes](#creating-initcontainerssidecarcontainers-and-customvolumes)
    - [Removing initContainers/sidecarContainers/customVolumes](#removing-initcontainerssidecarcontainerscustomvolumes)
    - [Managing a TG Cluster with Custom Containers](#managing-a-tg-cluster-with-custom-containers)

## Basic knowledge

A K8S Pod has the capability to house multiple containers, including both init containers and app containers. Upon pod creation, the init containers execute sequentially in a designated order. Should any of the init containers encounter a failure, the overall pod execution is halted (for more insights, consult [Init Containers](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/)). Following the successful completion of all init containers, the app containers proceed to run concurrently.

By default, in the configuration of the TigerGraph CR, each TigerGraph Pod features a singular app container named "tigergraph". This container runs all TigerGraph services within the Pod. The functionality "InitContainers,SidecarContainers and CustomVolumes" empowers users to seamlessly integrate personalized initContainers and sidecarContainers into TigerGraph Pods. Furthermore, users can create customVolumes, enabling the mounting of these volumes within their initContainers or sidecarContainers.

To grasp the concepts of InitContainers, Sidecar Containers, and Custom Volumes, please refer to the guide on [InitContainers, Sidecar Containers, and Custom Volumes](./custom-containers.md).

## Usage

### Creating initContainers,sidecarContainers and customVolumes

To make use of this feature, follow these steps:

1. Prepare a YAML file that includes the definitions for your `initContainers`, `sidecarContainers`, and `customVolumes`. This YAML file will be passed to the `--custom-containers` option.

   - For `initContainers` and `sidecarContainers`, you can refer to the Kubernetes documentation at [Container](https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/pod-v1/#Container) for available fields and configurations.

   - For `customVolumes`, the relevant fields and configurations can be found in the Kubernetes documentation at [Volumes](https://kubernetes.io/docs/concepts/storage/volumes/).

Below is an illustrative example of a custom container YAML file:

```yaml
initContainers:
  - image: alpine:3.17.2
    name: init-hello
    args:
      - /bin/sh
      - -c
      - echo hello

sidecarContainers:
  - image: alpine:3.17.2
    name: main-container
    args:
      - /bin/sh
      - -c
      - >
        while true; do
          echo "$(date) INFO hello from main-container" >> /var/log/myapp.log ;
          sleep 1;
        done
    volumeMounts:
    - name: tg-log
      mountPath: /var/tglog
    - name: log
      mountPath: /var/log
    readinessProbe:
      exec:
        command:
        - sh
        - -c
        - if [[ -f /var/log/myapp.log ]];then exit 0; else exit 1;fi
      initialDelaySeconds: 10
      periodSeconds: 5

  - name: sidecar-container
    image: alpine:3.17.2
    args:
      - /bin/sh
      - -c
      - tail -fn+1 /var/log/myapp.log
    volumeMounts:
    - name: log
      mountPath: /var/log
```

Below is an illustrative example of a custom volume YAML file:

```YAML
customVolumes:
  - name: log
    emptyDir: {}
```

This comprehensive example showcases the configuration of `initContainers`, `sidecarContainers`, and a `customVolume` named "log". Adjust the contents according to your specific use case.

Name the above YAML file as `tg-custom-container.yaml`. To create a new cluster using the `tg-custom-container.yaml`  and `tg-custom-volume.yaml` file:

```bash
kubectl tg  create --cluster-name test-cluster --namespace ${NAMESPACE} \
  --size 3 --ha 2 -k ssh-key-secret --version ${TG_CLUSTER_VERSION} \
  --storage-class standard --storage-size 10G -l ${LICENSE} \
  --custom-containers tg-custom-container.yaml --custom-volumes tg-custom-volume.yaml
```

If you already have a cluster, and you want to add/update initContainers/sidecarContainers for it, you can run

```bash
kubectl tg update --cluster-name test-cluster --namespace ${NAMESPACE} \
  --custom-containers tg-custom-container.yaml --custom-volumes tg-custom-volume.yaml
```

### Removing initContainers/sidecarContainers/customVolumes

To remove all of them, you can pass an empty file as an argument to the `--custom-containers` option:

```bash
touch empty.yaml

kubectl tg update --cluster-name test-cluster --namespace ${NAMESPACE} \
  --custom-containers empty.yaml --custom-volumes empty.yaml
```

If you wish to remove specific containers or volumes, simply edit your configuration file and then use the `kubectl tg update` command to apply the changes.

### Managing a TG Cluster with Custom Containers

Operating a TG cluster with custom containers is similar to managing a cluster without custom containers. You can utilize the `kubectl tg update` command to perform actions such as updating, upgrading, expanding, or shrinking the cluster.

If you need to modify your `initContainers`, `sidecarContainers`, or `customVolumes`, follow these steps:

1. Make the necessary adjustments to your custom container and custom volume YAML file.

2. Execute the following command to update the cluster with the new custom containers:

```bash
kubectl tg update --cluster-name test-cluster --namespace ${NAMESPACE} --custom-containers tg-custom-container.yaml --custom-volumes tg-custom-volume.yaml
```

This command triggers a rolling update, ensuring that your custom containers are seamlessly updated within the cluster.
