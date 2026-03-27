# How to integrate Envoy Gateway with TigerGraph

TigerGraph clusters can be exposed using the Kubernetes Gateway API with Envoy Gateway. This guide covers setting up Envoy Gateway with E2E TLS and mTLS for TigerGraph clusters.

- [How to integrate Envoy Gateway with TigerGraph](#how-to-integrate-envoy-gateway-with-tigergraph)
  - [Prerequisites](#prerequisites)
  - [Step 1: Install Envoy Gateway](#step-1-install-envoy-gateway)
  - [Step 2: Generate Certificates and Create Secrets](#step-2-generate-certificates-and-create-secrets)
  - [Step 3: Deploy TigerGraph with TLS](#step-3-deploy-tigergraph-with-tls)
  - [Step 4: Create Gateway and GatewayClass](#step-4-create-gateway-and-gatewayclass)
  - [Step 5: Create Backend with E2E TLS](#step-5-create-backend-with-e2e-tls)
  - [Step 6: Create HTTPRoute](#step-6-create-httproute)
  - [Step 7: Verify](#step-7-verify)
  - [Migrating from nginx Ingress](#migrating-from-nginx-ingress)
  - [Troubleshooting](#troubleshooting)

## Prerequisites

- TigerGraph Operator 1.7.1 or later
- TigerGraph 4.2.0 or later for TLS, 4.3.0 or later for mTLS
- Kubernetes 1.30 or later (validated versions: 1.30 - 1.34, see [version compatibility](../01-introduction/README.md)). Minimum 1.30 is required by both the Operator and Envoy Gateway v1.6.x.
- Envoy Gateway v1.6.3 or later
- `kubectl`, `helm`, and `openssl` installed
- A running TigerGraph cluster (see [Get Started](../02-get-started/get_started.md))

## Step 1: Install Envoy Gateway

Ensure your Envoy Gateway version is compatible with your Kubernetes version. Refer to the [Envoy Gateway compatibility matrix](https://gateway.envoyproxy.io/news/releases/matrix/) for supported combinations:

| Envoy Gateway | Kubernetes validated versions |
| ------------- | ----------------------------- |
| v1.6.x        | 1.30, 1.31, 1.32, 1.33        |
| v1.7.x        | 1.32, 1.33, 1.34, 1.35        |

Install the CRDs and controller:

```bash
# Install CRDs
helm pull oci://docker.io/envoyproxy/gateway-crds-helm --version v1.6.3 --untar
helm template eg-crds gateway-crds-helm \
  --set crds.gatewayAPI.enabled=true \
  --set crds.gatewayAPI.channel=experimental \
  --set crds.envoyGateway.enabled=true \
  | kubectl apply --server-side -f -

# Install controller
helm install eg oci://docker.io/envoyproxy/gateway-helm \
  --version v1.6.3 \
  --skip-crds \
  -n envoy-gateway-system --create-namespace \
  --set config.envoyGateway.extensionApis.enableBackend=true
```

Wait for the controller to be ready:

```bash
kubectl wait --timeout=5m -n envoy-gateway-system \
  deployment/envoy-gateway --for=condition=Available
```

> [!IMPORTANT]
> Use Envoy Gateway v1.6.3 or later. Earlier versions have a [known bug](https://github.com/envoyproxy/gateway/pull/7987) with `clientCertificateRef` namespace resolution. The `enableBackend=true` flag is required for backend TLS configuration.

## Step 2: Generate Certificates and Create Secrets

Generate SSL certificates and create Kubernetes secrets as described in [Configure Nginx TLS](../03-deploy/configure-nginx-tls.md). For mTLS (TigerGraph 4.3.0+), also follow the [Configure Nginx mTLS](../03-deploy/configure-nginx-tls.md#configure-nginx-mtls) section.

Additionally, create a client certificate secret for the Gateway to present to TG during mTLS:

```bash
kubectl create secret tls tg-gateway-client-cert \
  --cert=<client-cert-file> --key=<client-key-file> -n tigergraph
```

> [!NOTE]
> The `tg-gateway-client-cert` secret is only required for mTLS (4.3.0+). For TLS only, the server TLS secret is sufficient. If you do not need TLS at all, skip this step and Step 3.

## Step 3: Deploy TigerGraph with TLS

Configure TLS for TigerGraph's internal NGINX as described in [Configure Nginx TLS](../03-deploy/configure-nginx-tls.md). Use `nginxConfig` (recommended) or `postInitAction` if you need additional gadmin settings beyond SSL (e.g., RESTPP authentication, audit logging).

Deploy the cluster with `listener.type: ClusterIP` since all external routing will be handled by the Gateway API:

```yaml
  listener:
    type: ClusterIP
  nginxConfig:
    secretName: tg-server-tls            # Server certificate for TLS
    clientCertSecretName: tg-ca-cert     # CA certificate for mTLS verification (4.3.0+ only)
```

> [!IMPORTANT]
> When using `postInitAction`, you must restart `restpp`, `gsql`, `nginx`, and `gui` after applying the config. Restarting only `nginx` will break GUI authentication.

> [!NOTE]
> Operator versions prior to 1.7.1 have a bug that prevents creating clusters with `listener.type: ClusterIP` directly. This has been resolved in 1.7.1. If you are on an older version, deploy with `listener.type: LoadBalancer` first, then patch to `ClusterIP`:
> `kubectl patch tigergraph test-cluster -n tigergraph --type merge -p '{"spec":{"listener":{"type":"ClusterIP"}}}'`

Verify TLS is working before proceeding:

```bash
kubectl exec test-cluster-0 -n tigergraph -- curl -ks https://localhost:14240/restpp/echo
# Expected: {"error":false, "message":"Hello GSQL"}

kubectl exec test-cluster-0 -n tigergraph -- \
  curl -ks https://localhost:14240/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"tigergraph","password":"tigergraph"}'
# Expected: {"error":false, ...}
```

Verify the service is ClusterIP:

```bash
kubectl get svc test-cluster-nginx-external-service -n tigergraph
# TYPE should be ClusterIP, EXTERNAL-IP should be <none>
```

## Step 4: Create Gateway and GatewayClass

The `GatewayClass` defines which controller handles Gateway resources. The `Gateway` defines the external entry point with HTTPS and an HTTP-to-HTTPS redirect.

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: GatewayClass
metadata:
  name: eg
spec:
  controllerName: gateway.envoyproxy.io/gatewayclass-controller
---
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: tg-gateway
  namespace: tigergraph
spec:
  gatewayClassName: eg
  listeners:
    - name: https
      protocol: HTTPS
      port: 443
      tls:
        mode: Terminate
        certificateRefs:
          - kind: Secret
            name: tg-server-tls
      allowedRoutes:
        namespaces:
          from: Same
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes:
        namespaces:
          from: Same
```

Wait for the Gateway to receive an external address:

```bash
kubectl get gateway tg-gateway -n tigergraph -w
# Wait until ADDRESS is populated and PROGRAMMED: True
```

> [!NOTE]
> On AWS, ELB DNS propagation can take 2-5 minutes.

## Step 5: Create Backend with E2E TLS

The `Backend` resource configures how Envoy connects to TG's NGINX over HTTPS.

### TLS Only (TigerGraph 4.2.0+)

```yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: Backend
metadata:
  name: tg-backend
  namespace: tigergraph
spec:
  endpoints:
    - fqdn:
        hostname: test-cluster-nginx-external-service.tigergraph.svc.cluster.local
        port: 14240
  tls:
    caCertificateRefs:
      - group: ""
        kind: Secret
        name: tg-ca-cert
    sni: your-domain.com
```

### E2E mTLS (TigerGraph 4.3.0+)

If TG has mTLS enabled (`nginxConfig.clientCertSecretName`), the Gateway must also present a client certificate:

```yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: Backend
metadata:
  name: tg-backend
  namespace: tigergraph
spec:
  endpoints:
    - fqdn:
        hostname: test-cluster-nginx-external-service.tigergraph.svc.cluster.local
        port: 14240
  tls:
    caCertificateRefs:
      - group: ""
        kind: Secret
        name: tg-ca-cert
    clientCertificateRef:
      group: ""
      kind: Secret
      name: tg-gateway-client-cert
    sni: your-domain.com
```

* `caCertificateRefs` — Envoy verifies TG's server certificate against this CA
* `clientCertificateRef` — Envoy presents this certificate to TG's NGINX for mTLS (4.3.0+ only)
* `sni` — must match the CN or SAN in the server certificate

> [!IMPORTANT]
> All secrets must be in the same namespace as the Backend resource.

### Without TLS

If TigerGraph does not have TLS enabled, you can skip the `Backend` resource and reference the Service directly in the HTTPRoute (see Step 6).

## Step 6: Create HTTPRoute

Route HTTPS traffic to the TG backend and redirect HTTP to HTTPS:

### With Backend (TLS/mTLS enabled)

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: tg-route
  namespace: tigergraph
spec:
  parentRefs:
    - name: tg-gateway
      sectionName: https
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /
      backendRefs:
        - group: gateway.envoyproxy.io
          kind: Backend
          name: tg-backend
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: tg-http-redirect
  namespace: tigergraph
spec:
  parentRefs:
    - name: tg-gateway
      sectionName: http
  rules:
    - filters:
        - type: RequestRedirect
          requestRedirect:
            scheme: https
            statusCode: 301
```

### Without TLS (plain HTTP)

If TigerGraph does not have TLS enabled, the HTTPRoute can reference the ClusterIP Service directly without a Backend resource:

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: tg-route
  namespace: tigergraph
spec:
  parentRefs:
    - name: tg-gateway
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /
      backendRefs:
        - name: test-cluster-nginx-external-service
          port: 14240
```

## Step 7: Verify

```bash
export GATEWAY_IP=$(kubectl get gateway tg-gateway -n tigergraph \
  -o jsonpath='{.status.addresses[0].value}')

# HTTPS works
curl -k https://$GATEWAY_IP/restpp/echo
# Expected: {"error":false, "message":"Hello GSQL"}

# HTTP redirects to HTTPS
curl -v http://$GATEWAY_IP/restpp/echo 2>&1 | grep "301\|location"

# GUI login works through the Gateway
curl -k https://$GATEWAY_IP/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"<replace-username>","password":"<replace-password>"}'
```

## Migrating from nginx Ingress

For existing clusters using `listener.type: Ingress` with the nginx Ingress Controller:

1. Install Envoy Gateway alongside existing nginx (Step 1) — no impact to production traffic
2. Create Gateway and HTTPRoute (Steps 4-6) — new routing path, not yet serving traffic
3. Validate using `curl --resolve` to test through the Gateway without changing DNS
4. Switch TG CR from `Ingress` to `ClusterIP` — operator deletes the Ingress resource
5. Update DNS to point to the Envoy Gateway's address — traffic switches to new path
6. Decommission nginx Ingress Controller once DNS has fully propagated

> [!NOTE]
> If the existing cluster does not use TLS, skip Steps 2, 3, and 5 (Backend). The HTTPRoute can reference the ClusterIP Service directly (see "Without TLS" in Step 6).

## Troubleshooting

**Gateway has no ADDRESS:**
Check Envoy Gateway controller logs and verify the GatewayClass is accepted:
```bash
kubectl logs -n envoy-gateway-system deployment/envoy-gateway --tail=20
kubectl get gatewayclass eg
```

**HTTPRoute not Accepted:**
Verify `parentRefs.name` matches the Gateway name and the Gateway's `allowedRoutes` permits the namespace.

**Backend shows `InvalidBackendRef`:**
Ensure `enableBackend=true` was set during Envoy Gateway installation. Check the HTTPRoute status:
```bash
kubectl describe httproute tg-route -n tigergraph
```

**`clientCertificateRef` namespace error:**
Upgrade to Envoy Gateway v1.6.3 or later. All secrets must be in the same namespace as the Backend.

**`Forbidden: Client certificate verification failed`:**
The client certificate is not signed by the CA configured in `clientCertSecretName`. Verify:
```bash
openssl verify -CAfile ca-cert.pem client-cert.pem
```
