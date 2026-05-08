# Configure Nginx TLS

This guide demonstrates how to configure TLS certificates for TigerGraph Nginx services to enable secure HTTPS access to TigerGraph clusters. The `NginxConfig` field allows you to configure TLS for Nginx running in TigerGraph Pods.

- [Configure Nginx TLS](#configure-nginx-tls)
  - [Generate SSL Certificate](#generate-ssl-certificate)
  - [Create Kubernetes TLS Secret](#create-kubernetes-tls-secret)
  - [Configure TigerGraph Cluster with Nginx TLS](#configure-tigergraph-cluster-with-nginx-tls)
    - [LoadBalancer Service Type](#loadbalancer-service-type)
    - [NodePort Service Type](#nodeport-service-type)
    - [Ingress Service Type](#ingress-service-type)
      - [Prerequisites](#prerequisites)
      - [Configure Nginx TLS with Ingress Service Type](#configure-nginx-tls-with-ingress-service-type)
    - [Update Nginx Config](#update-nginx-config)
    - [Configure Nginx TLS by `kubectl-tg`](#configure-nginx-tls-by-kubectl-tg)
  - [Configure Nginx mTLS](#configure-nginx-mtls)
    - [Generate mTLS Certificates](#generate-mtls-certificates)
    - [Create Kubernetes Secrets for mTLS](#create-kubernetes-secrets-for-mtls)
      - [For LoadBalancer and NodePort Service Types](#for-loadbalancer-and-nodeport-service-types)
      - [For Ingress Service Type](#for-ingress-service-type)
    - [Configure TigerGraph Cluster with mTLS](#configure-tigergraph-cluster-with-mtls)
      - [LoadBalancer Service Type with mTLS](#loadbalancer-service-type-with-mtls)
      - [NodePort Service Type with mTLS](#nodeport-service-type-with-mtls)
      - [Ingress Service Type with mTLS](#ingress-service-type-with-mtls)
  - [Troubleshooting](#troubleshooting)
    - [Self-signed certificate](#self-signed-certificate)
    - [No alternative certificate subject name matches target host name](#no-alternative-certificate-subject-name-matches-target-host-name)
    - [SAN validation failed - No SAN extension found in client certificate](#san-validation-failed---no-san-extension-found-in-client-certificate)
    - [Client certificate verification failed](#client-certificate-verification-failed)

## Generate SSL Certificate

To enable TLS for Nginx, you must have a valid SSL certificate. Here we provide some commands to generate a self-signed certificate for Nginx TLS. Please replace the `your-domain.com` with your own domain.

```bash
# Configuration variables
export NAMESPACE=tigergraph
export CLUSTER_NAME=my-tigergraph-cluster
export DOMAIN=your-domain.com
export SECRET_NAME=nginx-tls-secret
export DAYS=365
export KEY_FILE="$HOME/nginx_tls_${CLUSTER_NAME}.key"
export CRT_FILE="$HOME/nginx_tls_${CLUSTER_NAME}.crt"

# Generate OpenSSL configuration
CONFIG_FILE=$(mktemp)
cat > $CONFIG_FILE <<-EOF
[req]
prompt = no
x509_extensions = san_env
distinguished_name = req_distinguished_name

[req_distinguished_name]
countryName = US
stateOrProvinceName = California
localityName = Palo Alto
organizationName = TigerGraph
organizationalUnitName = Engineering
commonName = tigergraph.dev

[san_env]
subjectAltName = DNS:$DOMAIN
EOF

# Generate certificate
openssl req -x509 -nodes -days $DAYS -newkey rsa:2048 \
  -keyout $KEY_FILE -out $CRT_FILE \
  -config $CONFIG_FILE \
  -subj "/CN=tigergraph.dev"

# Verify certificate
echo -e "\n=== Verifying certificate ==="
SAN_CHECK=$(openssl x509 -in $CRT_FILE -noout -text | grep "DNS:$DOMAIN" || true)

if [ -n "$SAN_CHECK" ]; then
  echo "SAN check passed: $SAN_CHECK"
else
  echo "SAN doesn't generate successfully"
  exit 1
fi

# Clean up temporary files
rm -f $CONFIG_FILE

echo -e "\n=== Certificate generated successfully! ==="
echo "Key file: $KEY_FILE"
echo "Certificate file: $CRT_FILE"
```

## Create Kubernetes TLS Secret

After generating the SSL certificate, create a Kubernetes TLS secret for Nginx:

```bash
# Create TLS secret for Nginx
kubectl create secret tls $SECRET_NAME \
  --cert=$CRT_FILE \
  --key=$KEY_FILE \
  --namespace $NAMESPACE
```

Verify the secret was created successfully:

```bash
# List secrets in the namespace
kubectl get secrets -n $NAMESPACE

# Describe the TLS secret
kubectl describe secret $SECRET_NAME -n $NAMESPACE
```

Expected output:
```
Name:         nginx-tls-secret
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>

Type:  kubernetes.io/tls

Data
====
tls.crt:  1476 bytes
tls.key:  1704 bytes
```

## Configure TigerGraph Cluster with Nginx TLS

For different service types, you need to configure the Nginx TLS differently.

### LoadBalancer Service Type

When using LoadBalancer service type, the `NginxConfig.SecretName` enables TLS for Nginx. The LoadBalancer service will expose the Nginx service directly, and Nginx will handle TLS termination.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.3.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  # Configure Nginx TLS
  nginxConfig:
    secretName: nginx-tls-secret
  privateKeyName: ssh-key-secret
  replicas: 3
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 50G
      storageClassName: standard
      volumeMode: Filesystem
```

**Verify the Access to Nginx**

First, let's get the address of service:

```bash
kubectl get svc ${CLUSTER_NAME}-nginx-external-service -n ${NAMESPACE}
```

The output will be like:
```text
NAME                                  TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)           AGE
test-cluster-nginx-external-service   LoadBalancer   34.118.232.158   34.67.253.147   14240:31078/TCP   3m26s
```

Then you can verify the access to Nginx by running the following command:

```bash
export EXTERNAL_IP=34.67.253.147
curl -k https://${EXTERNAL_IP}:14240/api/ping
```

> [!NOTE]
> We are using `-k` option to bypass the certificate verification. To access the service with certificate verification, you need to configure a DNS to point to the Nginx service. For example, you can create a DNS record like:
> ```text
> Type: A
> Host: your-domain.com
> Value: 34.67.253.147
> ```
>
> Then you can access the service with certificate verification by running the following command:
> ```bash
> curl https://your-domain.com:14240/api/ping --cacert $CRT_FILE
> ```


### NodePort Service Type

When using NodePort service type, the `NginxConfig.SecretName` enables TLS for Nginx. The NodePort service will expose the Nginx service directly, and Nginx will handle TLS termination.

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.3.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: NodePort
    # Configure Nginx NodePort
    nginxNodePort: 30241
    # Configure Nginx TLS
    nginxConfig:
      secretName: nginx-tls-secret
  privateKeyName: ssh-key-secret
  replicas: 3
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 50G
      storageClassName: standard
      volumeMode: Filesystem
```

**Verify the Access to Nginx**

First, let's get the external IP of Nodes:
```bash
kubectl get nodes -o wide
```

The output will be like:
```text
NAME                                         STATUS   ROLES    AGE   VERSION               INTERNAL-IP   EXTERNAL-IP     OS-IMAGE                             KERNEL-VERSION   CONTAINER-RUNTIME
gke-tg-gke-1024-default-pool-29f86fa3-3502   Ready    <none>   71m   v1.33.3-gke.1136000   10.128.0.69   34.61.161.217   Container-Optimized OS from Google   6.6.93+          containerd://2.0.4
gke-tg-gke-1024-default-pool-29f86fa3-7lqh   Ready    <none>   71m   v1.33.3-gke.1136000   10.128.0.70   34.133.63.247   Container-Optimized OS from Google   6.6.93+          containerd://2.0.4
```

You can use any of the external IP to verify the access to Nginx.

```bash
export EXTERNAL_IP=34.61.161.217
export NODE_PORT=30241 # Use the nginxNodePort you configured
curl -k https://${EXTERNAL_IP}:${NODE_PORT}/api/ping
```

> [!NOTE]
> We are using `-k` option to bypass the certificate verification. To access the service with certificate verification, you need to configure a DNS to point to the Nginx service. For example, you can create a DNS record like:
> ```text
> Type: A
> Host: your-domain.com
> Value: 34.61.161.217
> ```
>
> Then you can access the service with certificate verification by running the following command:
> ```bash
> curl https://your-domain.com:${NODE_PORT}/api/ping --cacert $CRT_FILE
> ```

### Ingress Service Type

#### Prerequisites

Before using Ingress service type, you need to install an Ingress Controller in the Kubernetes cluster and configure the DNS record to point to the Ingress Controller. Here we take Nginx Ingress Controller as an example.
Please refer to [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/deploy/) for how to install the Nginx Ingress Controller.

After the Ingress Controller is installed, you can get the address of the Ingress Controller by running the following command:

```bash
kubectl get svc ingress-nginx-controller  -n ingress-nginx
```

The output will be like:

```text
NAME                       TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)                      AGE
ingress-nginx-controller   LoadBalancer   34.118.236.161   34.31.217.219   80:31346/TCP,443:30511/TCP   89s
```

The `EXTERNAL-IP` of service `ingress-nginx-controller` is the address of the Ingress Controller. you need to create a DNS record and resolve the **base domain** to the Ingress Controller.
If the address of the ingress controller is an IP address, please create a DNS record like:

```text
Type: A
Host: your-domain.com
Value: 34.31.217.219
```

If you are using EKS, the address of the Ingress Controller may be a domain name. In this case, you can create a CNAME record like:

```text
Type: CNAME
Host: your-domain.com
Value: a1b2c3d4e5f6g7.here.amazonaws.com
```

#### Configure Nginx TLS with Ingress Service Type

When using Ingress service type, you need to configure both Ingress TLS termination and Nginx TLS termination. This creates a double TLS setup where:
1. Ingress terminates TLS for external traffic
2. Nginx terminates TLS for internal traffic between Ingress and TigerGraph services

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.3.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: Ingress
    ingressClassName: nginx
    nginxHost: your-domain.com
    # Required: Set a SecretName for Ingress TLS termination
    secretName: nginx-tls-secret
  # Required: Nginx TLS secret for internal traffic
  nginxConfig:
    secretName: nginx-tls-secret
  privateKeyName: ssh-key-secret
  replicas: 3
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 50G
      storageClassName: standard
      volumeMode: Filesystem
```

**Key points for Ingress configuration:**
- Both `listener.secretName` and `nginxConfig.secretName` are required
- `listener.secretName` is used for Ingress TLS termination (external traffic)
- `nginxConfig.secretName` is used for Nginx TLS termination (internal traffic)
- The backend protocol is automatically set to HTTPS when `nginxConfig.secretName` is configured
- You are allowed to use a different certificate for `listener.secretName` and `nginxConfig.secretName`, you need to create two separate TLS secrets. But we recommend you to use the same certificate for both.

**Verify the Access to Nginx**

Use the following command to verify the access to Nginx:
```bash
curl  https://your-domain.com/api/ping --cacert $CRT_FILE
```

### Update Nginx Config

You may want to update the Nginx Config of a cluster in the following scenarios:

1. Enable/Disable Nginx TLS for a running cluster.

2. Change the certificate for Nginx TLS.

To update the Nginx Config of a cluster, you just need to update the `nginxConfig` field of the CR. If you want to renew the certificate, you just need to change the content of the secret.
After you apply the CR or update the secret, a config-update job will be created to update the configurations of Nginx.

### Configure Nginx TLS by `kubectl-tg`

If you are using `kubectl tg` command to deploy/manage your cluster, you can use option `--nginx-secret-name` to set/update the `NginxConfig.SecretName` of the cluster.

```bash
# create cluster with Nginx TLS
kubectl tg create --cluster-name test-cluster -n tigergraph --nginx-secret-name nginx-tls-secret ${OTHER_OPTIONS}
# update cluster with Nginx TLS
kubectl tg update --cluster-name test-cluster -n tigergraph --nginx-secret-name nginx-tls-secret ${OTHER_OPTIONS}
```

## Configure Nginx mTLS

This section demonstrates how to configure mutual TLS (mTLS) for TigerGraph Nginx services. With mTLS enabled, Nginx will verify client certificates in addition to providing server certificates, ensuring both client and server authenticate each other.

### Generate mTLS Certificates

To enable mTLS, you need to generate a root CA certificate and client certificates based on that root CA. Here we provide commands to generate self-signed certificates for mTLS:

```bash
# Configuration variables
export NAMESPACE=tigergraph
export CLUSTER_NAME=my-tigergraph-cluster
export DOMAIN=your-domain.com
export CA_SECRET_NAME=nginx-mtls-ca-secret
export CLIENT_SECRET_NAME=nginx-mtls-client-secret
export DAYS=365
export CA_KEY_FILE="$HOME/nginx_mtls_ca_${CLUSTER_NAME}.key"
export CA_CRT_FILE="$HOME/nginx_mtls_ca_${CLUSTER_NAME}.crt"
export CLIENT_KEY_FILE="$HOME/nginx_mtls_client_${CLUSTER_NAME}.key"
export CLIENT_CRT_FILE="$HOME/nginx_mtls_client_${CLUSTER_NAME}.crt"

# Step 1: Generate Root CA certificate
echo "=== Generating Root CA certificate ==="
openssl req -x509 -nodes -days $DAYS -newkey rsa:2048 \
  -keyout $CA_KEY_FILE -out $CA_CRT_FILE \
  -subj "/C=US/ST=California/L=Palo Alto/O=TigerGraph/OU=Engineering/CN=TigerGraph-CA"

# Step 2: Generate client certificate signing request with SAN extension
echo "=== Generating client certificate signing request ==="
# Create OpenSSL configuration for client certificate with SAN
CLIENT_CONFIG_FILE=$(mktemp)
cat > $CLIENT_CONFIG_FILE <<-EOF
[req]
prompt = no
distinguished_name = req_distinguished_name
req_extensions = v3_req

[req_distinguished_name]
C = US
ST = California
L = Palo Alto
O = TigerGraph
OU = Engineering
CN = TigerGraph-Client

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = TigerGraph-Client
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

openssl req -new -nodes -newkey rsa:2048 \
  -keyout $CLIENT_KEY_FILE -out $CLIENT_CRT_FILE.csr \
  -config $CLIENT_CONFIG_FILE

# Step 3: Sign client certificate with Root CA
echo "=== Signing client certificate with Root CA ==="
openssl x509 -req -in $CLIENT_CRT_FILE.csr -CA $CA_CRT_FILE -CAkey $CA_KEY_FILE \
  -CAcreateserial -out $CLIENT_CRT_FILE -days $DAYS \
  -extensions v3_req -extfile $CLIENT_CONFIG_FILE

# Step 4: Verify certificates
echo "=== Verifying certificates ==="
echo "Root CA certificate:"
openssl x509 -in $CA_CRT_FILE -noout -subject -issuer

echo "Client certificate:"
openssl x509 -in $CLIENT_CRT_FILE -noout -subject -issuer

# Clean up temporary files
rm -f $CLIENT_CRT_FILE.csr $CLIENT_CONFIG_FILE

echo -e "\n=== mTLS certificates generated successfully! ==="
echo "Root CA key file: $CA_KEY_FILE"
echo "Root CA certificate file: $CA_CRT_FILE"
echo "Client key file: $CLIENT_KEY_FILE"
echo "Client certificate file: $CLIENT_CRT_FILE"
```

### Create Kubernetes Secrets for mTLS

After generating the mTLS certificates, create the appropriate Kubernetes secrets based on your service type.

#### For LoadBalancer and NodePort Service Types

For LoadBalancer and NodePort service types, you only need to create a secret containing the CA certificate. This certificate will be configured in Nginx to verify client certificates.

```bash
# Create CA secret for mTLS verification
kubectl create secret generic $CA_SECRET_NAME \
  --from-file=ca.crt=$CA_CRT_FILE \
  --namespace $NAMESPACE
```

Verify the secret was created successfully:

```bash
# List secrets in the namespace
kubectl get secrets -n $NAMESPACE

# Describe the CA secret
kubectl describe secret $CA_SECRET_NAME -n $NAMESPACE
```

Expected output:
```
Name:         nginx-mtls-ca-secret
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>

Type:  Opaque

Data
====
ca.crt:  1476 bytes
```

#### For Ingress Service Type

For Ingress service type, you need to create a single secret containing both the CA certificate and client certificate/key. This secret will be used by both Nginx (for client certificate verification) and the Ingress controller (for presenting client certificates when connecting to Nginx).

```bash
# Create combined mTLS secret with CA certificate and client certificate/key
kubectl create secret generic $CA_SECRET_NAME \
  --from-file=ca.crt=$CA_CRT_FILE \
  --from-file=tls.crt=$CLIENT_CRT_FILE \
  --from-file=tls.key=$CLIENT_KEY_FILE \
  --namespace $NAMESPACE
```

Verify the secret was created successfully:

```bash
# List secrets in the namespace
kubectl get secrets -n $NAMESPACE

# Describe the mTLS secret
kubectl describe secret $CA_SECRET_NAME -n $NAMESPACE
```

Expected output:
```
Name:         nginx-mtls-ca-secret
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>

Type:  Opaque

Data
====
ca.crt:   1476 bytes
tls.crt:  1476 bytes
tls.key:  1704 bytes
```

> [!IMPORTANT]
> The secret must contain the exact keys `ca.crt`, `tls.crt`, and `tls.key`.
> The name of these keys cannot be customized.

### Configure TigerGraph Cluster with mTLS

To enable mTLS for your TigerGraph cluster, configure the `ClientCertSecretName` field in the `NginxConfig` section of your TigerGraph custom resource.

#### LoadBalancer Service Type with mTLS

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.3.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: LoadBalancer
  # Configure Nginx TLS and mTLS
  nginxConfig:
    secretName: nginx-tls-secret          # Server certificate for TLS
    clientCertSecretName: nginx-mtls-ca-secret  # CA certificate for mTLS verification
  privateKeyName: ssh-key-secret
  replicas: 3
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 50G
      storageClassName: standard
      volumeMode: Filesystem
```

#### NodePort Service Type with mTLS

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.3.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: NodePort
    nginxNodePort: 30241
  # Configure Nginx TLS and mTLS
  nginxConfig:
    secretName: nginx-tls-secret          # Server certificate for TLS
    clientCertSecretName: nginx-mtls-ca-secret  # CA certificate for mTLS verification
  privateKeyName: ssh-key-secret
  replicas: 3
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 50G
      storageClassName: standard
      volumeMode: Filesystem
```

#### Ingress Service Type with mTLS

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.3.0
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: Ingress
    ingressClassName: nginx
    nginxHost: your-domain.com
    # Required: Set a SecretName for Ingress TLS termination
    secretName: nginx-tls-secret
  # Configure Nginx TLS and mTLS
  nginxConfig:
    secretName: nginx-tls-secret          # Server certificate for TLS
    clientCertSecretName: nginx-mtls-ca-secret  # CA certificate for mTLS verification
  privateKeyName: ssh-key-secret
  replicas: 3
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
      accessModes:
      - ReadWriteOnce
      resources:
        requests:
          storage: 50G
      storageClassName: standard
      volumeMode: Filesystem
```

**Key points for mTLS configuration:**
- `nginxConfig.secretName` is used for server TLS certificate (required for HTTPS)
- `nginxConfig.clientCertSecretName` is used for client certificate verification (enables mTLS)
- For LoadBalancer and NodePort: Only CA certificate is needed in the client secret
- For Ingress: Both CA certificate and client certificate/key are needed in the same secret
- When mTLS is enabled, all client requests must present a valid client certificate signed by the CA

**Verify mTLS Access**

To verify mTLS is working correctly, you need to use the client certificate when making requests:

```bash
# For LoadBalancer/NodePort services
curl --cert $CLIENT_CRT_FILE --key $CLIENT_KEY_FILE https://your-service-address:port/api/ping

# For Ingress services
curl --cert $CLIENT_CRT_FILE --key $CLIENT_KEY_FILE  https://your-domain.com/api/ping
```

## Troubleshooting

When you are not able to access the Nginx service with TLS/mTLS enabled, please run `curl` command with `-v` option to get the detailed error message. Here are some common error messages and solutions:

### Self-signed certificate

If you are using a self-signed certificate, you may encounter the following error:

```
curl: (60) SSL certificate problem: self-signed certificate
```

**Solution**:

1. You can use `-k` option to bypass the certificate verification.

2. You can use `--cacert` option to specify the certificate file.

### No alternative certificate subject name matches target host name

If you use the address of LoadBalancer or NodePort service, you may encounter the following error:

```
curl: (60) SSL: no alternative certificate subject name matches target host name '34.61.161.217'
```

When you create the certificate, you don't know the IP address of the LoadBalancer or NodePort service, so you can't add the IP address to the certificate. That's why you encounter this error.

**Solution**:

1. You can use `-k` option to bypass the certificate verification.

2. You can configure a DNS record to point to the LoadBalancer or NodePort service. Use the domain name that you configured in the certificate.

3. Generate a certificate with the IP address of the LoadBalancer or NodePort service. And update the Nginx TLS secret with the new certificate.

### SAN validation failed - No SAN extension found in client certificate

If you encounter the following error when using mTLS:

```
Forbidden: SAN validation failed - No SAN extension found in client certificate
```

This error occurs because the client certificate doesn't have a Subject Alternative Name (SAN) extension, which is required for proper certificate validation.

**Solution**:

1. **Regenerate the client certificate with SAN extension**: Use the certificate generation script provided in the [Generate mTLS Certificates](#generate-mtls-certificates) section, which includes proper SAN configuration.

2. **Verify the certificate has SAN extension**: You can check if your existing client certificate has SAN extension by running:
   ```bash
   openssl x509 -in $CLIENT_CRT_FILE -noout -text | grep -A 5 "Subject Alternative Name"
   ```

3. **Update the secret with the new certificate**: After regenerating the certificate with SAN extension, update your Kubernetes secret:
   ```bash
   kubectl create secret generic $CA_SECRET_NAME \
     --from-file=ca.crt=$CA_CRT_FILE \
     --from-file=tls.crt=$CLIENT_CRT_FILE \
     --from-file=tls.key=$CLIENT_KEY_FILE \
     --namespace $NAMESPACE \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

### Client certificate verification failed

If you encounter the following error when using mTLS:

```
Forbidden: Client certificate verification failed
```

This error occurs when the client certificate cannot be verified by the server. This can happen for several reasons.

**Common causes and solutions**:

1. **Client certificate is not signed by the trusted CA**:
   - **Check**: Verify that your client certificate is signed by the CA certificate configured in the `clientCertSecretName` secret:
     ```bash
     # Check the issuer of the client certificate
     openssl x509 -in $CLIENT_CRT_FILE -noout -issuer
     
     # Check the subject of the CA certificate
     openssl x509 -in $CA_CRT_FILE -noout -subject
     ```
   - **Solution**: Regenerate the client certificate using the correct CA certificate.

2. **Wrong CA certificate in the secret**:
   - **Check**: Verify the CA certificate in the Kubernetes secret matches the one used to sign the client certificate:
     ```bash
     # Extract and check the CA certificate from the secret
     kubectl get secret $CA_SECRET_NAME -n $NAMESPACE -o jsonpath='{.data.ca\.crt}' | base64 -d | openssl x509 -noout -subject
     ```
   - **Solution**: Update the secret with the correct CA certificate that was used to sign the client certificate.

3. **Client certificate has expired**:
   - **Check**: Verify the certificate validity period:
     ```bash
     openssl x509 -in $CLIENT_CRT_FILE -noout -dates
     ```
   - **Solution**: Generate a new client certificate with a valid expiration date.
