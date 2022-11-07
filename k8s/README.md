# Run Tigergraph in EKS/GKE/AKS

## Getting Started

### Prerequisites
    Please ensure the following dependencies are already fulfilled before starting
    - A running ```EKS/GKE/AKS``` cluster
    - The ```kubectl``` command-line tool **(v1.18.0+)**
    - AWS/GCP/Azure account to manage kubernetes resource
    - AWS EBS CSI driver
### Verify EBS_CSI_ADDON Installation Status
Important: If you have a 1.22 or earlier cluster that you currently run pods on that use Amazon EBS volumes, and you don't currently have this driver installed on your cluster, then be sure to install this driver to your cluster before updating the cluster to 1.23.

Following the instructions on AWS documentation to add EBS CSI add-on before proceed.
https://docs.aws.amazon.com/eks/latest/userguide/managing-ebs-csi.html

Use ```kubectl get pods -n kube-system``` to check if EBS CSI driver is running. An example output is
```
NAME                                  READY   STATUS    RESTARTS        AGE
...
coredns-5948f55769-kcnvx              1/1     Running   0               3d6h
coredns-5948f55769-z7mbr              1/1     Running   0               3d6h
ebs-csi-controller-75598cd6f4-48dp8   6/6     Running   0               3d4h
ebs-csi-controller-75598cd6f4-sqbhw   6/6     Running   4 (2d11h ago)   3d4h
ebs-csi-node-9cmbj                    3/3     Running   0               3d4h
ebs-csi-node-g65ns                    3/3     Running   0               3d4h
ebs-csi-node-qzflk                    3/3     Running   0               3d4h
ebs-csi-node-x2t22                    3/3     Running   0               3d4h
...
```
### Deployment Steps
   ```bash
   #create cluster namespace
   kubectl create ns tigergraph
   # deploy in EKS
   kubectl apply -k ./eks

   # deploy in GKE
   kubectl apply -k ./gke

   # deploy in AKS
   kubectl apply -k ./aks

   # use tg script with eks in tigergraph namespace 
   ./tg eks create -n tigergraph --size 3 
   ```
### Verify the Tigergraph Status
   ```bash
      kubectl get all -l app=tigergraph -n tigergraph
   ```
   Response similar as below :
   ```
   NAME               READY   STATUS    RESTARTS   AGE
   pod/tigergraph-0   1/1     Running   0          6d20h

   NAME                         TYPE           CLUSTER-IP       EXTERNAL-IP                                                              PORT(S)                          AGE
   service/tigergraph-service   LoadBalancer   10.100.214.243   a0ae52e0e62e54bf9b5c07d97deec5e2-982033604.us-east-1.elb.amazonaws.com   9000:30541/TCP,14240:31525/TCP   6d20h
   ```
   Login to the instances
   ```bash
   # use kubectl
   kubectl exec -it tigergraph-0 -n tigergraph -- /bin/bash
   # use ssh
   ip_m1=$(kubectl get pod -o wide |grep tigergraph-0| awk '{print $6}')
   ssh tigergraph@ip_m1
   # verify the cluster status
   source ~/.bashrc
   gadmin status -v
   # verify gsql
   gsql ls
   ```
   Try GraphStudio UI, change the url accordingly as upper output ```EXTERNAL-IP``` 
   ```
   http://a0ae52e0e62e54bf9b5c07d97deec5e2-982033604.us-east-1.elb.amazonaws.com:14240
   ```

   Try Tigergraph Rest API, change the url accordingly as upper output ```EXTERNAL-IP```
   ```bash
   curl http://a0ae52e0e62e54bf9b5c07d97deec5e2-982033604.us-east-1.elb.amazonaws.com:9000/echo
   ```
### Kustomize the TG setting
   You can use adjust the kustomize yaml file to change the TG setting. For regular minor changes, strongly recommend to customize them with ```tg``` script as below.
   ```bash
   USAGE:
      $0 K8S_PROVIDER [kustomize|create|delete|list|help] [OPTIONS]
     -n|--namespace :  set namespace to deploy TG cluster  
     -s|--size :       set TG cluster size, default 1
     -v|--version :    set TG cluster version,default as 3.2.0
     -l|--license :    set TG cluster license, default as free tie
     --ha :            set TG cluster ha setting, default 1
     --pv :            set Persistent volume size, default as 50
     --prefix :        set Pod name with prefix
   
   # Examples when working in eks:
   ## Generate the manifest for deployment
     ./tg eks kustomize -n tigergraph --size 3 --ha 3
   ## Create TG cluster:
     ./tg eks create -n tigergraph -s 2 
   ## Delete TG cluster: 
     ./tg eks delete
   ## List TG cluster:
     ./tg eks list -n tigergraph
   ```
    
