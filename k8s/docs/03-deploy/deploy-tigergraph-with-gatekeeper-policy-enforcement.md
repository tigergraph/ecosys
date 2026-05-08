# Deploy TigerGraph on Kubernetes with Gatekeeper Policy Controller

## Overview

As Kubernetes environments grow in complexity and scale, enforcing consistency, security, and compliance across workloads becomes critical. A policy controller like [Open Policy Agent (OPA) Gatekeeper](https://open-policy-agent.github.io/gatekeeper/website/) helps achieve this by enforcing policies at the API server level before resources are admitted into the cluster.

**How It Works:**

- **Constraint Templates** define reusable policy logic in Rego.
- **Constraints** instantiate those templates and apply them to specific resources.
- The Gatekeeper admission webhook intercepts API requests and evaluates them against these policies.
- Violations can be configured to block requests or simply warn (audit mode).

> [!NOTE]  
> Starting with TigerGraph Operator 1.6.0 and TigerGraph versions 3.10.3, 4.1.3, and 4.2.0, the Operator supports deploying TigerGraph on Kubernetes with the Gatekeeper Policy Controller.

## Prerequisites

Ensure you have the following before proceeding:

- [Helm](https://helm.sh/docs/intro/install/): Version >= 3.7.0
- [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/): Version >= 1.23
- An existing Kubernetes cluster with appropriate permissions

## Install Gatekeeper

> [!NOTE]  
> You can skip the section below if a policy controller is already installed.

You must have cluster admin permissions to install Gatekeeper.

To deploy a released version of Gatekeeper with a prebuilt image, run:

```bash
kubectl apply -f https://raw.githubusercontent.com/open-policy-agent/gatekeeper/v3.19.1/deploy/gatekeeper.yaml
```

For other installation methods, refer to the [Gatekeeper official documentation](https://open-policy-agent.github.io/gatekeeper/website/docs/install).

## Install Constraint Templates and Constraints

Before defining a constraint, you must first define a ConstraintTemplate, which describes both the Rego policy and the schema for the constraint. The schema allows you to fine-tune constraint behavior, similar to function arguments.

> [!NOTE]  
> You can skip the section below if a policy constraint templates are already installed.

Below, we use GKE Constraint Templates and constraints as examples. You can define your own Constraint Templates to customize your policies.

### Install GKE Constraint Templates

```bash
kubectl apply -f https://github.com/tigergraph/ecosys/blob/master/k8s/docs/10-samples/deploy/gke-constraints-templates.yaml
```

### Install GKE Constraints

> [!NOTE]  
> You can skip the section below if the policy constraints are already installed.

- **psp-v2022**

    ```bash
    kubectl apply -k https://github.com/GoogleCloudPlatform/gke-policy-library.git/bundles/psp-v2022
    ```

- **pss-baseline-v2022**

    ```bash
    kubectl apply -k https://github.com/GoogleCloudPlatform/gke-policy-library.git/bundles/pss-baseline-v2022
    ```

- **pss-restricted-v2022**

    ```bash
    kubectl apply -k https://github.com/GoogleCloudPlatform/gke-policy-library.git/anthos-bundles/pss-restricted-v2022
    ```

- **Install K8sRequiredResources constraint**

    ```bash
    cat <<EOF | kubectl apply -f -
    apiVersion: constraints.gatekeeper.sh/v1beta1
    kind: K8sRequiredResources
    metadata:
      name: container-must-have-limits-and-requests
    spec:
      match:
        kinds:
        - apiGroups: [""]
          kinds: ["Pod"]
        excludedNamespaces:
        - kube-system
        - monitoring-stack
        - cert-manager
        - ingress-nginx
        - tg-monitoring
      parameters:
        limits:
        - cpu
        - memory
        requests:
        - cpu
        - memory
    EOF
    ```

- **Patch the enforcementAction of all policies**

  There are three enforcement actions: `deny` (default), `dryrun`, and `warn`.

  - `deny`: Prevents the operation if there is a violation.
  - `dryrun`: Monitors violations without blocking transactions.
  - `warn`: Similar to `dryrun`, but provides immediate admission-time warnings.

    It is recommended to use `warn` or `dryrun` when testing new constraints or during migrations.

    ```bash
    export GKE_POLICY_ACTION=deny
    policy_list=(psp-v2022 pss-baseline-v2022 pss-restricted-v2022)
    for bundle_name in "${policy_list[@]}"; do
      constraint_list=$(kubectl get constraint -l policycontroller.gke.io/bundleName=$bundle_name -o name)
      echo "$constraint_list" | xargs -I {} kubectl patch {} --type='json' \
        -p='[{"op":"replace","path":"/spec/enforcementAction","value":"'$GKE_POLICY_ACTION'"}]'
    done
    ```

    > [!IMPORTANT]  
    > TigerGraph containers need to write some configurations under the root filesystem. You must set the enforcement action for the `K8sPSPReadOnlyRootFilesystem` constraint to `dryrun`.

    Set the action of the `K8sPSPReadOnlyRootFilesystem` constraint to `dryrun`:

    ```bash
    kubectl patch K8sPSPReadOnlyRootFilesystem psp-v2022-psp-readonlyrootfilesystem --type='json' -p='[{"op":"replace","path":"/spec/enforcementAction","value":"dryrun"}]'
    ```

- **Exclude constraints for specific namespaces**

    Replace `namespace1` and `namespace2` with your actual namespaces to exclude:

    ```bash
    policy_list=(psp-v2022 pss-baseline-v2022 pss-restricted-v2022)
    for bundle_name in "${policy_list[@]}"; do
      constraint_list=$(kubectl get constraint -l policycontroller.gke.io/bundleName=$bundle_name -o name)
      for constraint in $constraint_list; do
        if ! kubectl get $constraint -o yaml | grep excludedNamespaces; then
          kubectl patch $constraint --type='merge' -p='{"spec":{"match":{"excludedNamespaces":["namespace1","namespace2"]}}}'
        else
          kubectl patch $constraint --type='json' -p='[{"op": "add", "path": "/spec/match/excludedNamespaces/-", "value": "namespace1"},{"op": "add", "path": "/spec/match/excludedNamespaces/-", "value": "namespace2"}]'
        fi
      done
    done
    ```

## Install TigerGraph Operator

> [!IMPORTANT]  
> If you have installed the Gatekeeper controller and enabled constraints, you must use TigerGraph Operator version 1.6.0 or above.

Starting with TigerGraph Operator version 1.6.0, Gatekeeper policy support is enabled by default. You can install the TigerGraph Operator by following any of these guides:

- [Deploy TigerGraph on AWS EKS](../03-deploy/tigergraph-on-eks.md)
- [Deploy TigerGraph on Google Cloud GKE](../03-deploy/tigergraph-on-gke.md)
- [Deploy TigerGraph on Red Hat OpenShift](../03-deploy/tigergraph-on-openshift.md)
- [Deploy TigerGraph on Azure Kubernetes Service (AKS)](../03-deploy/tigergraph-on-aks.md)
- [Deploy TigerGraph on K8s without internet access](../03-deploy/deploy-without-internet.md)
- [Deploy TigerGraph Operator on K8s using Helm](../03-deploy/deploy-operator-with-helm.md)

## Deploy TigerGraph with a Specific Security Context

To comply with Gatekeeper policies, you must explicitly set the security context for TigerGraph pods.

If you use the TigerGraph CR to deploy a TigerGraph cluster, add the following configuration:

```yaml
securityContext:
  privileged: false
  runAsGroup: 1000
  runAsUser: 1000
  runAsNonRoot: true
  seccompProfile:
    type: RuntimeDefault
  allowPrivilegeEscalation: false
  capabilities:
    drop:
    - ALL
```

**Full example YAML resource:**

```yaml
apiVersion: graphdb.tigergraph.com/v1alpha1
kind: TigerGraph
metadata:
  name: test-cluster
spec:
  ha: 2
  image: tigergraph/tigergraph-k8s:4.2.2
  imagePullPolicy: Always
  imagePullSecrets:
    - name: tigergraph-image-pull-secret
  license: <YOUR_LICENSE>
  listener:
    type: LoadBalancer
  privateKeyName: ssh-key-secret
  replicas: 4
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
          storage: 10G
      storageClassName: standard
      volumeMode: Filesystem
  securityContext:
    privileged: false
    runAsGroup: 1000
    runAsUser: 1000
    runAsNonRoot: true
    seccompProfile:
      type: RuntimeDefault
    allowPrivilegeEscalation: false
    capabilities:
      drop:
      - ALL
```

If you are using the `kubectl-tg` plugin to create a TigerGraph cluster, create a security context configuration file named `policy-security-context.yaml`:

```yaml
securityContext:
  privileged: false
  runAsGroup: 1000
  runAsUser: 1000
  runAsNonRoot: true
  seccompProfile:
    type: RuntimeDefault
  allowPrivilegeEscalation: false
  capabilities:
    drop:
    - ALL
```

Then, create the TigerGraph cluster using:

```bash
kubectl tg create --docker-image-repo tigergraph --cluster-name ${YOUR_CLUSTER_NAME} --private-key-secret ${YOUR_SSH_KEY_SECRET_NAME} --size 4 --ha 2 --version ${TG_CLUSTER_VERSION_DEFAULT} --license ${LICENSE} \
--storage-class ${STORAGE_CLASS} --storage-size 100G --cpu 6000m --memory 12Gi --cpu-limit 6000m --memory-limit 12Gi --namespace \
${YOUR_NAMESPACE} --security-context policy-security-context.yaml
```
