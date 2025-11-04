# Configure SSL Certificate for Ingress Service

- [Configure SSL Certificate for Ingress Service](#configure-ssl-certificate-for-ingress-service)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Generate SSL Certificate](#generate-ssl-certificate)
  - [Create Kubernetes TLS Secret](#create-kubernetes-tls-secret)
  - [Configure TigerGraph Cluster with Ingress Service](#configure-tigergraph-cluster-with-ingress-service)
  - [Verify SSL Configuration](#verify-ssl-configuration)
  - [Access TigerGraph Services via HTTPS](#access-tigergraph-services-via-https)
    - [API Service](#api-service)
    - [RESTPP Service](#restpp-service)
    - [Metrics Service](#metrics-service)
  - [Troubleshooting](#troubleshooting)
    - [Certificate Issues](#certificate-issues)
    - [Ingress Issues](#ingress-issues)
      - [How to View Ingress Controller Logs](#how-to-view-ingress-controller-logs)
      - [Common Ingress controller error logs and solutions](#common-ingress-controller-error-logs-and-solutions)
  - [See also](#see-also)

## Overview

This guide demonstrates how to configure SSL certificates for TigerGraph Ingress services to enable secure HTTPS access to TigerGraph clusters. The process involves generating SSL certificates, creating Kubernetes TLS secrets, and configuring the TigerGraph cluster to use Ingress services with SSL termination.

## Prerequisites

Before you begin, ensure you have the following:

- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): Version >= 1.23
- [OpenSSL](https://www.openssl.org/): For generating SSL certificates
- A Kubernetes cluster with an Ingress controller (e.g., NGINX Ingress Controller)
- TigerGraph Operator installed and running
- Appropriate permissions to create secrets and services in the target namespace

## Generate SSL Certificate

Use the commands to generate a certificate:

```bash
# Configuration variables
export NAMESPACE=tigergraph
export CLUSTER_NAME=my-tigergraph-cluster
export DOMAIN=your-domain.com
export SECRET_NAME=ingress-secret
export DAYS=365
export KEY_FILE="$HOME/tls_${CLUSTER_NAME}.key"
export CRT_FILE="$HOME/tls_${CLUSTER_NAME}.crt"

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

After generating the SSL certificate, create a Kubernetes TLS secret:

```bash
# Create TLS secret
kubectl create secret tls $SECRET_NAME \
  --cert=$CRT_FILE \
  --key=$KEY_FILE \
  --namespace $NAMESPACE
```

Verify the secret was created successfully:

```bash
# List secrets in the namespace
kubectl get secrets -n tigergraph

# Describe the TLS secret
kubectl describe secret $SECRET_NAME -n tigergraph
```

Expected output:
```
Name:         ingress-secret
Namespace:    tigergraph
Labels:       <none>
Annotations:  <none>

Type:  kubernetes.io/tls

Data
====
tls.crt:  1476 bytes
tls.key:  1704 bytes
```

## Configure TigerGraph Cluster with Ingress Service

Create a TigerGraph cluster configuration that uses Ingress service type:

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
  namespace: tigergraph
spec:
  image: docker.io/tigergraph/tigergraph-k8s:4.2.1
  imagePullPolicy: IfNotPresent
  imagePullSecrets:
  - name: tigergraph-image-pull-secret
  ha: 1
  license: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  listener:
    type: Ingress
    ingressClassName: nginx
    nginxHost: test-nginx.helm.tigergraph.dev
    secretName: ingress-secret
  privateKeyName: ssh-key-secret
  replicas: 3
  resources:
    limits:
      cpu: "3"
      memory: 8Gi
    requests:
      cpu: "3"
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

Apply the configuration:

```bash
kubectl apply -f tigergraph-cluster.yaml
```

## Verify SSL Configuration

Check that the Ingress resource was created with SSL configuration:

```bash
# Get the Ingress resource
kubectl get ingress -n tigergraph

# Describe the Ingress to see SSL configuration
kubectl describe ingress test-cluster-ingress -n tigergraph
```

Expected output should show TLS configuration:
```
Name:             test-cluster-ingress
Namespace:        tigergraph
Address:          xxx.xxx.xxx.xxx
Default backend:  <default>
TLS:
  ingress-secret terminates your-domain.com
Rules:
  Host              Path  Backends
  ----              ----  --------
  your-domain.com
                    /   test-cluster-internal-service:14240 (10.244.0.5:14240)
```

## Access TigerGraph Services via HTTPS

Once the SSL certificate is configured, you can access TigerGraph services via HTTPS:

### API Service
```bash
# Test API service
curl -s --cacert $CRT_FILE https://your-domain.com/api/ping
```

### RESTPP Service
```bash
# Test RESTPP service
curl -s --cacert $CRT_FILE https://your-domain.com/restpp/echo
```

### Metrics Service
```bash
# Test Metrics service
curl -s --cacert $CRT_FILE \
  https://your-domain.com/informant/metrics/get/network \
  -d '{"LatestNum":"1"}'
```

## Troubleshooting

### Certificate Issues

If you encounter certificate-related errors:

1. **Verify certificate validity**:
   ```bash
   openssl x509 -in $CRT_FILE -text -noout
   ```

2. **Check certificate expiration**:
   ```bash
   openssl x509 -in $CRT_FILE -noout -dates
   ```

3. **Verify SAN configuration**:
   ```bash
   openssl x509 -in $CRT_FILE -noout -text | grep DNS
   ```

4. **Test server certificate from client**:
   ```bash
   # Test certificate chain and validity
   echo | openssl s_client -connect your-domain.com:443 -servername your-domain.com 2>/dev/null | openssl x509 -text -noout
   
   # Check if certificate matches the domain
   echo | openssl s_client -connect your-domain.com:443 -servername your-domain.com 2>/dev/null | openssl x509 -noout -subject -issuer
   ```

5. **Verify certificate chain**:
   ```bash
   # Show the complete certificate chain
   echo | openssl s_client -connect your-domain.com:443 -servername your-domain.com -showcerts 2>/dev/null
   ```
### Ingress Issues

#### How to View Ingress Controller Logs

The Ingress controller logs are the primary source for diagnosing SSL certificate and routing issues. Here's how to access and interpret them:

**Basic Log Commands**:
```bash
# View all Ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Filter logs for specific ingress
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller | grep "test-cluster-ingress"
```

#### Common Ingress controller error logs and solutions

   **Error 1: Invalid TLS Secret (Domain Mismatch)**
   ```
   W0805 09:17:39.123456       1 controller.go:1234] Error getting SSL certificate "tigergraph/ingress-secret": local SSL certificate "tigergraph/ingress-secret" is invalid: x509: certificate is not valid for any names, but wanted to match "test-nginx.helm.tigergraph.dev"
   ```
   
   **Solution**:
   ```bash
   # Step 1: Check current certificate domain
   kubectl get secret ingress-secret -n tigergraph -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -text | grep -A 1 "Subject Alternative Name"
   
   # Step 2: Generate new certificate with correct domain
   DOMAIN="your-domain.com"
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout ~/tls_test-cluster.key -out ~/tls_test-cluster.crt \
     -subj "/CN=$DOMAIN" \
     -addext "subjectAltName = DNS:$DOMAIN"
   
   # Step 3: Update the secret
   kubectl create secret tls ingress-secret \
     --cert=~/tls_test-cluster.crt \
     --key=~/tls_test-cluster.key \
     --namespace tigergraph \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

   **Error 2: Missing TLS Secret**
   ```
   W0805 09:17:39.123456       1 controller.go:1234] Error getting SSL certificate "tigergraph/ingress-secret": secret "tigergraph/ingress-secret" does not exist
   ```
   
   **Solution**:
   ```bash
   # Step 1: Check if secret exists
   kubectl get secret ingress-secret -n tigergraph
   
   # Step 2: If secret doesn't exist, create it
   DOMAIN="your-domain.com"
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout ~/tls_test-cluster.key -out ~/tls_test-cluster.crt \
     -subj "/CN=$DOMAIN" \
     -addext "subjectAltName = DNS:$DOMAIN"
   
   kubectl create secret tls ingress-secret \
     --cert=~/tls_test-cluster.crt \
     --key=~/tls_test-cluster.key \
     --namespace tigergraph
   
   # Step 3: Verify secret creation
   kubectl get secret ingress-secret -n tigergraph
   ```

   **Error 3: Malformed Certificate**
   ```
   W0805 09:17:39.123456       1 controller.go:1234] Error getting SSL certificate "tigergraph/ingress-secret": error parsing certificate: x509: malformed certificate
   ```
   
   **Solution**:
   ```bash
   # Step 1: Check certificate format
   kubectl get secret ingress-secret -n tigergraph -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -text -noout
   
   # Step 2: If certificate is malformed, regenerate it properly
   DOMAIN="your-domain.com"
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout ~/tls_test-cluster.key -out ~/tls_test-cluster.crt \
     -subj "/CN=$DOMAIN" \
     -addext "subjectAltName = DNS:$DOMAIN"
   
   # Step 3: Verify certificate is valid
   openssl x509 -in ~/tls_test-cluster.crt -text -noout
   
   # Step 4: Update secret with valid certificate
   kubectl create secret tls ingress-secret \
     --cert=~/tls_test-cluster.crt \
     --key=~/tls_test-cluster.key \
     --namespace tigergraph \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

   **Error 4: Certificate Expired**
   ```
   W0805 09:17:39.123456       1 controller.go:1234] Error getting SSL certificate "tigergraph/ingress-secret": x509: certificate has expired or is not yet valid
   ```
   
   **Solution**:
   ```bash
   # Step 1: Check certificate dates
   kubectl get secret ingress-secret -n tigergraph -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -noout -dates
   
   # Step 2: Generate new certificate with extended validity
   DOMAIN="your-domain.com"
   openssl req -x509 -nodes -days 730 -newkey rsa:2048 \
     -keyout ~/tls_test-cluster.key -out ~/tls_test-cluster.crt \
     -subj "/CN=$DOMAIN" \
     -addext "subjectAltName = DNS:$DOMAIN"
   
   # Step 3: Verify new certificate dates
   openssl x509 -in ~/tls_test-cluster.crt -noout -dates
   
   # Step 4: Update secret
   kubectl create secret tls ingress-secret \
     --cert=~/tls_test-cluster.crt \
     --key=~/tls_test-cluster.key \
     --namespace tigergraph \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

## See also

- [Configure Services of Sidecar Containers](configure-services-of-sidecar-containers.md)
- [Kubernetes Ingress Documentation](https://kubernetes.io/docs/concepts/services-networking/ingress/)
- [NGINX Ingress Controller Documentation](https://kubernetes.github.io/ingress-nginx/) 