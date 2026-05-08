# How to Install the Operator and Deploy TigerGraph on Kubernetes Without Internet Access

- [How to Install the Operator and Deploy TigerGraph on Kubernetes Without Internet Access](#how-to-install-the-operator-and-deploy-tigergraph-on-kubernetes-without-internet-access)
  - [Prerequisites](#prerequisites)
  - [Procedure](#procedure)
    - [Transferring Docker Images and Helm Chart Package](#transferring-docker-images-and-helm-chart-package)
      - [TigerGraph Operator](#tigergraph-operator)
      - [Cert-manager](#cert-manager)
    - [Install Operator with kubectl-tg](#install-operator-with-kubectl-tg)
    - [Install Operator Using the Helm Command Locally](#install-operator-using-the-helm-command-locally)
    - [Deploy TigerGraph Cluster](#deploy-tigergraph-cluster)
  - [See Also](#see-also)

## Prerequisites

- Docker
- Private Docker registry
- Private Helm repository(Optional)

## Procedure

### Transferring Docker Images and Helm Chart Package

Ensure your environment has internet access before downloading the required Docker images and Helm chart packages.

For illustration, this guide uses TigerGraph cluster version `4.2.2` and TG K8s Operator version `1.7.1`. Adjust the versions as needed for your deployment.

#### TigerGraph Operator

**Required Docker Images:**

1. `tigergraph/tigergraph-k8s:4.2.2`
2. `tigergraph/tigergraph-k8s-operator:1.7.1`
3. `tigergraph/tigergraph-k8s-init:1.7.1`

**Steps:**

1. **Pull the images:**

    ```bash
    docker pull tigergraph/tigergraph-k8s:4.2.2
    docker pull tigergraph/tigergraph-k8s-operator:1.7.1
    docker pull tigergraph/tigergraph-k8s-init:1.7.1
    ```

2. **Export the images as a tar package:**

    ```bash
    docker save tigergraph/tigergraph-k8s:4.2.2 tigergraph/tigergraph-k8s-operator:1.7.1 tigergraph/tigergraph-k8s-init:1.7.1 > tigergraph-operator-images.tar
    ```

3. **Copy the tar file to your target machine and load the images:**

    ```bash
    docker load < tigergraph-operator-images.tar
    ```

4. **Tag the images for your private Docker registry:**

    ```bash
    export DOCKER_REPO=${YOUR_PRIVATE_DOCKER_REPO}
    docker tag tigergraph/tigergraph-k8s:4.2.2 ${DOCKER_REPO}/tigergraph-k8s:4.2.2
    docker tag tigergraph/tigergraph-k8s-operator:1.7.1 ${DOCKER_REPO}/tigergraph-k8s-operator:1.7.1
    docker tag tigergraph/tigergraph-k8s-init:1.7.1 ${DOCKER_REPO}/tigergraph/tigergraph-k8s-init:1.7.1
    ```

5. **Push the images to your private Docker registry:**

    ```bash
    docker push ${DOCKER_REPO}/tigergraph-k8s:4.2.2
    docker push ${DOCKER_REPO}/tigergraph-k8s-operator:1.7.1
    docker push ${DOCKER_REPO}/tigergraph-k8s-init:1.7.1
    ```

**Helm Chart Package (Private Helm Repo Required):**

If you plan to install the operator using `kubectl-tg`, a private Helm repository is required. If you do not have a private Helm repo and need to install the operator offline, refer to the section on local Helm chart installation.

1. **Download the Helm chart:**

    ```bash
    curl https://dl.tigergraph.com/charts/tg-operator-1.7.1.tgz -o tg-operator-1.7.1.tgz
    ```

2. **Upload the Helm chart to your private Helm repository:**

    ```bash
    export HELM_REPO=${YOUR_PRIVATE_HELM_REPO}
    export VERSION=1.7.1
    curl --request DELETE ${HELM_REPO}/api/charts/tg-operator/${VERSION}
    curl --data-binary "@charts/tg-operator-${VERSION}.tgz" ${HELM_REPO}/api/charts
    ```

#### Cert-manager

This example uses [cert-manager version 1.12.17](https://github.com/cert-manager/cert-manager/releases/download/v1.12.17/cert-manager.yaml).

**Transferring Cert-manager Docker Images:**

1. **Pull the images:**

    ```bash
    docker pull quay.io/jetstack/cert-manager-cainjector:v1.12.17
    docker pull quay.io/jetstack/cert-manager-controller:v1.12.17
    docker pull quay.io/jetstack/cert-manager-webhook:v1.12.17
    ```

2. **Export the images as a tar package:**

    ```bash
    docker save quay.io/jetstack/cert-manager-cainjector:v1.12.17 quay.io/jetstack/cert-manager-controller:v1.12.17 quay.io/jetstack/cert-manager-webhook:v1.12.17 > cert-manager-images.tar
    ```

3. **Copy the tar file to your target machine and load the images:**

    ```bash
    docker load < cert-manager-images.tar
    ```

4. **Tag the images for your private Docker registry:**

    ```bash
    export DOCKER_REPO=${YOUR_PRIVATE_DOCKER_REPO}
    docker tag quay.io/jetstack/cert-manager-cainjector:v1.12.17 ${DOCKER_REPO}/cert-manager-cainjector:v1.12.17
    docker tag quay.io/jetstack/cert-manager-controller:v1.12.17 ${DOCKER_REPO}/cert-manager-controller:v1.12.17
    docker tag quay.io/jetstack/cert-manager-webhook:v1.12.17 ${DOCKER_REPO}/cert-manager-webhook:v1.12.17
    ```

5. **Push the images to your private Docker registry:**

    ```bash
    docker push ${DOCKER_REPO}/cert-manager-cainjector:v1.12.17
    docker push ${DOCKER_REPO}/cert-manager-controller:v1.12.17
    docker push ${DOCKER_REPO}/cert-manager-webhook:v1.12.17
    ```

**Modify the Cert-manager Manifest:**

1. **Download the cert-manager YAML resource:**

    ```bash
    curl -L "https://github.com/cert-manager/cert-manager/releases/download/v1.12.17/cert-manager.yaml" -o "cert-manager.yaml"
    ```

2. **Replace the public Docker image URLs with your private registry URLs:**

    - **On macOS:**

      ```bash
      sed -i '' "s|quay.io/jetstack/cert-manager-cainjector:v1.12.17|${DOCKER_REPO}/cert-manager-cainjector:v1.12.17|g" cert-manager.yaml
      sed -i '' "s|quay.io/jetstack/cert-manager-controller:v1.12.17|${DOCKER_REPO}/cert-manager-controller:v1.12.17|g" cert-manager.yaml
      sed -i '' "s|quay.io/jetstack/cert-manager-webhook:v1.12.17|${DOCKER_REPO}/cert-manager-webhook:v1.12.17|g" cert-manager.yaml
      ```

    - **On Linux:**

      ```bash
      sed -i "s|quay.io/jetstack/cert-manager-cainjector:v1.12.17|${DOCKER_REPO}/cert-manager-cainjector:v1.12.17|g" cert-manager.yaml
      sed -i "s|quay.io/jetstack/cert-manager-controller:v1.12.17|${DOCKER_REPO}/cert-manager-controller:v1.12.17|g" cert-manager.yaml
      sed -i "s|quay.io/jetstack/cert-manager-webhook:v1.12.17|${DOCKER_REPO}/cert-manager-webhook:v1.12.17|g" cert-manager.yaml
      ```

3. **Install cert-manager using your private Docker images:**

    ```bash
    kubectl apply -f cert-manager.yaml
    ```

---

### Install Operator with kubectl-tg

If your Docker registry requires authentication, specify a custom secret name using the `--image-pull-secret` option (default: `tigergraph-image-pull-secret`).  
Create the image pull secret in the target namespace before deploying your TG cluster.

```bash
export HELM_REPO=${YOUR_PRIVATE_HELM_REPO}
export DOCKER_REPO=${YOUR_PRIVATE_DOCKER_REPO}
kubectl tg init --namespace tigergraph --helm-repo ${HELM_REPO} --image-pull-secret yoursecret --docker-registry ${DOCKER_REPO}
```

---

### Install Operator Using the Helm Command Locally

**Steps:**

1. Download the Helm chart:

    ```bash
    curl https://dl.tigergraph.com/charts/tg-operator-1.7.1.tgz -o tg-operator-1.7.1.tgz
    ```

2. Extract the chart:

    ```bash
    tar xvf tg-operator-1.7.1.tgz
    ```

3. Navigate to the chart directory:

    ```bash
    cd tg-operator && tree
    ```

    Example output:

    ```bash
    .
    ├── Chart.yaml
    ├── crds
    │   └── tg-operator-crd.yaml
    ├── templates
    │   ├── NOTES.txt
    │   ├── _helpers.tpl
    │   ├── crd-upgrade
    │   │   ├── crd-job.yaml
    │   │   ├── crd-rbac.yaml
    │   │   └── crd-serviceaccount.yaml
    │   └── tg-operator.yaml
    └── values.yaml
    ```

4. Edit `values.yaml` to customize the operator configuration (e.g., Docker images, replicas, namespaces, resource limits).

    Example `values.yaml` snippet:

    ```yaml
    # Default values for tg-operator.
    replicas: 3
    image: tigergraph-k8s-operator
    jobImage: tigergraph-k8s-init
    pullPolicy: IfNotPresent
    imagePullSecret: tigergraph-image-pull-secret
    watchNameSpaces: ""
    clusterScope: true
    maxConcurrentReconcilesOfTG: 2
    maxConcurrentReconcilesOfBackup: 2
    maxConcurrentReconcilesOfBackupSchedule: 2
    maxConcurrentReconcilesOfRestore: 2
    resources:
      requests:
        cpu: 1000m
        memory: 1024Mi
      limits:
        cpu: 2000m
        memory: 4096Mi
    nodeSelector: null
    ```

5. Install the TigerGraph Operator:

    ```bash
    helm install tg-operator ./tg-operator -n tigergraph
    ```

6. Verify the installation:

    ```bash
    helm list -n tigergraph
    kubectl get pods -n tigergraph
    ```

    Example output:

    ```bash
    NAME                                                      READY   STATUS    RESTARTS   AGE
    tigergraph-operator-controller-manager-7cfc4476c7-692r4   2/2     Running   0          5m8s
    tigergraph-operator-controller-manager-7cfc4476c7-76msk   2/2     Running   0          5m8s
    tigergraph-operator-controller-manager-7cfc4476c7-k8425   2/2     Running   0          5m8s
    ```

---

### Deploy TigerGraph Cluster

If your Docker registry requires authentication, create an image pull secret and adjust the namespace as needed.

**Example secret definition:**

```yaml
apiVersion: v1
data:
  .dockerconfigjson: ******************************************
kind: Secret
metadata:
  name: tigergraph-image-pull-secret
  namespace: tigergraph
type: kubernetes.io/dockerconfigjson
```

**Create a private SSH key secret (Operator 0.0.7+ required):**

```bash
echo -e 'y\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''
kubectl create secret generic ssh-key-secret --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace tigergraph
```

**Deploy the TigerGraph cluster with a specific Docker registry:**

```bash
export DOCKER_REGISTRY=${YOUR_DOCKER_REGISTRY}
kubectl tg create --cluster-name test001 --namespace tigergraph --private-key-secret ssh-key-secret --docker-registry ${DOCKER_REGISTRY} \
  -s 6 --ha 2 --version TG_CLUSTER_VERSION \
  --storage-class YOUR_STORAGE_CLASS_NAME --storage-size 100G
```

**Alternatively, modify the TigerGraph manifest directly:**

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: ${YOUR_DOCKER_REGISTRY}/tigergraph-k8s:4.2.2
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  ha: 2
  license: xxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
  resources:
    requests:
      cpu: "6"
      memory: 12Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 100G
      storageClassName: standard
      volumeMode: Filesystem
```

---

## See Also

For more details on managing the operator in other environments, refer to:

- [How to upgrade TigerGraph Kubernetes Operator](../04-manage/operator-upgrade.md)
- [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md)
- [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md)
- [Deploy TigerGraph on Red Hat OpenShift](../03-deploy/tigergraph-on-openshift.md)
- [Deploy TigerGraph on Azure Kubernetes Service (AKS)](../03-deploy/tigergraph-on-aks.md)
- [Deploy TigerGraph Operator on K8s using Helm](../03-deploy/deploy-operator-with-helm.md)
