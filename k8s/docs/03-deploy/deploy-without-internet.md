# How to install Operator and deploy TG on K8s without internet access

- [How to install Operator and deploy TG on K8s without internet access](#how-to-install-operator-and-deploy-tg-on-k8s-without-internet-access)
  - [Prerequisites](#prerequisites)
  - [Procedure](#procedure)
    - [Transferring Docker Images and Helm Chart Package](#transferring-docker-images-and-helm-chart-package)
      - [TigerGraph Operator](#tigergraph-operator)
      - [Cert-manager](#cert-manager)
    - [Install Operator with kubect-tg](#install-operator-with-kubect-tg)
    - [Install Operator using the helm command to install it locally](#install-operator-using-the-helm-command-to-install-it-locally)
    - [Deploy TG cluster](#deploy-tg-cluster)

## Prerequisites

- Docker

- Private Docker registry

- Private helm repo

## Procedure

### Transferring Docker Images and Helm Chart Package

Please ensure that your environment has internet access before proceeding with the download of these docker images and helm chart packages.

For illustrative purposes, we will utilize TG cluster version 3.9.2 and TG K8s Operator version 0.0.7. Kindly make the necessary adjustments based on your specific version.

#### TigerGraph Operator

- Docker images

1. tigergraph/tigergraph-k8s:3.9.2

2. tigergraph/tigergraph-k8s-operator:0.0.7

3. tigergraph/tigergraph-k8s-init:0.0.7

```bash
docker pull tigergraph/tigergraph-k8s:3.9.2
docker pull tigergraph/tigergraph-k8s-operator:0.0.7
docker pull tigergraph/tigergraph-k8s-init:0.0.7

docker save tigergraph/tigergraph-k8s:3.9.2 tigergraph/tigergraph-k8s-operator:0.0.7 tigergraph/tigergraph-k8s-init:0.0.7  > tigergraph-operator-images.tar

# copy the docker images tar files to your target machine before loading
docker load < tigergraph-operator-images.tar
# replace it to your private DOCKER_REPO
export DOCKER_REPO=docker.io/internal
docker tag tigergraph/tigergraph-k8s:3.9.2 ${DOCKER_REPO}/tigergraph-k8s:3.9.2
docker tag tigergraph/tigergraph-k8s-operator:0.0.7 ${DOCKER_REPO}/tigergraph-k8s-operator:0.0.7
docker tag tigergraph/tigergraph-k8s-init:0.0.7 ${DOCKER_REPO}/tigergraph/tigergraph-k8s-init:0.0.7

# push them to your private docker repo
docker push ${DOCKER_REPO}/tigergraph-k8s:3.9.2
docker push ${DOCKER_REPO}/tigergraph-k8s-operator:0.0.7
docker push ${DOCKER_REPO}/tigergraph-k8s-init:0.0.7
```

- Helm chart package (private helm repo required)

If the goal is to install the operator using kubectl-tg, having a private Helm repository is crucial. If such a repository is unavailable and you aim to install an operator without internet connectivity, refer to the section that outlines the procedure for installing the Helm chart locally.

```bash
# Dowload the helm chart package from TG public repo
curl https://dl.tigergraph.com/charts/tg-operator-0.0.7.tgz -o tg-operator-0.0.7.tgz

# 
#    mkdir -p /tmp/charts
#    chmod 0777 /tmp/charts
#    docker run -d \
#      -p 8383:8080 \
#      -e DEBUG=1 \
#      -e STORAGE=local \
#      -e STORAGE_LOCAL_ROOTDIR=/charts \
#      -v /tmp/charts:/charts \
#      --name ${helm_repo_name} ghcr.io/helm/chartmuseum:v0.13.1
# replace the HELM_REPO to your own one, the following steps will take chartmuseum as an example.
export HELM_REPO=http://127.0.0.1:8383
# upload the helm chart package to your private helm repo
curl --request DELETE ${HELM_REPO}/api/charts/tg-operator/${VERSION}
curl --data-binary "@charts/tg-operator-${VERSION}.tgz" ${HELM_REPO}/api/charts
```

- Install Operator and deploy TG cluster with private docker repo and helm repo

#### Cert-manager

The following examples suppose you are going to use cert-manager 1.8.0 version

- Transferring the cert-manager Docker images to your private Docker registry

```bash
# curl https://github.com/cert-manager/cert-manager/releases/download/v1.8.0/cert-manager.yaml
# quay.io/jetstack/cert-manager-cainjector:v1.8.0
# quay.io/jetstack/cert-manager-controller:v1.8.0
# quay.io/jetstack/cert-manager-webhook:v1.8.0
docker pull quay.io/jetstack/cert-manager-cainjector:v1.8.0
docker pull quay.io/jetstack/cert-manager-controller:v1.8.0
docker pull quay.io/jetstack/cert-manager-webhook:v1.8.0

docker save quay.io/jetstack/cert-manager-cainjector:v1.8.0 quay.io/jetstack/cert-manager-controller:v1.8.0 quay.io/jetstack/cert-manager-webhook:v1.8.0  > cert-manager-images.tar

# copy the docker images tar files to your target machine before loading
docker load < cert-manager-images.tar

# replace it to your private DOCKER REPO
export DOCKER_REPO=docker.io/internal
docker tag quay.io/jetstack/cert-manager-cainjector:v1.8.0 ${DOCKER_REPO}/cert-manager-cainjector:v1.8.0
docker tag quay.io/jetstack/cert-manager-controller:v1.8.0 ${DOCKER_REPO}/cert-manager-controller:v1.8.0
docker tag quay.io/jetstack/cert-manager-webhook:v1.8.0 ${DOCKER_REPO}/cert-manager-webhook:v1.8.0

# push them to your private docker repo
docker push ${DOCKER_REPO}/cert-manager-cainjector:v1.8.0
docker push ${DOCKER_REPO}/cert-manager-controller:v1.8.0
docker push ${DOCKER_REPO}/cert-manager-webhook:v1.8.0
```

- Modify the manifests of cert-manager according to your docker registry

```bash
curl -L "https://github.com/cert-manager/cert-manager/releases/download/v1.8.0/cert-manager.yaml" -o "cert-manager.yaml"

# edit cert-manager.yaml, change the following lines which including cert-manager images
quay.io/jetstack/cert-manager-cainjector:v1.8.0 -> ${DOCKER_REPO}/cert-manager-cainjector:v1.8.0
quay.io/jetstack/cert-manager-cainjector:v1.8.0 -> ${DOCKER_REPO}/cert-manager-controller:v1.8.0
quay.io/jetstack/cert-manager-cainjector:v1.8.0 -> ${DOCKER_REPO}/cert-manager-webhook:v1.8.0

# Install the cert-manager with your private docker 
kubectl apply -f cert-manager.yaml
```

### Install Operator with kubect-tg

In scenarios where your Docker registry necessitates authentication, you can specify a custom secret name using the `--image-pull-secret` option. The default secret name is `tigergraph-image-pull-secret`.

Furthermore, it's imperative to create the image pull secret within the designated namespace before initiating the deployment of your TG cluster.

```bash
# please make sure the HELM_REPO and DOCKER_REPO is correct
export HELM_REPO=http://127.0.0.1:8383
export DOCKER_REPO=docker.io/internal
kubectl tg init --namespace tigergraph --helm-repo ${HELM_REPO} --image-pull-secret yoursecret --docker-registry ${DOCKER_REPO}
```

### Install Operator using the helm command to install it locally

Please follow these steps to install a Helm chart:

1. Download the Helm chart you want to install.

2. Extract the chart to a directory on your local machine.

3. Open a terminal window and navigate to the directory where you extracted the chart.

4. Modify the default configuration of Operator by editing `values.yaml`.

5. Run the following command to install the chart:

Customize the operator configuration via values.yaml, we should change the image filed to your internal docker repo.

```bash
# Default values for tg-operator.
# This is a YAML-formatted file.
# Declare variables to be passed into your templates.

# Default values for deployment replicas of operator
replicas: 3
# image is the docker image of operator
image: tigergraph-k8s-operator
# jobImange is the docker image of cluster operation(int, upgrade, scale and so on) job
jobImage: tigergraph-k8s-init
pullPolicy: IfNotPresent
# imagePullSecret is the docker image pull secret of operator
imagePullSecret: tigergraph-image-pull-secret
# watchNameSpaces are the namespaces which operator watch, multiple namespaces separated by comma, empty indicates watch all namespaces
watchNameSpaces: ""
# clusterScope is whether the operator has ClusterRole
clusterScope: true
# resources are resources reqeusts configuration of operator
resources:
  requests:
    cpu: 1000m
    memory: 1024Mi
  limits:
    cpu: 2000m
    memory: 4096Mi
```

Install Operator using helm

```bash
curl https://dl.tigergraph.com/charts/tg-operator-0.0.7.tgz -o tg-operator-0.0.7.tgz
tar xvf tg-operator-0.0.7.tgz

$ cd tg-operator
$ tree
.
├── Chart.yaml
├── crds
│   └── tg-operator-crd.yaml
├── templates
│   ├── NOTES.txt
│   ├── _helpers.tpl
│   └── tg-operator.yaml
└── values.yaml

# before you install the Operator, you may need to modify the configuration in values.yaml
# such as docker images, replicas, watchnamesapce, resources limits and so on
$ helm install tg-operator ./tg-operator -n tigergraph
NAME: tg-operator
LAST DEPLOYED: Tue Mar 14 06:58:41 2023
NAMESPACE: tigergraph
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:

$ helm list -n tigergraph
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                   APP VERSION
tg-operator     tigergraph      1               2023-03-14 06:58:41.849883727 +0000 UTC deployed        tg-operator-0.0.7

# make sure the deployment of Operator is running now
kubectl get pods -n tigergraph
NAME                                                      READY   STATUS    RESTARTS   AGE
tigergraph-operator-controller-manager-7cfc4476c7-692r4   2/2     Running   0          5m8s
tigergraph-operator-controller-manager-7cfc4476c7-76msk   2/2     Running   0          5m8s
tigergraph-operator-controller-manager-7cfc4476c7-k8425   2/2     Running   0          5m8s   
```

### Deploy TG cluster

If your Docker registry necessitates authentication, you need to create an image pull secret. Please make the necessary adjustments to the namespace based on your environment.

Here's the secret definition you can use as a reference, please make the necessary adjustments to the namespace based on your environment.:

```bash
apiVersion: v1
data:
  .dockerconfigjson: ******************************************
kind: Secret
metadata:
  name: tigergraph-image-pull-secret
  namespace: tigergraph
type: kubernetes.io/dockerconfigjson
```

Create a private ssh key secret(Required operator 0.0.7 and later)

```bash
echo -e 'y\\n' | ssh-keygen -b 4096 -t rsa -f $HOME/.ssh/tigergraph_rsa -q -N ''

kubectl create secret generic ssh-key-secret --from-file=private-ssh-key=$HOME/.ssh/tigergraph_rsa --from-file=public-ssh-key=$HOME/.ssh/tigergraph_rsa.pub --namespace tigergraph
```

Deploy TG cluster with specific docker registry

```bash
  # please make sure the DOCKER_REGISTRY is correct, the following is just an expamle.
  export DOCKER_REGISTRY=docker.io/internal
  kubectl tg create --cluster-name test001 --namespace tigergraph --private-key-secret ssh-key-secret --docker-registry  ${DOCKER_REGISTRY} \
    -s 6 --ha 2 --version TG_CLUSTER_VERSION \
    --storage-class YOUR_STORAGE_CLASS_NAME --storage-size 100G
```

We can also modify the TigerGraph manifest to deploy the TG cluster directly

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  image: ${DOCKER_REGISTRY}/tigergraph-k8s:3.9.2
  imagePullPolicy: Always
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 2
  license: xxxxxxxxxxxx
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    requests:
      cpu: "2"
      memory: 8Gi
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
