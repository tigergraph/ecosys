# How to integrate the envoy sidecar with TG Pod

Starting from Operator version 0.0.6, we support adding sidecar containers to the TG Pod. This guide is dedicated to the integration process of the envoy sidecar with the TG Pod. To proceed, please ensure that you have Operator version 0.0.6 or a newer version installed. Additionally, please note that this document does not delve into the intricacies of envoy, such as TLS configuration. Instead, its primary focus is to describe the configuration of envoy sidecar containers for accessing TG services.

## Configuration of Envoy sidecar container

The initial step involves the creation of a ConfigMap resource and its subsequent mounting onto the pod as the Envoy's configuration.

Below is an illustrative example of the ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: sidecar-test-configmap
  labels:
    app: sidecar-test
data:
  envoy.yaml: |
    static_resources:
      listeners:
      - name: listener_0
        address:
          socket_address:
            address: 0.0.0.0
            port_value: 12000
        filter_chains:
        - filters:
          - name: envoy.filters.network.http_connection_manager
            typed_config:
              "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
              stat_prefix: ingress_http
              access_log:
              - name: envoy.access_loggers.stdout
                typed_config:
                  "@type": type.googleapis.com/envoy.extensions.access_loggers.stream.v3.StdoutAccessLog
              http_filters:
              - name: envoy.filters.http.router
                typed_config:
                  "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router
              route_config:
                name: local_route
                virtual_hosts:
                - name: nginx_service
                  domains:
                  - "*"
                  routes:
                  - match:
                      prefix: "/"
                    route:
                      cluster: nginx_service
      clusters:
      - name: nginx_service
        type: LOGICAL_DNS
        # Comment out the following line to test on v6 networks
        dns_lookup_family: V4_ONLY
        load_assignment:
          cluster_name: nginx_service
          endpoints:
          - lb_endpoints:
            - endpoint:
                address:
                  socket_address:
                    address: 127.0.0.1
                    port_value: 14240
```

* Add listener to forward the requests to the API gateway of TG

  * `listener_1` is listening on port 12000 which is used for routing to the Nginx service, in `rout_config` part, we use cluster nginx\_service as the route.

* Add cluster to configure the endpoint for the above listener

  * cluster `nginx_service` specifies the `endpoint` to address 127.0.0.1 and port 14240 where the NGINX service will listen.

## Add `sidecarContainers` and `customVolumes` to the TigerGraph CR

```yaml
  sidecarContainers:
    - image: envoyproxy/envoy:v1.26.0
      name: envoy-sidecar-container
      resources:
        requests:
          memory: "512Mi"
          cpu: "500m"
        limits:
          memory: "512Mi"
          cpu: "500m"
      ports:
        - name: tg-nginx
          containerPort: 12000
          protocol: TCP
      volumeMounts:
        - name: sidecar-config
          mountPath: "/etc/envoy"
          readOnly: true
  customVolumes:
    - name: sidecar-config
      configMap:
        name: sidecar-test-configmap
```

## Validation

Finally, to ensure the proper functionality of the Envoy sidecar service and its access to the RESTPP and Metric services, we will establish a Kubernetes (K8s) Service. This service will facilitate the verification process.

Assume that the TG cluster name is `test-cluster`, the `selector` of the service should include label `tigergraph.com/cluster-pod: test-cluster`

Furthermore, for those aiming to access the web console, an additional label `tigergraph.com/gui-service: "true"` must be included within the selector of the Service.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: envoy-sidecar-nginx-service
  labels:
    app: envoy-sidecar-test
spec:
  type: LoadBalancer
  selector:
    tigergraph.com/cluster-pod: test-cluster
    # optional,only for web cosole accessing.
    # tigergraph.com/gui-service: "true"
  ports:
    - name: nginx
      port: 12000
      targetPort: 12000
```

* RESTPP

```bash
curl http://${LBS_EXTERNAL_IP}:12000/restpp/echo
# it will return {"error":false, "message":"Hello GSQL"} if accessing successfully.
```

* Metric

```bash
# it will return the latest metrics of cpu and mem.
curl http://${LBS_EXTERNAL_IP}:12000/informant/metrics/get/cpu-memory -d '{"ServiceDescriptor":{"ServiceName":"gse","Partition": 1,"Replica":1}}'
```

* WEB API and console

```bash
# it will return {"error":false,"message":"pong","results":null} if accessing successfully
curl http://${LBS_EXTERNAL_IP}:12000/api/ping

# Web console
# open the the url http://${LBS_EXTERNAL_IP}:12000 in Chrome or other browser.
```
