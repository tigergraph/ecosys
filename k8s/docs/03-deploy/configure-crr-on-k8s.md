# Configure Cross-Region Replication on Kubernetes

Starting with version 1.5.0, TigerGraph Operator supports configuring Cross-Region Replication on Kubernetes.

This guide will walk you through configuring Cross-Region Replication (CRR) on Kubernetes.
To set up CRR, you set up **Disaster Recovery (DR) clusters** that sync with the **primary cluster**.
There are no restrictions on the environment in which the DR clusters and primary cluster are deployed,
you can deploy them on Kubernetes or Non-k8s environments（e.g. AWS EC2, GCP VM, etc.）.
So there are many different scenarios for CRR, please check the following table:

| Scenario | Primary Cluster | DR Cluster | Note |
| --- | --- | --- | --- |
| Both Primary and DR Clusters are on non-k8s environment | non-k8s | non-k8s | You don't need to read this document, just refer to [TigerGraph CRR documentation](https://docs.tigergraph.com/tigergraph-server/4.1/cluster-and-ha-management/crr-index) |
| Both Primary and DR Clusters are on a **same Kubernetes cluster** | k8s | the same k8s cluster as Primary cluster  | Please create a Primary cluster by following [Create a Primary Cluster without External Kafka Listeners](#create-a-primary-cluster-without-external-kafka-listeners) |
| Primary Cluster is on a Kubernetes cluster, DR Clusters are on **different Kubernetes clusters** or non-k8s environment | k8s | different k8s clusters/non-k8s | Please create a Primary cluster by following [Create a Primary Cluster with External Kafka Listeners](#create-a-primary-cluster-with-external-kafka-listeners) |

We won't talk about the details of CRR in this document, but only focus on how to configure CRR on Kubernetes.
To know more about CRR, please refer to the [TigerGraph CRR documentation](https://docs.tigergraph.com/tigergraph-server/4.1/cluster-and-ha-management/crr-index).

- [Configure Cross-Region Replication on Kubernetes](#configure-cross-region-replication-on-kubernetes)
  - [Create a Primary Cluster on Kubernetes](#create-a-primary-cluster-on-kubernetes)
    - [Create a Primary Cluster without External Kafka Listeners](#create-a-primary-cluster-without-external-kafka-listeners)
      - [Get the Addresses of Kafka brokers of the Primary Cluster](#get-the-addresses-of-kafka-brokers-of-the-primary-cluster)
    - [Create a Primary Cluster with External Kafka Listeners](#create-a-primary-cluster-with-external-kafka-listeners)
      - [Use LoadBalancer Service to Expose Kafka External Listeners](#use-loadbalancer-service-to-expose-kafka-external-listeners)
      - [Use Ingress to Expose Kafka External Listeners](#use-ingress-to-expose-kafka-external-listeners)
        - [Step 1: Install an Ingress Controller](#step-1-install-an-ingress-controller)
        - [Step 2: Create a DNS record and resolve the base domain to the Ingress Controller](#step-2-create-a-dns-record-and-resolve-the-base-domain-to-the-ingress-controller)
        - [Step 3: Generate private key and certificate for your kafka brokers](#step-3-generate-private-key-and-certificate-for-your-kafka-brokers)
        - [Step 4: Create a Primary Cluster with External Kafka Listeners and KafkaSecurity](#step-4-create-a-primary-cluster-with-external-kafka-listeners-and-kafkasecurity)
      - [Configure the Security of Kafka Brokers](#configure-the-security-of-kafka-brokers)
        - [Enable SSL for Kafka brokers](#enable-ssl-for-kafka-brokers)
        - [Enable Client Authentication for Kafka brokers](#enable-client-authentication-for-kafka-brokers)
      - [Get the Addresses of Kafka External Listeners of the Primary Cluster](#get-the-addresses-of-kafka-external-listeners-of-the-primary-cluster)
    - [Configure `KafkaConfig` by `kubectl-tg`](#configure-kafkaconfig-by-kubectl-tg)
  - [Set up DR Clusters](#set-up-dr-clusters)
    - [Create a Backup CR to backup the primary cluster](#create-a-backup-cr-to-backup-the-primary-cluster)
    - [Get the addresses of kafka brokers of the primary cluster](#get-the-addresses-of-kafka-brokers-of-the-primary-cluster-1)
    - [Set up CRR](#set-up-crr)
      - [Step 1: configure Primary IP and Port](#step-1-configure-primary-ip-and-port)
      - [Step 2\[Optional\]: configure Kafka Security on DR cluster](#step-2optional-configure-kafka-security-on-dr-cluster)
      - [Step 3\[Optional\]: configure restore for DR cluster](#step-3optional-configure-restore-for-dr-cluster)
      - [Step 4: set up CRR](#step-4-set-up-crr)
  - [Update Kafka Config](#update-kafka-config)
  - [Demo](#demo)

## Create a Primary Cluster on Kubernetes

When you want to set up DR clusters that are in the same Kubernetes cluster as the primary cluster, you don't need to configure External Kafka Listeners.
Because in the same Kubernetes cluster, the DR clusters can directly access Kafka brokers of the primary cluster by using the headless service.

If you want to set up DR clusters that are in different Kubernetes clusters or non-k8s environments, you will need to configure External Kafka Listeners for the primary cluster.
That means the Kafka brokers of the primary cluster should be accessible from outside the Kubernetes cluster. Currently we support two ways to expose the Kafka brokers to the external network: LoadBalancer Service and Ingress.
As you expose the Kafka brokers to the external network, you may consider the security of the Kafka brokers. We also provide a field to configure the Kafka brokers' security in TigerGraph CR.

### Create a Primary Cluster without External Kafka Listeners

When you just want to set up DR clusters that are in the **same Kubernetes cluster** as the primary cluster, you don't need to configure External Kafka Listeners. Just follow the [Get Started](../01-get-started/get_started.md) guide to create a cluster.

Here is a simple CR to create a primary cluster without External Kafka Listeners:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: pr-cluster
spec:
  ha: 1
  image: docker.io/tigergraph/tigergraph-k8s:4.2.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: YOUR_LICENSE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    requests:
      cpu: "4"
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
```

#### Get the Addresses of Kafka brokers of the Primary Cluster

Please run the following command to get the addresses of Kafka brokers of the primary cluster:

```bash
# Replace the namespace and cluster name with your own
CLUSTER_NAME=pr-cluster
NAMESPACE=tigergraph
# get the addresses of Kafka brokers, you can copy the output and use it in the DR cluster configuration
kubectl exec -ti ${CLUSTER_NAME}-0 -c tigergraph -n ${NAMESPACE} -- /home/tigergraph/tigergraph/app/cmd/gadmin config get System.HostList |  jq -r 'map(.Hostname) | join(",")'
# get the port of Kafka brokers
kubectl exec ${CLUSTER_NAME}-0 -c tigergraph -n ${NAMESPACE} -- /home/tigergraph/tigergraph/app/cmd/gadmin config get Kafka.Port
```

The output will be like:

```text
pr-cluster-0.pr-cluster-internal-service.tigergraph,pr-cluster-1.pr-cluster-internal-service.tigergraph,pr-cluster-2.pr-cluster-internal-service.tigergraph
30002
```

You can copy the output and use it in the DR cluster configuration.

### Create a Primary Cluster with External Kafka Listeners

> [!IMPORTANT]
> This feature is only supported in TigerGraph 4.2.0 and later versions.
> For TigerGraph 4.1.x and earlier versions, you can only set up DR clusters that are in the same Kubernetes cluster as the primary cluster.

If you want to set up DR clusters that are in different Kubernetes clusters or non-k8s environments, you will need to configure External Kafka Listeners for the primary cluster. We support two ways to expose the Kafka brokers to the external network: LoadBalancer Service and Ingress. They have different use cases and you can choose the one that fits your scenario. You can check the following table to choose the right way to expose Kafka brokers:

| | Ingress | LoadBalancer |
| --- | --- | --- |
| The addresses of kafka brokers | A base domain should be configured in CR. `broker${i}.${baseDomain}` will be the address of broker i. From the perspective of the DR cluster, **the addresses are fixed**. | The addresses are random strings. They will be generated by the LoadBalancer Service. From the perspective of the DR cluster, **the addresses are dynamic**. |
| Are additional configurations needed except for the CR | **Yes**, you have to <br> 1. Install an Ingress Controller in the Kubernetes cluster and enable SSL Passthrough<br> 2. Create a DNS record and resolve baseDomain to the Ingress Controller. | **No**, the addresses of LoadBalancer will be assigned automatically. |
| What will happen when a service is deleted and recreated | If the address of the ingress controller is stable, you don't need to do anything when the ingresses are deleted and recreated. | If the LoadBalancer Service is deleted and recreated, the addresses of the LoadBalancer will be changed.  From the perspective of the DR cluster, you will have to change the configuration of primary kafka IPs in the DR cluster configuration. |
| Is SSL required | **Yes**, if you want to use Ingress you have to enable SSL. This also requires a server certificate. If you set Ingress for KafkaExternalListener without SSL enabled, the CR will be rejected by webhook. | No, when you use LoadBalancer, SSL is optional. |

In summary, the configurations of Ingress are more complex and have more restrictions. But stable addresses can be provided for kafka brokers. The LoadBalancer configurations are simple, but it cannot guarantee stable addresses. When CRR is enabled, if the address of the LoadBalancer changes, the DR cluster will not be able to synchronize with the primary cluster.

#### Use LoadBalancer Service to Expose Kafka External Listeners

Here is an example of a CR to create a primary cluster with External Kafka Listeners exposed by LoadBalancer Service:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: pr-cluster
spec:
  ha: 1
  image: docker.io/tigergraph/tigergraph-k8s:4.2.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: YOUR_LICENSE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    requests:
      cpu: "4"
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
  kafkaConfig:
    externalListener:
      type: LoadBalancer
      # Optional. You can configure labels of the LoadBalancer Service
      labels:
        cluster: pr-cluster
      # Optional. You can configure annotations of the LoadBalancer Service
      annotations:
        annotation1: value1
      # Optional. You can configure the externalTrafficPolicy
      externalTrafficPolicy: Local
      # Optional. You can configure the servicePort, the default value is 30006.
      # 30002 is not allowed because it is used by the internal Kafka service.
      servicePort: 30006
```

When you create a cluster with the above CR, X LoadBalancer Services will be created to expose the Kafka External Listeners(X is the value of `replicas` in the CR).
The addresses of the Kafka brokers will be the addresses of the LoadBalancer Services.

#### Use Ingress to Expose Kafka External Listeners

As mentioned in the previous section, configuring Ingress is more complex than LoadBalancer. You have to install an Ingress Controller in the Kubernetes cluster and enable SSL Passthrough. You also have to create a DNS record and resolve the base domain to the Ingress Controller. Please follow the following steps to configure Ingress.

##### Step 1: Install an Ingress Controller

Here we take Nginx Ingress Controller as an example. You can also use other Ingress Controllers, but you should make sure that they support SSL Passthrough. About the details of SSL Passthrough, you can refer to the document of [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/user-guide/tls/#ssl-passthrough).

We have prepared a YAML file to install Nginx Ingress Controller and enable SSL Passthrough in our sample directory. You can find it here [nginx-ingress-controller.yaml](../09-samples/deploy/nginx-ingress-controller.yaml). Please download this file and run the following command to install Nginx Ingress Controller:

```bash
kubectl apply -f nginx-ingress-controller.yaml
```

> [!NOTE]
> This sample YAML file will install Nginx Ingress Controller v1.11.3 and enable SSL Passthrough.
> If you want to install a different version, please refer to the [official document](https://kubernetes.github.io/ingress-nginx/deploy/) to install the Ingress Controller,
> and then refer to [this section](https://kubernetes.github.io/ingress-nginx/user-guide/tls/#ssl-passthrough) to enable SSL Passthrough.

After the Ingress Controller is installed, you can get the address of the Ingress Controller by running the following command:

```bash
kubectl get svc ingress-nginx-controller  -n ingress-nginx
```

The output will be like:

```text
NAME                       TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)                      AGE
ingress-nginx-controller   LoadBalancer   34.118.236.161   34.31.217.219   80:31346/TCP,443:30511/TCP   89s
```

The `EXTERNAL-IP` of service `ingress-nginx-controller` is the address of the Ingress Controller.

##### Step 2: Create a DNS record and resolve the base domain to the Ingress Controller

After installing an Ingress controller in your cluster and getting the address of the Ingress Controller,
you need to create a DNS record and resolve the **base domain** to the Ingress Controller.
If the address of the ingress controller is an IP address, please create a DNS record like:

```text
Type: A
Host: *.kafka.your-domain.com
Value: 34.31.217.219
```

If you are using EKS, the address of the Ingress Controller may be a domain name. In this case, you can create a CNAME record like:

```text
Type: CNAME
Host: *.kafka.your-domain.com
Value: a1b2c3d4e5f6g7.here.amazonaws.com
```

Once you configure the DNS record, you can use the base domain in the CR to expose the Kafka External Listeners, in step 4 we will show you how to do it.

##### Step 3: Generate private key and certificate for your kafka brokers

Since exposing Kafka brokers by Ingress requires SSL, you need to generate a private key and certificate for your Kafka brokers. Here we provide the simplest example that generates a self-signed certificate as the root certificate, and then generates a certificate signed by the root certificate as the server certificate. You can also use a certificate signed by a CA.

First, let's set following environment variables:

```bash
# the password of the private key
export PASSPHRASE="your_secure_password"
# paths of the generated files
export ROOT_KEY_PATH="rootCA.key"
export ROOT_CERT_PATH="rootCA.crt"
export SERVER_KEY_PATH="server.key"
export SERVER_CSR_PATH="server.csr"
export SERVER_CERT_PATH="server.crt"
export CERT_CHAIN_PATH="cert_chain.pem"
# the namespace and name of the Secret that we want to create on K8s
export NAMESPACE="tigergraph"
export SECRET_NAME="kafka-secret"
```

Second, generate root key and certificate:

```bash
# generate root key
# Note: the command is different between openssl 1.x.x and openssl 3.x.x,
# if you are using openssl 3.x.x, you should add an option `-traditional`
# For openssl 1.x.x
openssl genrsa -aes256 -passout pass:$PASSPHRASE -out $ROOT_KEY_PATH 4096 
# For openssl 3.x.x
openssl genrsa -traditional -aes256 -passout pass:$PASSPHRASE -out $ROOT_KEY_PATH 4096 

# generate root certificate
openssl req -x509 -new -key $ROOT_KEY_PATH -sha256 -days 3650 \
  -passin pass:$PASSPHRASE \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=MyCompany/OU=IT/CN=MyRootCA" \
  -out $ROOT_CERT_PATH
```

Then generate server key and certificate:

```bash
# generate server key
# For openssl 1.x.x
openssl genrsa -aes256 -passout pass:$PASSPHRASE -out $SERVER_KEY_PATH 2048
# For openssl 3.x.x
openssl genrsa -traditional -aes256 -passout pass:$PASSPHRASE -out $SERVER_KEY_PATH 2048

# generate server csr
openssl req -new -key $SERVER_KEY_PATH -passin pass:$PASSPHRASE \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=MyCompany/OU=IT/CN=myserver.local" \
  -out $SERVER_CSR_PATH

# generate server certificate
openssl x509 -req -in $SERVER_CSR_PATH -CA $ROOT_CERT_PATH -CAkey $ROOT_KEY_PATH \
  -CAcreateserial -out $SERVER_CERT_PATH -days 365 -sha256 \
  -passin pass:$PASSPHRASE

# concatenate the server certificate and the root certificate to form the certificate chain
cat $SERVER_CERT_PATH $ROOT_CERT_PATH > $CERT_CHAIN_PATH

# verify the certificate chain
openssl verify -CAfile $ROOT_CERT_PATH $SERVER_CERT_PATH
```

Finally, create a Secret on Kubernetes to store the private key and certificate chain:

```bash
kubectl create secret generic "$SECRET_NAME" \
  --from-file=server.key="$SERVER_KEY_PATH" \
  --from-file=server.crt="$CERT_CHAIN_PATH" \
  --from-literal=server.passphrase="$PASSPHRASE" \
  -n "$NAMESPACE"
```

##### Step 4: Create a Primary Cluster with External Kafka Listeners and KafkaSecurity

Let's recall previous steps. We have installed an Ingress Controller, created a DNS record, and generated a private key and certificate for Kafka brokers. Now we can create a primary cluster with External Kafka Listeners exposed by Ingress. Here is an example CR:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: pr-cluster
spec:
  ha: 1
  image: docker.io/tigergraph/tigergraph-k8s:4.2.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: YOUR_LICENSE
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    requests:
      cpu: "4"
      memory: 8Gi
  storage:
    type: persistent-claim
    volumeClaimTemplate:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
  kafkaConfig:
    externalListener:
      type: Ingress
      # Optional. You can configure labels of the Ingress
      labels:
        cluster: pr-cluster
      # Optional. You can configure annotations of the Ingress
      annotations:
        annotation1: value1
      # Configure the ingressClassName
      ingressClassName: nginx
      # Required if you use Ingress. The base domain should be resolved to the Ingress Controller
      baseDomain: pr-cluster.kafka.your-domain.com
    security:
      secretName: kafka-secret
      # Optional. Default is false.
      enableClientAuth: false
```

> [!NOTE]
> Let me explain more about the `baseDomain` field. As you create a DNS record and resolve `*.kafka.your-domain.com` to the Ingress Controller.
> You can configure the `baseDomain` in CR as `${CLUTSER_NAME}.kafka.your-domain.com`. The addresses of the Kafka brokers will be `broker${i}.${CLUSTER_NAME}.kafka.your-domain.com`.
> For example, if you create a cluster with above CR, the addresses of the Kafka brokers will be `broker1.pr-cluster.kafka.your-domain.com`, `broker2.pr-cluster.kafka.your-domain.com`, `broker3.pr-cluster.kafka.your-domain.com`.

#### Configure the Security of Kafka Brokers

##### Enable SSL for Kafka brokers

When you expose Kafka brokers to the external network, you may consider the security of the Kafka brokers. We provide a field `security` in the CR to configure the security of Kafka brokers. You can enable SSL for Kafka brokers and enable client authentication for Kafka brokers. Security configurations are supported for both LoadBalancer and Ingress.

To enable security for Kafka brokers, you need to generate a private key and certificate for Kafka brokers. You can follow the steps in [Step 3: Generate private key and certificate for your kafka brokers](#step-3-generate-private-key-and-certificate-for-your-kafka-brokers) to generate a private key and certificate for Kafka brokers. And then create a Secret on Kubernetes to store the private key and certificate chain. The content of the Secret should be like:

```yaml
apiVersion: v1
data:
  server.crt: your-certificate-chain-base64
  server.key: your-private-key-base64
  server.passphrase: your-pass-phrase-base64
kind: Secret
metadata:
  name: your-secret-name
  namespace: your-namespace
type: Opaque
```

Then you can configure the security of Kafka brokers in the CR. Here is an example CR to enable SSL for Kafka brokers:

```yaml
...
spec:
  ...
  kafkaConfig:
    security:
      secretName: your-secret-name
      enableClientAuth: false
```

##### Enable Client Authentication for Kafka brokers

If you want to enable client authentication for Kafka brokers, you can set `enableClientAuth` to `true`.
Then kafka brokers will require clients to provide a certificate when they connect to the brokers.

> [!IMPORTANT]
> The truststore of the Kafka brokers contain the root certificate of the server certificate.
> So when you enable client authentication, you should make sure that the client certificate is signed by the **same root certificate** as the server certificate.

#### Get the Addresses of Kafka External Listeners of the Primary Cluster

When the cluster is initialized successfully, you can get the addresses of Kafka External Listeners by running the following command:

```bash
# Replace the namespace and cluster name with your own
CLUSTER_NAME=pr-cluster
NAMESPACE=tigergraph
# get the addresses of Kafka External Listeners, you can copy the output and use it in the DR cluster configuration
kubectl exec ${CLUSTER_NAME}-0 -c tigergraph -n ${NAMESPACE} -- \
  /home/tigergraph/tigergraph/app/cmd/gadmin config get Kafka.ExternalListeners | jq -r 'map(.Hostname) | join(",")'

# get the port of Kafka External Listeners
kubectl exec ${CLUSTER_NAME}-0 -c tigergraph -n ${NAMESPACE} -- \
  /home/tigergraph/tigergraph/app/cmd/gadmin config get Kafka.ExternalListeners | jq -r '.[0].Port'
```

The output will be like:

```text
broker1.pr-cluster.kafka.tigergraph.dev,broker2.pr-cluster.kafka.tigergraph.dev,broker3.pr-cluster.kafka.tigergraph.dev
443
```

### Configure `KafkaConfig` by `kubectl-tg`

If you are using `kubectl tg` command to deploy/manage your cluster, you can use option `--kafka-config` to set/update the `KafkaConfig` of the cluster.
The option accepts name of a file that contains the `KafkaConfig` in YAML format. Here is an example of the `KafkaConfig` file:

```yaml
kafkaConfig:
  externalListener:
    type: LoadBalancer
  security:
    secretName: kafka-secret
    enableClientAuth: false
```

You can save the content to a file named `kafka-config.yaml`, and then use the following command to set the `KafkaConfig` of the cluster:

```bash
# create cluster with KafkaConfig
kubectl tg create -c pr-cluster -n tigergraph --kafka-config kafka-config.yaml ${OTHER_OPTIONS}
# update cluster with KafkaConfig
kubectl tg update -c pr-cluster -n tigergraph --kafka-config kafka-config.yaml ${OTHER_OPTIONS}
```

## Set up DR Clusters

After you create a primary cluster, you can set up DR clusters that sync with the primary cluster.

Before setting up DR clusters, you should make sure that the primary cluster is running normally and the Kafka External Listeners are accessible. You can use `kafkacat` tool to test the connectivity of Kafka brokers.

```bash
export ADDRESS_OF_KAFKA_BROKER=your_address_of_kafka_broker
export PORT=your_port_of_kafka_broker
export ROOT_CERT_PATH="rootCA.crt"
# list topics of the primary cluster, the SSL is not enabled
kafkacat -b ${ADDRESS_OF_KAFKA_BROKER}:${PORT} -L

# if the SSL is enabled, please use following command
kafkacat -b ${ADDRESS_OF_KAFKA_BROKER}:${PORT} -L -X security.protocol=ssl -X ssl.ca.location=${ROOT_CERT_PATH}

# if enableClientAuth is true, you should provide the client certificate and key
export CLIENT_CERT_PATH="client.crt"
export CLIENT_KEY_PATH="client.key"
export PASSPHRASE="your_secure_password"

kafkacat -b ${ADDRESS_OF_KAFKA_BROKER}:${PORT} -L -X security.protocol=ssl \
  -X ssl.ca.location=${ROOT_CERT_PATH} \
  -X ssl.certificate.location=${CLIENT_CERT_PATH} \
  -X ssl.key.location=${CLIENT_KEY_PATH} \
  -X ssl.key.password=${PASSPHRASE}
```

You should also make sure that your DR cluster has **the same number of partitions** and **the same version** as the primary cluster.

### Create a Backup CR to backup the primary cluster

If your cluster is an **empty cluster** without any data, you can skip this step. But if your cluster has data, you should create a Backup CR to backup the primary cluster. Please follow this guide to create a Backup CR to backup the cluster to a S3 bucket: [Backup to S3 bucket](../04-manage/backup-and-restore/backup-restore-by-cr.md#backup-to-s3-bucket). Here is an example CR:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraphBackup
metadata:
  name: backup-pr
spec:
  clusterName: pr-cluster
  destination:
    storage: s3Bucket
    s3Bucket:
      bucketName: operator-backup
      secretKeyName: s3-secret
  backupConfig:
    tag: dr
  cleanPolicy: Delete
```

After the backup is finished, you can use following command to get the tag of the backup package:

```bash
export BACKUP_CR_NAME=backup-pr
export NAMESPACE=tigergraph
kubectl get tgbackup ${BACKUP_CR_NAME} -n ${NAMESPACE} -o yaml | yq .status.backupInfo.tag
```

The output will be like:

```text
dr-2025-01-16T091050.613
```

You can use the tag to restore the backup package to the DR cluster and enable CRR.

### Get the addresses of kafka brokers of the primary cluster

If your DR cluster is in the same k8s cluster as the primary, please  refer to [Get the Addresses of Kafka brokers of the Primary Cluster](#get-the-addresses-of-kafka-brokers-of-the-primary-cluster).

If your DR cluster is in a different k8s cluster os a non-k8s environment, please refer to[Get the Addresses of Kafka External Listeners of the Primary Cluster](#get-the-addresses-of-kafka-external-listeners-of-the-primary-cluster) to get the addresses of Kafka brokers of the primary cluster.

The format of the addresses of Kafka brokers is `${ADDRESS_OF_BROKER1},${ADDRESS_OF_BROKER2},...,${ADDRESS_OF_BROKERN}`.

### Set up CRR

#### Step 1: configure Primary IP and Port

Please run the following commands:

```bash
# replace the primary IPs and port with your own
export PRIMARY_IPS=broker1.pr-cluster.kafka.helm.tigergraph.dev,broker2.pr-cluster.kafka.helm.tigergraph.dev,broker3.pr-cluster.kafka.helm.tigergraph.dev
export PRIMARY_PORT=30006

gadmin config set System.CrossRegionReplication.PrimaryKafkaIPs "${PRIMARY_IPS}"
gadmin config set System.CrossRegionReplication.PrimaryKafkaPort ${PRIMARY_PORT}
gadmin config set System.CrossRegionReplication.TopicPrefix Primary
gadmin config apply -y
```

> [!IMPORTANT]
> If you are using TigerGraph that is earlier than 3.10.x, the steps to configure CRR are different.
> Please refer to the [Set Up Cross-Region Replication](https://docs.tigergraph.com/tigergraph-server/3.9/cluster-and-ha-management/set-up-crr) for earlier versions.

#### Step 2[Optional]: configure Kafka Security on DR cluster

If you have enabled the `enableClientAuth` in the primary cluster, you will need to configure the Kafka Security on DR cluster.
The certificate configured by `Kafka.Security.SSL.Certificate` in the DR cluster will be used as the client certificate to connect to the primary cluster.
You should generate a client certificate that is signed by the same root certificate as the server certificate of the primary cluster.
Here is an example to generate a client certificate with the same root certificate as the server certificate:

```bash
# the password of the private key
export PASSPHRASE="your_secure_password"
# paths of the generated files
export ROOT_KEY_PATH="rootCA.key"
export ROOT_CERT_PATH="rootCA.crt"
export CLIENT_KEY_PATH="client.key"
export CLIENT_CSR_PATH="client.csr"
export CLIENT_CERT_PATH="client.crt"
export CLIENT_CERT_CHAIN_PATH="client_cert_chain.pem"

# generate client key
# For openssl 1.x.x
openssl genrsa -aes256 -passout pass:$PASSPHRASE -out $CLIENT_KEY_PATH 2048
# For openssl 3.x.x 
openssl genrsa -traditional -aes256 -passout pass:$PASSPHRASE -out $CLIENT_KEY_PATH 2048

# generate client csr
openssl req -new -key $CLIENT_KEY_PATH -passin pass:$PASSPHRASE \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=MyCompany/OU=IT/CN=myclient.local" \
  -out $CLIENT_CSR_PATH

# generate client certificate
openssl x509 -req -in $CLIENT_CSR_PATH -CA $ROOT_CERT_PATH -CAkey $ROOT_KEY_PATH \
  -CAcreateserial -out $CLIENT_CERT_PATH -days 365 -sha256 \
  -passin pass:$PASSPHRASE

# concatenate the client certificate and the root certificate to form the certificate chain
cat $CLIENT_CERT_PATH $ROOT_CERT_PATH > $CLIENT_CERT_CHAIN_PATH

# verify the certificate chain
openssl verify -CAfile $ROOT_CERT_PATH $CLIENT_CERT_PATH
```

Please copy the private key and certificate chain to the DR cluster, assume that the file paths are `/home/tigergraph/client_cert_chain.pem` and `/home/tigergraph/client.key`.
Login into the DR cluster and run following commands to configure the Kafka Security on DR cluster:

```bash
gadmin config set Kafka.Security.SSL.Certificate "@/home/tigergraph/client_cert_chain.pem"
gadmin config set Kafka.Security.SSL.PrivateKey "@/home/tigergraph/client.key"
gadmin config set Kafka.Security.SSL.Enable true
gadmin config set Kafka.Security.SSL.Passphrase your_secure_password
gadmin config apply -y
```

#### Step 3[Optional]: configure restore for DR cluster

If the primary cluster has data and you have created a backup package, you can restore the backup package to the DR cluster.

```bash
export AWS_ACCESS_KEY_ID=YOUR_AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=YOUR_AWS_SECRET_ACCESS_KEY
export S3_BUCKET_NAME=YOUR_BUCKET_NAME

gadmin config set System.Backup.S3.Enable true
gadmin config set System.Backup.S3.AWSAccessKeyID $AWS_ACCESS_KEY_ID
gadmin config set System.Backup.S3.AWSSecretAccessKey $AWS_SECRET_ACCESS_KEY
gadmin config set System.Backup.S3.BucketName $S3_BUCKET_NAME
gadmin config apply -y
```

#### Step 4: set up CRR

Finally you can enable CRR by running the following command:

```bash
# if you have a backup package and get the tag of the backup package in the previous steps
gadmin backup restore dr-2025-01-16T091050.613 --dr
# if the primary cluster is empty
gadmin backup restore --dr
```

The output will be like:

```text
[   Note] gadmin backup restore needs to reset TigerGraph system.
Are you sure? (y/N)y
[   Info] [Fri Jan 17 03:30:53 UTC 2025] Restore to DR cluster: set System.CrossRegionReplication.Enabled to true
[   Info] [Fri Jan 17 03:30:54 UTC 2025] Restore to DR cluster: Initializing Kafka...
[   Info] [Fri Jan 17 03:31:29 UTC 2025] Start to restore full backup dr-2025-01-17T030555.701
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Staging path: /home/tigergraph/tigergraph/data/backup_staging_dir/restore/dr-2025-01-17T030555.701 (shared: false)
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Begin to check needed disk space...
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Backup archives temporary path: /home/tigergraph/tigergraph/data/backup_staging_dir/restore/dr-2025-01-17T030555.701/cloudstor_temp
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Downloading backup archives from S3...
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Scan backup archives under the path /home/tigergraph/tigergraph/data/backup_staging_dir/restore/dr-2025-01-17T030555.701/cloudstor_temp
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Prepare archives...
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Start to restore GPE & GSE data...
[   Info] [Fri Jan 17 03:31:30 UTC 2025] Stopping GPE, GSE, RESTPP, and GSQL...
[   Info] [Fri Jan 17 03:31:48 UTC 2025] Backing up current GPE & GSE data...
[   Info] [Fri Jan 17 03:31:48 UTC 2025] Extracting GPE & GSE data...
[   Info] [Fri Jan 17 03:31:48 UTC 2025] Replace Gstore successfully. you can remove manually the path /home/tigergraph/tigergraph/data/gstore_memo-20250117033148 after the restore is finished.
[   Info] [Fri Jan 17 03:31:48 UTC 2025] Resetting GPE & GSE
[   Info] [Fri Jan 17 03:32:08 UTC 2025] Write back full backup's watermarks
[   Info] [Fri Jan 17 03:32:21 UTC 2025] Restore GPE & GSE successfully.
[   Info] [Fri Jan 17 03:32:21 UTC 2025] Importing GSQL data... (async)
[   Info] [Fri Jan 17 03:32:21 UTC 2025] Extracting GSQL data...
[   Info] [Fri Jan 17 03:32:21 UTC 2025] Importing GUI data... (async)
[   Info] [Fri Jan 17 03:32:21 UTC 2025] Extracting GUI#1 data...
[   Info] [Fri Jan 17 03:32:21 UTC 2025] GUI imported
[   Info] [Fri Jan 17 03:32:45 UTC 2025] GSQL imported
[   Info] [Fri Jan 17 03:32:45 UTC 2025] Resetting GSQL & KAFKACONN
[   Info] [Fri Jan 17 03:33:06 UTC 2025] Starting services
[   Info] [Fri Jan 17 03:33:17 UTC 2025] Restore completes
[   Info] [Fri Jan 17 03:33:17 UTC 2025] Clean staging directory: /home/tigergraph/tigergraph/data/backup_staging_dir/restore/dr-2025-01-17T030555.701
[   Info] [Fri Jan 17 03:33:17 UTC 2025] Clean staging directory /home/tigergraph/tigergraph/data/backup_staging_dir/restore/dr-2025-01-17T030555.701 successfully
[   Info] [Fri Jan 17 03:33:17 UTC 2025] Restore to DR cluster: waiting KAFKACONN online...
[   Info] [Fri Jan 17 03:33:17 UTC 2025] Waiting KAFKACONN
[   Info] [Fri Jan 17 03:33:32 UTC 2025] Restore to DR cluster: stop CRR by `gadmin crr stop`
[   Info] [Fri Jan 17 03:33:32 UTC 2025] Restore to DR cluster: start CRR by `gadmin crr start`
```

If no error occurs, the DR cluster will be synchronized with the primary cluster.

## Update Kafka Config

You may want to update the Kafka Config of a cluster in the following scenarios:

1. Add Kafka external listeners to the cluster that doesn't have external listeners.
   
2. Change the type of Kafka external listeners.

3. Enable SSL or client authentication for Kafka brokers.

4. Remove Kafka external listeners from the cluster.

To update the Kafka Config of a cluster, you just need to update the `kafkaConfig` field of the CR.
After you apply the CR, the Services created for Kafka external listeners will be updated automatically.
Then a config-update job will be created to update the configurations of Kafka brokers.

> [!IMPORTANT]
> If you have already configured a DR to synchronize with the primary cluster before updating the Kafka config, they may lose synchronization after the update.  
>
> For example, if you modify the listener type, the service address will inevitably change, causing the DR to fail to synchronize with the primary cluster. Similarly, enabling or disabling SSL will result in the same issue.  
>
> Additionally, if you scale the primary cluster, the number of Kafka external listeners will change, which can also cause the DR to lose synchronization.
> In these cases, you need to recreate the backup of the primary cluster and reconfigure the DR cluster to ensure CRR functions properly.

## Demo

Please watch the following video to see how to set up DR clusters that are in different Kubernetes clusters:

[How to set up CRR on K8s](https://drive.google.com/file/d/1UOFvNuEVNipCfQlhPp70BnhmRD3asMzV/view?usp=sharing)