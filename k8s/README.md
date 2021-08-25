# Run TigerGraph in Kubernetes

## Prerequisites
- A running ```EKS/GKE/AKS``` cluster with worker nodes that meet the minimum hardware and software requirements to run TigerGraph
- The ```kubectl``` command-line tool **(v1.18.0+)**
- `kubectl` is configured for cluster access

## Customize deployment
You can use adjust the `kustomize.yaml` file to customize your deployment configurations. For your convenience, we provided a script (`./tg`) that allows you to make common customizations. Help text for the script is below:

```bash
USAGE:
   $0 K8S_PROVIDER [kustomize|create|delete|list|help] [OPTIONS]
   -n|--namespace :  set namespace to deploy TigerGraph cluster in. Default namespace is "default" 
   -s|--size :       set cluster size, default 1
   -v|--version :    set TigerGraph version, default is 3.2.0
   -l|--license :    set TigerGraph license, default is enterprise-free
   --ha :            set cluster replication factor; default is 1
   --pv :            set Persistent Volume size, default is 50 GB
   --prefix :        set Pod name with prefix
   
# Examples when working in eks:
   ## Generate manifest for deployment
     ./tg eks kustomize -n tigergraph --size 3 --ha 3
   ## Create cluster:
     ./tg eks create -n tigergraph -s 2 
   ## Delete cluster: 
     ./tg eks delete
   ## List clusters:
     ./tg eks list -n tigergraph
   ```
    

## Create deployment
After you have customized your `kustomize.yaml` file, you can run `kubectl apply -k` to create deployments directly. 
```bash
# Deploy in EKS
$ kubectl apply -k ./eks

# deploy in GKE
$ kubectl apply -k ./gke

# deploy in AKS
$ kubectl apply -k ./aks

# Use tg script with eks in tigergraph namespace 
$ ./tg eks create -n tigergraph --size 3 
```

Alternatively, you can use `kustomize` command in the `./tg` script to generate a manifest, and then deploy using the manifest. The manifest will be generated in the `deploy` directory.  

```bash
$ ./tg eks kustomize -s 3
$ kubectl apply -f deploy/tigergraph-eks.yaml
```

## Verify deployment
Run `kubectl get all -k app=tigergraph` to verify deployment:
   ```bash
      kubectl get all -l app=tigergraph
   ```
You should see a response similar to below:
```
   NAME               READY   STATUS    RESTARTS   AGE
   pod/tigergraph-0   1/1     Running   0          6d20h

   NAME                         TYPE           CLUSTER-IP       EXTERNAL-IP                                                              PORT(S)                          AGE
   service/tigergraph-service   LoadBalancer   10.100.214.243   a0ae52e0e62e54bf9b5c07d97deec5e2-982033604.us-east-1.elb.amazonaws.com   9000:30541/TCP,14240:31525/TCP   6d20h
```

Log in to the instances:
   ```bash
   # via kubectl
   kubectl exec -it tigergraph-0 -- /bin/bash
   
   # via ssh
   ip_m1=$(kubectl get pod -o wide |grep tigergraph-0| awk '{print $6}')
   ssh tigergraph@ip_m1
   
   # verify cluster status
   source ~/.bashrc
   gadmin status -v
   ```

   Work in GraphStudio by visiting the following URL in the browser. Change ```EXTERNAL-IP``` to the corresponding field in the output of the `kubectl get all` command:

   ```
   http://<EXTERNAL-IP>:14240
   ```

   Verify that RESTPP is up and running: ```EXTERNAL-IP```
   ```bash
   curl <EXTERNAL-IP>:9000/echo
   ```

