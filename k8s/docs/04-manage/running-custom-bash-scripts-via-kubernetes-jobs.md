# Running Custom Bash Scripts in a TigerGraph Cluster via Kubernetes Jobs

## Overview

After deploying a TigerGraph cluster on Kubernetes, customers often need to perform operations that cannot be configured through the TigerGraph Custom Resource (CR), such as:

- Creating GSQL roles and managing user permissions
- Executing custom GSQL scripts for data operations
- Running maintenance tasks and administrative commands
- Performing cluster-specific configurations

While these tasks can technically be performed by manually logging into the TigerGraph pod, this approach fails to meet automation and operational efficiency standards. This document provides a standardized procedure for running custom bash scripts in a TigerGraph cluster using Kubernetes Jobs.

## Prerequisites

Before proceeding, ensure you have:

1. A deployed TigerGraph cluster on Kubernetes
2. `kubectl` configured to access your cluster
3. Appropriate RBAC permissions to create Jobs and ConfigMaps
4. SSH key secret configured for the TigerGraph cluster
5. Knowledge of the cluster's namespace and service names

## Architecture Overview

The solution uses Kubernetes Jobs to execute custom scripts within the TigerGraph cluster environment. The approach involves:

1. **Kubernetes Job**: A one-time execution container that runs your custom script
2. **ConfigMap**: Stores your custom script content
3. **Secret**: Contains SSH credentials for cluster access
4. **Volume Mounts**: Provides access to scripts and credentials

## Step-by-Step Procedure

### Step 1: Prepare Your Custom Script

Create your custom bash script. For example, to create GSQL roles:

```bash
#!/bin/bash
# create-gsql-roles.sh

set -eo pipefail

# Connect to the TigerGraph cluster
export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH

echo "Creating GSQL roles..."

# Create read-only role
gsql -c "CREATE ROLE readonly_role"

# Grant permissions to roles
gsql -c "GRANT READ  ON ALL QUERIES IN GRAPH social TO readonly_role"

echo "GSQL roles created successfully"

# Drop a role
# gsql -c "DROP ROLE readonly_role"
```

### Step 2: Create a ConfigMap for Your Script

Create a ConfigMap containing your script:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: custom-script-cm
  namespace: tigergraph
data:
  custom-script.sh: |
    #!/bin/bash
    # Your custom script content here
    set -eo pipefail
    
    export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH
    
    echo "Starting custom script execution..."
    
    # Your script logic here
    gsql -c "SHOW USERS"
    
    echo "Custom script completed successfully"
```

Apply the ConfigMap:

```bash
kubectl apply -f custom-script-cm.yaml
```

### Step 3: Create the Kubernetes Job

> [!IMPORTANT]
> **​Kubernetes Job Namespace Requirement​**
>
> The Kubernetes Job ​must be created in the same namespace as the TigerGraph cluster.
>
> ​Why?​​ The Job requires access to the Kubernetes Secret containing the TigerGraph cluster's private SSH key.
>
> Secrets are ​namespace-scoped, so Jobs in different namespaces ​cannot access these credentials​ and will fail to authenticate

Create a Kubernetes Job that will execute your script:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: custom-script-job
  namespace: tigergraph
spec:
  template:
    metadata: {}
    spec:
      containers:
      - name: script-runner
        image: docker.io/tigergraph/tigergraph-k8s-init:1.6.0
        imagePullPolicy: IfNotPresent
        command:
        - /bin/bash
        - -c
        - |
          set -eo pipefail
          PRIVATE_KEY_FILE=/etc/private-key-volume/tigergraph_rsa
          SERVICE_NAME=${CLUSTER_NAME}-internal-service
          
          echo "Copying script to cluster..."
          scp -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no -P ${SSH_PORT} \
            /tmp/custom-script/custom-script.sh \
            tigergraph@${CLUSTER_NAME}-0.${SERVICE_NAME}.${NAMESPACE}:/home/tigergraph/custom-script.sh > /dev/null
          
          echo "Making script executable..."
          ssh -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no \
            -p ${SSH_PORT} tigergraph@${CLUSTER_NAME}-0.${SERVICE_NAME}.${NAMESPACE} \
            "chmod +x /home/tigergraph/custom-script.sh"
          
          echo "Running script in cluster..."
          ssh -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no \
            -p ${SSH_PORT} tigergraph@${CLUSTER_NAME}-0.${SERVICE_NAME}.${NAMESPACE} <<EOF
          /home/tigergraph/custom-script.sh
          EOF
        env:
        - name: NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: CLUSTER_NAME
          value: "your-cluster-name"  # Replace with your actual cluster name
        - name: SSH_PORT
          value: "10022"
        resources:
          limits:
            cpu: 500m
            memory: 256Mi
          requests:
            cpu: 100m
            memory: 128Mi
        securityContext:
          privileged: false
          runAsUser: 1000
          runAsGroup: 1000
        volumeMounts:
        - mountPath: /tmp/custom-script
          name: custom-script-cm
          readOnly: true
        - mountPath: /etc/private-key-volume
          name: private-key-volume
          readOnly: true
      restartPolicy: Never
      imagePullSecrets:
      - name: tigergraph-image-pull-secret
      securityContext:
        fsGroup: 1000
      terminationGracePeriodSeconds: 30
      volumes:
      - name: custom-script-cm
        configMap:
          name: custom-script-cm
          defaultMode: 0555
      - name: private-key-volume
        secret:
          secretName: ssh-key-secret
          defaultMode: 0420
          items:
          - key: private-ssh-key
            path: tigergraph_rsa
          - key: public-ssh-key
            path: tigergraph_rsa.pub
```

### Step 4: Deploy and Monitor the Job

Deploy the job:

```bash
kubectl apply -f custom-script-job.yaml
```

Monitor the job execution:

```bash
# Check job status
kubectl get jobs -n tigergraph

# View job logs
kubectl logs -f job/custom-script-job -n tigergraph

# Check job details
kubectl describe job custom-script-job -n tigergraph
```

### Step 5: Clean Up

After successful execution, clean up the job:

```bash
kubectl delete job custom-script-job -n tigergraph
```

## Common Use Cases and Examples

### Example 1: Creating GSQL Roles and Users

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gsql-roles-script-cm
  namespace: tigergraph
data:
  gsql-roles.sh: |
    #!/bin/bash
    set -eo pipefail
    export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH
    
    echo "Creating GSQL roles and users..."
    
    echo "Creating GSQL roles..."

    # Create read-only role
    gsql -c "CREATE ROLE readonly_role"

    # Grant permissions to roles
    gsql -c "GRANT READ  ON ALL QUERIES IN GRAPH social TO readonly_role"

    echo "GSQL roles created successfully"
```

### Example 2: Running Custom GSQL Queries

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: custom-gsql-script-cm
  namespace: tigergraph
data:
  custom-gsql.sh: |
    #!/bin/bash
    set -eo pipefail
    export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH
    
    echo "Running custom GSQL queries..."

    cat << EOF > person.csv
    name,gender,age,state
    Tom,male,40,ca
    Dan,male,34,ny
    Jenny,female,25,tx
    Kevin,male,28,az
    Amily,female,22,ca
    Nancy,female,20,ky
    Jack,male,26,fl
    EOF

    cat << EOF > friendship.csv
    person1,person2,date
    Tom,Dan,2017-06-03
    Tom,Jenny,2015-01-01
    Dan,Jenny,2016-08-03
    Jenny,Amily,2015-06-08
    Dan,Nancy,2016-01-03
    Nancy,Jack,2017-03-02
    Dan,Kevin,2015-12-30
    EOF

    cat << EOF > gsql101.gsql
    BEGIN
    CREATE VERTEX person (
        PRIMARY_ID name STRING,
        name STRING,
        age INT,
        gender STRING,
        state STRING
    )
    END

    CREATE UNDIRECTED EDGE friendship (FROM person, TO person, connect_day DATETIME)

    CREATE GRAPH social (person, friendship)

    USE GRAPH social
    BEGIN
    CREATE LOADING JOB load_social FOR GRAPH social {
      DEFINE FILENAME file1="/home/tigergraph/person.csv";
      DEFINE FILENAME file2="/home/tigergraph/friendship.csv";

      LOAD file1 TO VERTEX person VALUES ($"name", $"name", $"age", $"gender", $"state") USING header="true", separator=",";
      LOAD file2 TO EDGE friendship VALUES (\$0, \$1, \$2) USING header="true", separator=",";
    }
    END
    RUN LOADING JOB load_social

    CREATE QUERY hello(VERTEX<person> p) {
      Start = {p};
      Result = SELECT tgt
              FROM Start:s-(friendship:e) ->person:tgt;
      PRINT Result;
    }

    INSTALL QUERY hello

    RUN QUERY hello("Tom")
    EOF

    gsql /home/tigergraph/gsql101.gsql

    echo "Custom GSQL operations completed successfully"
```

Complete YAML example:

[custom-script-job.yaml](../10-samples/manage/custom-script-job.yaml)

### Example 3: Cluster Maintenance Tasks

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: maintenance-script-cm
  namespace: tigergraph
data:
  maintenance.sh: |
    #!/bin/bash
    set -eo pipefail
    export PATH=/home/tigergraph/tigergraph/app/cmd:$PATH
    
    echo "Starting cluster maintenance..."
    
    # Check cluster status
    gadmin status -v
    
    # Clean up old logs
    find /home/tigergraph/tigergraph/log -name "*.log" -mtime +7 -delete
    
    # Check disk usage
    df -h /home/tigergraph/tigergraph/data

    echo "Maintenance completed successfully"
```

## Using CronJobs for Scheduled Tasks

For recurring tasks, use CronJobs instead of Jobs:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: scheduled-maintenance
  namespace: tigergraph
spec:
  schedule: "0 2 * * *"  # Run daily at 2 AM
  jobTemplate:
    metadata: {}
    spec:
      template:
        spec:
          containers:
          - name: maintenance-runner
            image: docker.io/tigergraph/tigergraph-k8s-init:1.6.0
            imagePullPolicy: IfNotPresent
            command:
            - /bin/bash
            - -c
            - |
              # Your maintenance script here
              set -eo pipefail
              PRIVATE_KEY_FILE=/etc/private-key-volume/tigergraph_rsa
              SERVICE_NAME=${CLUSTER_NAME}-internal-service
              
              ssh -i $PRIVATE_KEY_FILE -o StrictHostKeyChecking=no \
                -p ${SSH_PORT} tigergraph@${CLUSTER_NAME}-0.${SERVICE_NAME}.${NAMESPACE} <<EOF
              export PATH=/home/tigergraph/tigergraph/app/cmd:\$PATH
              gadmin status -v
              EOF
            env:
            - name: NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: CLUSTER_NAME
              value: "your-cluster-name"
            - name: SSH_PORT
              value: "10022"
            resources:
              limits:
                cpu: 500m
                memory: 256Mi
              requests:
                cpu: 100m
                memory: 128Mi
            volumeMounts:
            - mountPath: /etc/private-key-volume
              name: private-key-volume
              readOnly: true
          restartPolicy: Never
          volumes:
          - name: private-key-volume
            secret:
              secretName: ssh-key-secret
              defaultMode: 0420
              items:
              - key: private-ssh-key
                path: tigergraph_rsa
              - key: public-ssh-key
                path: tigergraph_rsa.pub
```

## Best Practices

### 1. Script Design

- Always use `set -eo pipefail` for error handling
- Include proper logging and error messages
- Test scripts in a non-production environment first
- Use idempotent operations when possible

### 2. Security

- Store sensitive data in Kubernetes Secrets, not ConfigMaps
- Use least-privilege principles for RBAC
- Validate input parameters

### 3. Resource Management

- Set appropriate resource limits and requests
- Monitor job execution time and resource usage
- Clean up completed jobs to prevent resource accumulation
- Use appropriate image pull policies

### 4. Monitoring and Logging

- Implement comprehensive logging in your scripts
- Monitor job status and logs
- Set up alerts for job failures
- Use structured logging for better observability

## Troubleshooting

### Common Issues

#### Job Fails to Start

```bash
# Check job status
kubectl describe job your-job-name -n tigergraph

# Check pod logs
kubectl logs job/your-job-name -n tigergraph
```

#### SSH Connection Issues

- Verify SSH key secret exists and is properly configured
- Check that the cluster is running and accessible
- Ensure the SSH port is correct (default: 10022)

#### Script Execution Errors

- Verify script syntax and permissions
- Check that required tools are available in the container
- Ensure proper error handling in the script

#### Resource Issues

- Check resource limits and requests
- Monitor cluster resource availability
- Adjust resource allocation if needed

### Debugging Commands

```bash
# Get detailed job information
kubectl describe job your-job-name -n tigergraph

# View job logs
kubectl logs job/your-job-name -n tigergraph

# Check pod status
kubectl get pods -n tigergraph -l job-name=your-job-name

# Execute interactive shell in job pod
kubectl exec -it job/your-job-name -n tigergraph -- /bin/bash
```

## Security Considerations

1. **SSH Key Management**: Ensure SSH keys are properly secured
2. **Network Policies**: Implement network policies to restrict job access
3. **RBAC**: Use appropriate RBAC permissions for job creation and execution
4. **Secret Management**: Store sensitive data in Kubernetes Secrets
5. **Image Security**: Use trusted base images and scan for vulnerabilities

## Conclusion

This standardized procedure provides a robust, automated approach to running custom bash scripts in TigerGraph clusters via Kubernetes Jobs. By following these guidelines, you can achieve operational efficiency while maintaining security and reliability standards.

For additional support or advanced use cases, refer to the TigerGraph documentation or contact your system administrator.
