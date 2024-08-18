<h1>TigerGraph Operator API Reference</h1>
<h2 id="graphdb.tigergraph.com/v1alpha1">graphdb.tigergraph.com/v1alpha1</h2>
<p>
</p>
Resource Types:
<ul><li>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraph">TigerGraph</a>
</li><li>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackup">TigerGraphBackup</a>
</li><li>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSchedule">TigerGraphBackupSchedule</a>
</li><li>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestore">TigerGraphRestore</a>
</li></ul>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraph">TigerGraph</h3>
<p>
<p>TigerGraph is the Schema for the tigergraphs API</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>apiVersion</code></br>
string</td>
<td>
<code>graphdb.tigergraph.com/v1alpha1</code>
</td>
</tr>
<tr>
<td>
<code>kind</code></br>
string
</td>
<td><code>TigerGraph</code></td>
</tr>
<tr>
<td>
<code>metadata</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#objectmeta-v1-meta">
Kubernetes meta/v1.ObjectMeta
</a>
</em>
</td>
<td>
Refer to the Kubernetes API documentation for the fields of the
<code>metadata</code> field.
</td>
</tr>
<tr>
<td>
<code>spec</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">
TigerGraphSpec
</a>
</em>
</td>
<td>
<br/>
<br/>
<table>
<tr>
<td>
<code>replicas</code></br>
<em>
int32
</em>
</td>
<td>
<p>Replicas is the number of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>ha</code></br>
<em>
int32
</em>
</td>
<td>
<p>HA is the replication factor of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>license</code></br>
<em>
string
</em>
</td>
<td>
<p>The license for TigerGraph will be applied when the cluster:
1. Is in the status InitializePost, if the cluster is being initialized for the first time.
2. Is in the status ConfigUpdate, when the license is updated.</p>
</td>
</tr>
<tr>
<td>
<code>image</code></br>
<em>
string
</em>
</td>
<td>
<p>Specify the image of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>imagePullPolicy</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#pullpolicy-v1-core">
Kubernetes core/v1.PullPolicy
</a>
</em>
</td>
<td>
<p>Set the ImagePullPolicy of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>imagePullSecrets</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#localobjectreference-v1-core">
[]Kubernetes core/v1.LocalObjectReference
</a>
</em>
</td>
<td>
<p>Set the ImagePullSecrets of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>serviceAccountName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set the ServiceAccountName used by TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>privateKeyName</code></br>
<em>
string
</em>
</td>
<td>
<p>Set the name of the PrivateKey, which is used for the SSH connection to TigerGraph container</p>
</td>
</tr>
<tr>
<td>
<code>resources</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#resourcerequirements-v1-core">
Kubernetes core/v1.ResourceRequirements
</a>
</em>
</td>
<td>
<p>Set the resources request and limit of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>listener</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Listener">
Listener
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Configure the external service of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>storage</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStorage">
TigerGraphStorage
</a>
</em>
</td>
<td>
<p>Configure the storage of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>affinityConfiguration</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.AffinityConfiguration">
AffinityConfiguration
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set the affinity configuration of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>initContainers</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#container-v1-core">
[]Kubernetes core/v1.Container
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Add initContainers to TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>sidecarContainers</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#container-v1-core">
[]Kubernetes core/v1.Container
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Add sidecar containers to TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>customVolumes</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#volume-v1-core">
[]Kubernetes core/v1.Volume
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Customize volumes for TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>customVolumeMounts</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#volumemount-v1-core">
[]Kubernetes core/v1.VolumeMount
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>CustomVolumeMounts will be added to corev1.Container.VolumeMounts of all TigerGraph Containers.</p>
</td>
</tr>
<tr>
<td>
<code>tigergraphConfig</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set configurations for TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>lifecycle</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Lifecycle">
Lifecycle
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>LifeCycle contains lifecycle hooks of TigerGraph CR.
Refer to <a href="../03-deploy/lifecycle-of-tigergraph.md">Lifecycle of TigerGraph</a></p>
</td>
</tr>
<tr>
<td>
<code>podLabels</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PodLabels will be added to .metadata.labels of all TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>podAnnotations</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PodAnnotations will be added to .metadata.annotations of all TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>pause</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>Pause is used for pausing the cluster, if pause is set as &ldquo;true&rdquo;,
the computing resources of the cluster will be cleaned while data will be retained.
Refer to <a href="../04-manage/pause-and-resume.md">Pause and Resume TigerGraph Clusters</a></p>
</td>
</tr>
<tr>
<td>
<code>securityContext</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#securitycontext-v1-core">
Kubernetes core/v1.SecurityContext
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>SecurityContext is used for setting SecurityContext of TigerGraph container</p>
</td>
</tr>
<tr>
<td>
<code>sidecarListener</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.SidecarListener">
SidecarListener
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>SidecarListener is used for setting the service of sidecar containers.
Refer to <a href="../03-deploy/configure-services-of-sidecar-containers.md">Configure Services of Sidecar Containers</a></p>
</td>
</tr>
<tr>
<td>
<code>regionAware</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.RegionAware">
RegionAware
</a>
</em>
</td>
<td>
<p>RegionAware is used for setting the region-aware replica placement of TigerGraph.
Refer to <a href="../03-deploy/region-awareness-with-pod-topology-spread-constraints.md">Enable Region Awareness with Pod Topology Spread Constraints</a></p>
</td>
</tr>
<tr>
<td>
<code>topologySpreadConstraints</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#topologyspreadconstraint-v1-core">
[]Kubernetes core/v1.TopologySpreadConstraint
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>TopologySpreadConstraints describes how a group of TigerGraph pods ought to spread across topology
domains. Scheduler will schedule pods in a way which abides by the constraints.
All topologySpreadConstraints are ANDed.
Refer to <a href="https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/">Pod Topology Spread Constraints</a></p>
</td>
</tr>
</table>
</td>
</tr>
<tr>
<td>
<code>status</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStatus">
TigerGraphStatus
</a>
</em>
</td>
<td>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphBackup">TigerGraphBackup</h3>
<p>
<p>TigerGraphBackup is the Schema for the tigergraphbackups API</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>apiVersion</code></br>
string</td>
<td>
<code>graphdb.tigergraph.com/v1alpha1</code>
</td>
</tr>
<tr>
<td>
<code>kind</code></br>
string
</td>
<td><code>TigerGraphBackup</code></td>
</tr>
<tr>
<td>
<code>metadata</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#objectmeta-v1-meta">
Kubernetes meta/v1.ObjectMeta
</a>
</em>
</td>
<td>
Refer to the Kubernetes API documentation for the fields of the
<code>metadata</code> field.
</td>
</tr>
<tr>
<td>
<code>spec</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">
TigerGraphBackupSpec
</a>
</em>
</td>
<td>
<br/>
<br/>
<table>
<tr>
<td>
<code>clusterName</code></br>
<em>
string
</em>
</td>
<td>
<p>ClusterName is the name of the target cluster, the cluster must be
in the same namespace. You must provide the name of the cluster
that you want to backup</p>
</td>
</tr>
<tr>
<td>
<code>destination</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Destination">
Destination
</a>
</em>
</td>
<td>
<p>Destination specify the storage of backups. You must specify
destination.</p>
</td>
</tr>
<tr>
<td>
<code>backupConfig</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupConfig">
BackupConfig
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>BackupConfig contains configurations of backup, if you don&rsquo;t provide
BackupConfig, default configurations will be used</p>
</td>
</tr>
<tr>
<td>
<code>backoffRetryPolicy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackoffRetryPolicy">
BackoffRetryPolicy
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>BackoffRetryPolicy controls the backoff and retry behavior of
backup job.
Note: this field only works for TigerGraphBackup, if it is set in
TigerGraphBackupSchedule, it will be ignored.</p>
</td>
</tr>
<tr>
<td>
<code>cleanPolicy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupCleanPolicy">
BackupCleanPolicy
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>CleanPolicy is the cleaning policy for the backup data when the CR
is deleted. You can choose from Retain and Delete.
* Retain: Is the default behavior, the backup data will be retained
when deleting CR
* Delete: Delete the backup data when deleting CR.</p>
</td>
</tr>
</table>
</td>
</tr>
<tr>
<td>
<code>status</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupStatus">
TigerGraphBackupStatus
</a>
</em>
</td>
<td>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSchedule">TigerGraphBackupSchedule</h3>
<p>
<p>TigerGraphBackupSchedule is the Schema for the tigergraphbackupschedules API</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>apiVersion</code></br>
string</td>
<td>
<code>graphdb.tigergraph.com/v1alpha1</code>
</td>
</tr>
<tr>
<td>
<code>kind</code></br>
string
</td>
<td><code>TigerGraphBackupSchedule</code></td>
</tr>
<tr>
<td>
<code>metadata</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#objectmeta-v1-meta">
Kubernetes meta/v1.ObjectMeta
</a>
</em>
</td>
<td>
Refer to the Kubernetes API documentation for the fields of the
<code>metadata</code> field.
</td>
</tr>
<tr>
<td>
<code>spec</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleSpec">
TigerGraphBackupScheduleSpec
</a>
</em>
</td>
<td>
<br/>
<br/>
<table>
<tr>
<td>
<code>schedule</code></br>
<em>
string
</em>
</td>
<td>
<p>The schedule of the backup, using the cron format</p>
</td>
</tr>
<tr>
<td>
<code>pause</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set to true to pause the backup schedule</p>
</td>
</tr>
<tr>
<td>
<code>strategy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupStrategy">
BackupStrategy
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>The strategy of retrying and how to keep the backup files</p>
</td>
</tr>
<tr>
<td>
<code>backupTemplate</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">
TigerGraphBackupSpec
</a>
</em>
</td>
<td>
<p>The template of the TigerGraphBackup CR
TigerGraphBackupSchedule will create TigerGraphBackup CR based on this template
at the scheduled time</p>
</td>
</tr>
</table>
</td>
</tr>
<tr>
<td>
<code>status</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleStatus">
TigerGraphBackupScheduleStatus
</a>
</em>
</td>
<td>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphRestore">TigerGraphRestore</h3>
<p>
<p>TigerGraphRestore is the Schema for the tigergraphrestores API</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>apiVersion</code></br>
string</td>
<td>
<code>graphdb.tigergraph.com/v1alpha1</code>
</td>
</tr>
<tr>
<td>
<code>kind</code></br>
string
</td>
<td><code>TigerGraphRestore</code></td>
</tr>
<tr>
<td>
<code>metadata</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#objectmeta-v1-meta">
Kubernetes meta/v1.ObjectMeta
</a>
</em>
</td>
<td>
Refer to the Kubernetes API documentation for the fields of the
<code>metadata</code> field.
</td>
</tr>
<tr>
<td>
<code>spec</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreSpec">
TigerGraphRestoreSpec
</a>
</em>
</td>
<td>
<br/>
<br/>
<table>
<tr>
<td>
<code>clusterName</code></br>
<em>
string
</em>
</td>
<td>
<p>ClusterName is the name of the target cluster that you want to restore</p>
</td>
</tr>
<tr>
<td>
<code>restoreConfig</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.RestoreConfig">
RestoreConfig
</a>
</em>
</td>
<td>
<p>RestoreConfig is the configuration of the restore job</p>
</td>
</tr>
<tr>
<td>
<code>source</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Destination">
Destination
</a>
</em>
</td>
<td>
<p>Set the source storage of the backup files</p>
</td>
</tr>
<tr>
<td>
<code>clusterTemplate</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">
TigerGraphSpec
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>ClusterTemplate is the configuration of a TigerGraph cluster.
If the target cluster does not exist, TigerGraph Operator will
create a new cluster with this configuration first.
Use this field to clone a cluster
See <a href="../04-manage/backup-and-restore/backup-restore-by-cr.md#clone-cluster-version-392">Clone a cluster</a></p>
</td>
</tr>
<tr>
<td>
<code>backoffRetryPolicy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackoffRetryPolicy">
BackoffRetryPolicy
</a>
</em>
</td>
<td>
<p>BackoffRetryPolicy is the policy of retrying the restore job</p>
</td>
</tr>
</table>
</td>
</tr>
<tr>
<td>
<code>status</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreStatus">
TigerGraphRestoreStatus
</a>
</em>
</td>
<td>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.AffinityConfiguration">AffinityConfiguration</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>nodeSelector</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The configuration of assigning pods to special nodes using NodeSelector</p>
</td>
</tr>
<tr>
<td>
<code>affinity</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#affinity-v1-core">
Kubernetes core/v1.Affinity
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>The tolerations configuration of TigerGraph Pod</p>
</td>
</tr>
<tr>
<td>
<code>tolerations</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#toleration-v1-core">
[]Kubernetes core/v1.Toleration
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>The tolerations configuration of TigerGraph pod</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.BackoffRetryPolicy">BackoffRetryPolicy</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">TigerGraphBackupSpec</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreSpec">TigerGraphRestoreSpec</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>minRetryDuration</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The minimum retry interval after first failure.
The format should be like &ldquo;5s&rdquo;,&ldquo;10m&rdquo;,&ldquo;1h&rdquo;,&ldquo;1h20m5s&rdquo;
Default value is 5s</p>
</td>
</tr>
<tr>
<td>
<code>maxRetryDuration</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The maximum retry interval.
The format should be like &ldquo;5s&rdquo;,&ldquo;10m&rdquo;,&ldquo;1h&rdquo;,&ldquo;1h20m5s&rdquo;
Default value is 10m.</p>
</td>
</tr>
<tr>
<td>
<code>maxRetryTimes</code></br>
<em>
int32
</em>
</td>
<td>
<em>(Optional)</em>
<p>The maximum number of retries, empty means retry forever</p>
</td>
</tr>
<tr>
<td>
<code>forceDeleteAfterMaxRetries</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>If this field is true, the backup CR will be deleted even if
the backup clean job failed and cleanPolicy is Delete</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.BackupConfig">BackupConfig</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">TigerGraphBackupSpec</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>tag</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Tag specifies tag of the backup. The actual name of the backup
will be {TAG}-{TIMESTAMP}. If this field is not specified, the name
of this CR will be used as tag.</p>
</td>
</tr>
<tr>
<td>
<code>stagingPath</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Specify the path where the backup files are stored temporarily</p>
</td>
</tr>
<tr>
<td>
<code>incremental</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>[Preview] Enable incremental backup</p>
</td>
</tr>
<tr>
<td>
<code>timeout</code></br>
<em>
int
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set the timeout seconds for backup job</p>
</td>
</tr>
<tr>
<td>
<code>compressProcessNumber</code></br>
<em>
int
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set the CompressProcessNumber for backup job</p>
</td>
</tr>
<tr>
<td>
<code>compressLevel</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>If CompressLevel is empty, means we don&rsquo;t set it explicitly
Available values are BestSpeed, DefaultCompression, BestCompression</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.BackupInfo">BackupInfo</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupStatus">TigerGraphBackupStatus</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>tag</code></br>
<em>
string
</em>
</td>
<td>
<p>The actual tag of the backup package
Can be used in TigerGraphRestore CR</p>
</td>
</tr>
<tr>
<td>
<code>sizeBytes</code></br>
<em>
string
</em>
</td>
<td>
<p>The size of the backup package</p>
</td>
</tr>
<tr>
<td>
<code>time</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>The time when the backup package is created</p>
</td>
</tr>
<tr>
<td>
<code>type</code></br>
<em>
string
</em>
</td>
<td>
<p>Is the backup package incremental or full backup</p>
</td>
</tr>
<tr>
<td>
<code>version</code></br>
<em>
string
</em>
</td>
<td>
<p>The version of the cluster where the backup package is created</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.BackupRestoreObjectType">BackupRestoreObjectType</h3>
<p>
</p>
<h3 id="graphdb.tigergraph.com/v1alpha1.BackupStrategy">BackupStrategy</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleSpec">TigerGraphBackupScheduleSpec</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>maxBackupFiles</code></br>
<em>
int
</em>
</td>
<td>
<em>(Optional)</em>
<p>The maximum number of backup files to retain
0 means backups won&rsquo;t be deleted according to the number</p>
</td>
</tr>
<tr>
<td>
<code>maxReservedDays</code></br>
<em>
int
</em>
</td>
<td>
<em>(Optional)</em>
<p>The maximum number of days to retain the backup files
0 means backups won&rsquo;t be deleted according to the date</p>
</td>
</tr>
<tr>
<td>
<code>maxRetry</code></br>
<em>
int32
</em>
</td>
<td>
<em>(Optional)</em>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.Destination">Destination</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">TigerGraphBackupSpec</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreSpec">TigerGraphRestoreSpec</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>storage</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupStorageType">
BackupStorageType
</a>
</em>
</td>
<td>
<p>Set the storage type of the backup
Available values are local, s3Bucket</p>
</td>
</tr>
<tr>
<td>
<code>local</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.LocalStorage">
LocalStorage
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Configure this field if you choose local storage</p>
</td>
</tr>
<tr>
<td>
<code>s3Bucket</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.S3Bucket">
S3Bucket
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Configure this field if you choose s3Bucket storage</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.IngressRule">IngressRule</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.ListenerPort">ListenerPort</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>host</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Host can be &ldquo;precise&rdquo; which is a domain name without the terminating dot of
a network host (e.g. &ldquo;foo.bar.com&rdquo;) or &ldquo;wildcard&rdquo;, which is a domain name
prefixed with a single wildcard label (e.g. &ldquo;<em>.foo.com&rdquo;).
The wildcard character &lsquo;</em>&rsquo; must appear by itself as the first DNS label and
matches only a single label. You cannot have a wildcard label by itself (e.g. Host == &ldquo;*&rdquo;).
Requests will be matched against the Host field in the following way:
1. If Host is precise, the request matches this rule if the http host header is equal to Host.
2. If Host is a wildcard, then the request matches this rule if the http host header
is to equal to the suffix (removing the first label) of the wildcard rule.</p>
</td>
</tr>
<tr>
<td>
<code>secretName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>SecretName is the name of the secret used to terminate TLS traffic on
port 443. Field is left optional to allow TLS routing based on SNI
hostname alone. If the SNI host in a listener conflicts with the &ldquo;Host&rdquo;
header field used by an IngressRule, the SNI host is used for termination
and value of the Host header is used for routing.</p>
</td>
</tr>
<tr>
<td>
<code>path</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Path is matched against the path of an incoming request. Currently it can
contain characters disallowed from the conventional &ldquo;path&rdquo; part of a URL
as defined by RFC 3986. Paths must begin with a &lsquo;/&rsquo; and must be present
when using PathType with value &ldquo;Exact&rdquo; or &ldquo;Prefix&rdquo;.</p>
</td>
</tr>
<tr>
<td>
<code>pathType</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#pathtype-v1-networking">
Kubernetes networking/v1.PathType
</a>
</em>
</td>
<td>
<p>PathType represents the type of path referred to by a HTTPIngressPath.</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.JobCounter">JobCounter</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleStatus">TigerGraphBackupScheduleStatus</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>successfulJobs</code></br>
<em>
int32
</em>
</td>
<td>
<p>The number of successful jobs</p>
</td>
</tr>
<tr>
<td>
<code>failedJobs</code></br>
<em>
int32
</em>
</td>
<td>
<p>The number of failed jobs</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.Lifecycle">Lifecycle</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</a>)
</p>
<p>
<p>LifeCycle contains lifecycle hooks of TigerGraph CR</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>postInitAction</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PostInitAction will be executed in init-job after initialization progress.
If any error occurs in PostInitAction, the init-job will fail.
The error should be handled manually and if the init-job failed, you
have to recover it by yourself.</p>
</td>
</tr>
<tr>
<td>
<code>prePauseAction</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PrePauseAction will be executed in pre-pause job before cleaning computing resources
when .spec.pause is set as &ldquo;true&rdquo;.
The error should be handled manually and if the pre-pause failed, you
have to recover it by yourself.</p>
</td>
</tr>
<tr>
<td>
<code>preDeleteAction</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PreDeleteAction will be executed in pre-delete job before deleting the cluster.
The error should be handled manually and if the pre-delete failed, you
have to recover it by yourself.</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.Listener">Listener</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStatus">TigerGraphStatus</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>type</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#servicetype-v1-core">
Kubernetes core/v1.ServiceType
</a>
</em>
</td>
<td>
<p>The service type of external service for TigerGraph cluster,
which can be set to LoadBalancer, NodePort, and Ingress</p>
</td>
</tr>
<tr>
<td>
<code>nginxNodePort</code></br>
<em>
int32
</em>
</td>
<td>
<em>(Optional)</em>
<p>The nginx service port which is required when setting listener.type to NodePort</p>
</td>
</tr>
<tr>
<td>
<code>ingressClassName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The ingress class name of nginx service which can be set optionally when setting listener.type to Ingress</p>
</td>
</tr>
<tr>
<td>
<code>nginxHost</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The domain name of nginx service which is required when setting listener.type to Ingress</p>
</td>
</tr>
<tr>
<td>
<code>secretName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The secretName is the name of the secret used to terminate TLS traffic on port 443
when setting listener.type to Ingress</p>
</td>
</tr>
<tr>
<td>
<code>labels</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The customized labels will be added to external service</p>
</td>
</tr>
<tr>
<td>
<code>annotations</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The customized annotations will be added to external service</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.ListenerPort">ListenerPort</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.SidecarListener">SidecarListener</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>name</code></br>
<em>
string
</em>
</td>
<td>
<p>The port name that should be unique</p>
</td>
</tr>
<tr>
<td>
<code>port</code></br>
<em>
int32
</em>
</td>
<td>
<p>The port that exposes in sidecar container</p>
</td>
</tr>
<tr>
<td>
<code>nodePort</code></br>
<em>
int32
</em>
</td>
<td>
<em>(Optional)</em>
<p>only used when ServiceType is NodePort</p>
</td>
</tr>
<tr>
<td>
<code>ingressRule</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.IngressRule">
IngressRule
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>only used when ServiceType is Ingress</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.LocalStorage">LocalStorage</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.Destination">Destination</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>path</code></br>
<em>
string
</em>
</td>
<td>
<p>Path is the local path where the backup files are stored</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.RegionAware">RegionAware</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStatus">TigerGraphStatus</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>enable</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>Enable region-aware replica placement of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>topologyKey</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Specify the custom topologyKey of Nodes that have a label with this key
and identical values are considered to be in the same topology
default value is topology.kubernetes.io/zone</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.RestoreConfig">RestoreConfig</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreSpec">TigerGraphRestoreSpec</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>meta</code></br>
<em>
string
</em>
</td>
<td>
<p>Meta should be read from a file get by gadmin backup list &ndash;meta
Meta contains the information of the backup package that you want to restore
One of Meta and tag should be specified</p>
</td>
</tr>
<tr>
<td>
<code>tag</code></br>
<em>
string
</em>
</td>
<td>
<p>Tag is the actual tag of the backup package that you want to restore
One of Meta and tag should be specified</p>
</td>
</tr>
<tr>
<td>
<code>stagingPath</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Specify the path where the backup files are stored temporarily</p>
</td>
</tr>
<tr>
<td>
<code>decompressProcessNumber</code></br>
<em>
int
</em>
</td>
<td>
<em>(Optional)</em>
<p>DecompressProcessNumber must be greater or equal to 0, if it equals to 0
means do not set it explicitly</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.S3Bucket">S3Bucket</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.Destination">Destination</a>)
</p>
<p>
<p>S3Bucket is the configuration of S3 bucket used by backup/restore</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>bucketName</code></br>
<em>
string
</em>
</td>
<td>
<p>The name of the S3 bucket used to store backups</p>
</td>
</tr>
<tr>
<td>
<code>secretKeyName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>a Secret should be created in cluster
the secret should contain AccessKeyID and SecretAccessKey</p>
</td>
</tr>
<tr>
<td>
<code>endpoint</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>An S3 optional endpoint URL(hostname only or fully qualified URI, such as <a href="https://s3.amazonaws.com/">https://s3.amazonaws.com/</a>)
that overrides the default generated endpoint for a client.</p>
</td>
</tr>
<tr>
<td>
<code>roleARN</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Use RoleARN to access S3 bucket
Need extra configuration of TigerGraph
if you want to use this way to access S3 bucket
See <a href="../04-manage/backup-and-restore/create-tg-with-access-to-s3.md">Create TigerGraph cluster with access to S3</a></p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.SidecarListener">SidecarListener</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStatus">TigerGraphStatus</a>)
</p>
<p>
<p>SidecarListener contains the sidecar container&rsquo;s service definitions of TigerGraph CR</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>type</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#servicetype-v1-core">
Kubernetes core/v1.ServiceType
</a>
</em>
</td>
<td>
<p>The external service type for sidecar service, which can be set to LoadBalancer, NodePort, and Ingress</p>
</td>
</tr>
<tr>
<td>
<code>labels</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The labels are for sidecar services</p>
</td>
</tr>
<tr>
<td>
<code>annotations</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The annotations are for sidecar services</p>
</td>
</tr>
<tr>
<td>
<code>externalTrafficPolicy</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#serviceexternaltrafficpolicytype-v1-core">
Kubernetes core/v1.ServiceExternalTrafficPolicyType
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Service external trafficPolicy type for LoadBalancer and NodePort</p>
</td>
</tr>
<tr>
<td>
<code>ingressClassName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Ingress class name, only used when ServiceType is Ingress</p>
</td>
</tr>
<tr>
<td>
<code>listenerPorts</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.ListenerPort">
[]ListenerPort
</a>
</em>
</td>
<td>
<p>The port and ingress rules that would be exposed for sidecar services</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.StorageStatus">StorageStatus</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStatus">TigerGraphStatus</a>)
</p>
<p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>tgDataSize</code></br>
<em>
k8s.io/apimachinery/pkg/api/resource.Quantity
</em>
</td>
<td>
<p>The size of main storage of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>additionalStorageSize</code></br>
<em>
map[string]k8s.io/apimachinery/pkg/api/resource.Quantity
</em>
</td>
<td>
<p>The sizes of additional storages of TigerGraph</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.StorageVolume">StorageVolume</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStorage">TigerGraphStorage</a>)
</p>
<p>
<p>StorageVolume configures additional PVC template for StatefulSets and volumeMount for pods that mount this PVC.
Note:
If <code>MountPath</code> is not set, volumeMount will not be generated.
(You may not want to set this field when you inject volumeMount
in somewhere else such as Mutating Admission Webhook)
If <code>StorageClassName</code> is not set, default to the <code>spec.storage.volumeClaimTemplate.storageClassName</code></p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>name</code></br>
<em>
string
</em>
</td>
<td>
<p>Additional storage name</p>
</td>
</tr>
<tr>
<td>
<code>storageClassName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The StorageClassName of an additional storage
if it is empty, the Storage Class Name of main storage will be used.</p>
</td>
</tr>
<tr>
<td>
<code>storageSize</code></br>
<em>
string
</em>
</td>
<td>
<p>The storage size of an additional storage</p>
</td>
</tr>
<tr>
<td>
<code>mountPath</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The mount path of TigerGraph container for an additional storage</p>
</td>
</tr>
<tr>
<td>
<code>accessMode</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The access mode of an additional storage,
which can be set to ReadWriteOnce, ReadOnlyMany, ReadWriteMany, or ReadWriteOncePod</p>
</td>
</tr>
<tr>
<td>
<code>volumeMode</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>The volume mode of an additional storage, which can be set to Filesystem or Block</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleSpec">TigerGraphBackupScheduleSpec</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSchedule">TigerGraphBackupSchedule</a>)
</p>
<p>
<p>TigerGraphBackupScheduleSpec defines the desired state of TigerGraphBackupSchedule</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>schedule</code></br>
<em>
string
</em>
</td>
<td>
<p>The schedule of the backup, using the cron format</p>
</td>
</tr>
<tr>
<td>
<code>pause</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set to true to pause the backup schedule</p>
</td>
</tr>
<tr>
<td>
<code>strategy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupStrategy">
BackupStrategy
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>The strategy of retrying and how to keep the backup files</p>
</td>
</tr>
<tr>
<td>
<code>backupTemplate</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">
TigerGraphBackupSpec
</a>
</em>
</td>
<td>
<p>The template of the TigerGraphBackup CR
TigerGraphBackupSchedule will create TigerGraphBackup CR based on this template
at the scheduled time</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleStatus">TigerGraphBackupScheduleStatus</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSchedule">TigerGraphBackupSchedule</a>)
</p>
<p>
<p>TigerGraphBackupScheduleStatus defines the observed state of TigerGraphBackupSchedule</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>lastScheduleTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>The last time the backup schedule was scheduled</p>
</td>
</tr>
<tr>
<td>
<code>lastSuccessfulTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>The last time the scheduled backup was successful</p>
</td>
</tr>
<tr>
<td>
<code>lastBackup</code></br>
<em>
string
</em>
</td>
<td>
<p>The name of the last scheduled backup CR</p>
</td>
</tr>
<tr>
<td>
<code>nextScheduleTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>The next time the backup schedule will be scheduled</p>
</td>
</tr>
<tr>
<td>
<code>conditions</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#condition-v1-meta">
[]Kubernetes meta/v1.Condition
</a>
</em>
</td>
<td>
<p>Conditions represent the latest available observations of TigerGraphBackupSchedule&rsquo;s state
Refer to <a href="../04-manage/backup-and-restore/status-of-backup-restore.md">Status of Backup and Restore</a></p>
</td>
</tr>
<tr>
<td>
<code>jobCounter</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.JobCounter">
JobCounter
</a>
</em>
</td>
<td>
<p>JobCounter represents the number of successful and failed jobs</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphBackupSpec">TigerGraphBackupSpec</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackup">TigerGraphBackup</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackupScheduleSpec">TigerGraphBackupScheduleSpec</a>)
</p>
<p>
<p>TigerGraphBackupSpec defines the desired state of TigerGraphBackup</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>clusterName</code></br>
<em>
string
</em>
</td>
<td>
<p>ClusterName is the name of the target cluster, the cluster must be
in the same namespace. You must provide the name of the cluster
that you want to backup</p>
</td>
</tr>
<tr>
<td>
<code>destination</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Destination">
Destination
</a>
</em>
</td>
<td>
<p>Destination specify the storage of backups. You must specify
destination.</p>
</td>
</tr>
<tr>
<td>
<code>backupConfig</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupConfig">
BackupConfig
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>BackupConfig contains configurations of backup, if you don&rsquo;t provide
BackupConfig, default configurations will be used</p>
</td>
</tr>
<tr>
<td>
<code>backoffRetryPolicy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackoffRetryPolicy">
BackoffRetryPolicy
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>BackoffRetryPolicy controls the backoff and retry behavior of
backup job.
Note: this field only works for TigerGraphBackup, if it is set in
TigerGraphBackupSchedule, it will be ignored.</p>
</td>
</tr>
<tr>
<td>
<code>cleanPolicy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupCleanPolicy">
BackupCleanPolicy
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>CleanPolicy is the cleaning policy for the backup data when the CR
is deleted. You can choose from Retain and Delete.
* Retain: Is the default behavior, the backup data will be retained
when deleting CR
* Delete: Delete the backup data when deleting CR.</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphBackupStatus">TigerGraphBackupStatus</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphBackup">TigerGraphBackup</a>)
</p>
<p>
<p>TigerGraphBackupStatus defines the observed state of TigerGraphBackup</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>startTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>The start time of the backup job</p>
</td>
</tr>
<tr>
<td>
<code>completionTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>The completion time of the backup job</p>
</td>
</tr>
<tr>
<td>
<code>conditions</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#condition-v1-meta">
[]Kubernetes meta/v1.Condition
</a>
</em>
</td>
<td>
<p>Conditions represent the latest available observations of TigerGraphBackup&rsquo;s state
Refer to <a href="../04-manage/backup-and-restore/status-of-backup-restore.md">Status of Backup and Restore</a></p>
</td>
</tr>
<tr>
<td>
<code>targetReady</code></br>
<em>
bool
</em>
</td>
<td>
<p>TargetReady is false when target cluster does not exist or not ready for backup</p>
</td>
</tr>
<tr>
<td>
<code>backOffTimes</code></br>
<em>
int
</em>
</td>
<td>
<p>Equal to the number of backup retries after failure</p>
</td>
</tr>
<tr>
<td>
<code>backupInfo</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackupInfo">
BackupInfo
</a>
</em>
</td>
<td>
<p>BackupInfo contains the information of the backup package created by this CR</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreSpec">TigerGraphRestoreSpec</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestore">TigerGraphRestore</a>)
</p>
<p>
<p>TigerGraphRestoreSpec defines the desired state of TigerGraphRestore</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>clusterName</code></br>
<em>
string
</em>
</td>
<td>
<p>ClusterName is the name of the target cluster that you want to restore</p>
</td>
</tr>
<tr>
<td>
<code>restoreConfig</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.RestoreConfig">
RestoreConfig
</a>
</em>
</td>
<td>
<p>RestoreConfig is the configuration of the restore job</p>
</td>
</tr>
<tr>
<td>
<code>source</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Destination">
Destination
</a>
</em>
</td>
<td>
<p>Set the source storage of the backup files</p>
</td>
</tr>
<tr>
<td>
<code>clusterTemplate</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">
TigerGraphSpec
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>ClusterTemplate is the configuration of a TigerGraph cluster.
If the target cluster does not exist, TigerGraph Operator will
create a new cluster with this configuration first.
Use this field to clone a cluster
See <a href="../04-manage/backup-and-restore/backup-restore-by-cr.md#clone-cluster-version-392">Clone a cluster</a></p>
</td>
</tr>
<tr>
<td>
<code>backoffRetryPolicy</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.BackoffRetryPolicy">
BackoffRetryPolicy
</a>
</em>
</td>
<td>
<p>BackoffRetryPolicy is the policy of retrying the restore job</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreStatus">TigerGraphRestoreStatus</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestore">TigerGraphRestore</a>)
</p>
<p>
<p>TigerGraphRestoreStatus defines the observed state of TigerGraphRestore</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>startTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>StartTime is the time when the restore job starts</p>
</td>
</tr>
<tr>
<td>
<code>completionTime</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#time-v1-meta">
Kubernetes meta/v1.Time
</a>
</em>
</td>
<td>
<p>CompletionTime is the time when the restore job completes</p>
</td>
</tr>
<tr>
<td>
<code>conditions</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#condition-v1-meta">
[]Kubernetes meta/v1.Condition
</a>
</em>
</td>
<td>
<p>Conditions represent the latest available observations of TigerGraphRestore&rsquo;s state
Refer to <a href="../04-manage/backup-and-restore/status-of-backup-restore.md">Status of Backup and Restore</a></p>
</td>
</tr>
<tr>
<td>
<code>targetReady</code></br>
<em>
bool
</em>
</td>
<td>
<p>TargetReady is false when target cluster does not exist or not ready for restore</p>
</td>
</tr>
<tr>
<td>
<code>backOffTimes</code></br>
<em>
int
</em>
</td>
<td>
<p>Equal to the number of backup retries after failure</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraph">TigerGraph</a>, 
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphRestoreSpec">TigerGraphRestoreSpec</a>)
</p>
<p>
<p>TigerGraphSpec defines the desired state of TigerGraph</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>replicas</code></br>
<em>
int32
</em>
</td>
<td>
<p>Replicas is the number of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>ha</code></br>
<em>
int32
</em>
</td>
<td>
<p>HA is the replication factor of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>license</code></br>
<em>
string
</em>
</td>
<td>
<p>The license for TigerGraph will be applied when the cluster:
1. Is in the status InitializePost, if the cluster is being initialized for the first time.
2. Is in the status ConfigUpdate, when the license is updated.</p>
</td>
</tr>
<tr>
<td>
<code>image</code></br>
<em>
string
</em>
</td>
<td>
<p>Specify the image of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>imagePullPolicy</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#pullpolicy-v1-core">
Kubernetes core/v1.PullPolicy
</a>
</em>
</td>
<td>
<p>Set the ImagePullPolicy of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>imagePullSecrets</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#localobjectreference-v1-core">
[]Kubernetes core/v1.LocalObjectReference
</a>
</em>
</td>
<td>
<p>Set the ImagePullSecrets of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>serviceAccountName</code></br>
<em>
string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set the ServiceAccountName used by TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>privateKeyName</code></br>
<em>
string
</em>
</td>
<td>
<p>Set the name of the PrivateKey, which is used for the SSH connection to TigerGraph container</p>
</td>
</tr>
<tr>
<td>
<code>resources</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#resourcerequirements-v1-core">
Kubernetes core/v1.ResourceRequirements
</a>
</em>
</td>
<td>
<p>Set the resources request and limit of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>listener</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Listener">
Listener
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Configure the external service of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>storage</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphStorage">
TigerGraphStorage
</a>
</em>
</td>
<td>
<p>Configure the storage of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>affinityConfiguration</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.AffinityConfiguration">
AffinityConfiguration
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set the affinity configuration of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>initContainers</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#container-v1-core">
[]Kubernetes core/v1.Container
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Add initContainers to TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>sidecarContainers</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#container-v1-core">
[]Kubernetes core/v1.Container
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Add sidecar containers to TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>customVolumes</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#volume-v1-core">
[]Kubernetes core/v1.Volume
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Customize volumes for TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>customVolumeMounts</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#volumemount-v1-core">
[]Kubernetes core/v1.VolumeMount
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>CustomVolumeMounts will be added to corev1.Container.VolumeMounts of all TigerGraph Containers.</p>
</td>
</tr>
<tr>
<td>
<code>tigergraphConfig</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>Set configurations for TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>lifecycle</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Lifecycle">
Lifecycle
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>LifeCycle contains lifecycle hooks of TigerGraph CR.
Refer to <a href="../03-deploy/lifecycle-of-tigergraph.md">Lifecycle of TigerGraph</a></p>
</td>
</tr>
<tr>
<td>
<code>podLabels</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PodLabels will be added to .metadata.labels of all TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>podAnnotations</code></br>
<em>
map[string]string
</em>
</td>
<td>
<em>(Optional)</em>
<p>PodAnnotations will be added to .metadata.annotations of all TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>pause</code></br>
<em>
bool
</em>
</td>
<td>
<em>(Optional)</em>
<p>Pause is used for pausing the cluster, if pause is set as &ldquo;true&rdquo;,
the computing resources of the cluster will be cleaned while data will be retained.
Refer to <a href="../04-manage/pause-and-resume.md">Pause and Resume TigerGraph Clusters</a></p>
</td>
</tr>
<tr>
<td>
<code>securityContext</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#securitycontext-v1-core">
Kubernetes core/v1.SecurityContext
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>SecurityContext is used for setting SecurityContext of TigerGraph container</p>
</td>
</tr>
<tr>
<td>
<code>sidecarListener</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.SidecarListener">
SidecarListener
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>SidecarListener is used for setting the service of sidecar containers.
Refer to <a href="../03-deploy/configure-services-of-sidecar-containers.md">Configure Services of Sidecar Containers</a></p>
</td>
</tr>
<tr>
<td>
<code>regionAware</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.RegionAware">
RegionAware
</a>
</em>
</td>
<td>
<p>RegionAware is used for setting the region-aware replica placement of TigerGraph.
Refer to <a href="../03-deploy/region-awareness-with-pod-topology-spread-constraints.md">Enable Region Awareness with Pod Topology Spread Constraints</a></p>
</td>
</tr>
<tr>
<td>
<code>topologySpreadConstraints</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#topologyspreadconstraint-v1-core">
[]Kubernetes core/v1.TopologySpreadConstraint
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>TopologySpreadConstraints describes how a group of TigerGraph pods ought to spread across topology
domains. Scheduler will schedule pods in a way which abides by the constraints.
All topologySpreadConstraints are ANDed.
Refer to <a href="https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/">Pod Topology Spread Constraints</a></p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphStatus">TigerGraphStatus</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraph">TigerGraph</a>)
</p>
<p>
<p>TigerGraphStatus defines the observed state of TigerGraph</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>replicas</code></br>
<em>
int32
</em>
</td>
<td>
<p>Replicas is the current replicas of StatefulSet</p>
</td>
</tr>
<tr>
<td>
<code>clusterSize</code></br>
<em>
int32
</em>
</td>
<td>
<p>ClusterSize is the actual cluster size of TigerGraph cluster</p>
</td>
</tr>
<tr>
<td>
<code>ha</code></br>
<em>
int32
</em>
</td>
<td>
<p>HA is the actual replication factor of TigerGraph cluster</p>
</td>
</tr>
<tr>
<td>
<code>image</code></br>
<em>
string
</em>
</td>
<td>
<p>Image is current image used by TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>conditions</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#condition-v1-meta">
[]Kubernetes meta/v1.Condition
</a>
</em>
</td>
<td>
<p>Condition contains details for one aspect of the current state of TigerGraph Cluster Resource</p>
</td>
</tr>
<tr>
<td>
<code>listener</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.Listener">
Listener
</a>
</em>
</td>
<td>
<p>Current Listener used by TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>hashBucketInBit</code></br>
<em>
int32
</em>
</td>
<td>
<p>Current hash bucket used by TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>tigergraphConfig</code></br>
<em>
map[string]string
</em>
</td>
<td>
<p>Current configurations of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>licenseHash</code></br>
<em>
string
</em>
</td>
<td>
<p>MD5 of current license used by TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>podLabels</code></br>
<em>
map[string]string
</em>
</td>
<td>
<p>Current customized labels of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>podAnnotations</code></br>
<em>
map[string]string
</em>
</td>
<td>
<p>Current customized annotations of TigerGraph Pods</p>
</td>
</tr>
<tr>
<td>
<code>sidecarListener</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.SidecarListener">
SidecarListener
</a>
</em>
</td>
<td>
<p>Current listeners for sidecar containers</p>
</td>
</tr>
<tr>
<td>
<code>storage</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.StorageStatus">
StorageStatus
</a>
</em>
</td>
<td>
<p>Status of storages of TigerGraph</p>
</td>
</tr>
<tr>
<td>
<code>regionAware</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.RegionAware">
RegionAware
</a>
</em>
</td>
<td>
<p>Current RegionAware for TigerGraph cluster</p>
</td>
</tr>
<tr>
<td>
<code>topologySpreadConstraints</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#topologyspreadconstraint-v1-core">
[]Kubernetes core/v1.TopologySpreadConstraint
</a>
</em>
</td>
<td>
<p>Current TopologySpreadConstraints of TigerGraph Pods</p>
</td>
</tr>
</tbody>
</table>
<h3 id="graphdb.tigergraph.com/v1alpha1.TigerGraphStorage">TigerGraphStorage</h3>
<p>
(<em>Appears on:</em>
<a href="#graphdb.tigergraph.com/v1alpha1.TigerGraphSpec">TigerGraphSpec</a>)
</p>
<p>
<p>TigerGraphStorage is the inteface to add pvc and pv support in tigergraph</p>
</p>
<table>
<thead>
<tr>
<th>Field</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td>
<code>type</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.StorageType">
StorageType
</a>
</em>
</td>
<td>
<p>The volume type used for storing main data in TigerGraph
it can be set to persistent-claim or ephemeral</p>
</td>
</tr>
<tr>
<td>
<code>volumeClaimTemplate</code></br>
<em>
<a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.26/#persistentvolumeclaimspec-v1-core">
Kubernetes core/v1.PersistentVolumeClaimSpec
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>PersistentVolumeClaimSpec describes the common attributes of storage devices
and allows a Source for provider-specific attributes
it&rsquo;s required when the StorageType is persistent-claim</p>
</td>
</tr>
<tr>
<td>
<code>additionalStorages</code></br>
<em>
<a href="#graphdb.tigergraph.com/v1alpha1.StorageVolume">
[]StorageVolume
</a>
</em>
</td>
<td>
<em>(Optional)</em>
<p>Additional storages for TigerGraph Pods</p>
</td>
</tr>
</tbody>
</table>
<hr/>
<p><em>
Generated with <code>gen-crd-api-reference-docs</code>
</em></p>
