![TG_LOGO](https://github.com/trumanWangtg/ecosys/blob/master/docs/images/github/tgcloudbanner.png)

# TigerGraph 108 TB Soical Network Graph 
To get started with LDBC SNB benchmarks, check out the introductory presentation: [The LDBC Social Network Benchmark](https://docs.google.com/presentation/d/1p-nuHarSOKCldZ9iEz__6_V3sJ5kbGWlzZHusudW_Cc/) ([PDF](https://ldbcouncil.org/docs/presentations/ldbc-snb-2021-12.pdf)).

The official LDBC SNB benchmarks Github repository can be found here: [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi).

The following numerical novelties are highlighted in this summary:

* The full source dataset comprises 108TB, encompassing `1.619 trillion relationships and 218 billion vertices`.
* The benchmarking process, which includes an initial data loading, one power batch run, and one throughput batch run (with each batch running all BI queries five times), took a total of `35.4 hours`.
* The hardware cost for the benchmarking was `$843/hr, utilizing 72 AWS r6a.48xlarge machines and 432T GP3 SSD volumes`.
* The 108TB data is an inflation of the LDBC SNB official SF-30000 dataset. The LDBC SNB schema and BI workload queries were altered to ensure that most queries `activate the entire 108TB data set`.

 This experiment was carried out with TigerGraph Version 3.7.0 and not using any containerization/virtualization technology.  Full Disclosure Report of the [108T Social Network Benchmark](https://docs.google.com/document/d/1h4PnZGpg8-HYBvIHjjgdeonchZo7hSHfEK8IsTrTZN0/edit). (document to be replaced) 


## Methodology
The LDBC_SNB SF-30k data generator produces 44TB of raw data, consisting of 36.5TB in the initial snapshot and 7TB in the form of inserts and deletes. The initial snapshot includes 36TB of dynamic data, which can be altered through daily batch updates, and less than 1TB of static data, which remains unchanged.

To increase the volume of data, the 36TB of dynamic vertex and edge types were replicated three times, including Comment, Person, Post, Forum, and their associated edges. This resulted in `a total of more than 108TB of raw data loaded into TigerGraph`. For instance, the new graph schema includes Comment1, Comment2, and Comment3. All dynamic vertex types are connected to the original static vertex types, forming a single, cohesive graph. 

![Schema_change_diagram](https://github.com/trumanWangtg/108TB_trillion_graph_SNB/blob/main/common/schema_diagram.png)

There are no subgraphs or disconnected elements, allowing for BI queries to traverse the entire 108TB of data. Accordingly, it is necessary to substitute the original dynamic vertex types with the tripled dynamic vertex types in BI queries to ensure proper functioning.
## Implementations

The repository contains the following implementations:

* [`Trillion Graph Schema Setup`](tigergraph/ddl/schema.gsql): an implementation about the 108T graph schema setup demonstrating how to triple the LDBC SNB SF30k dataset
* [`Loading job`](tigergraph/ddl/load_dynamic.gsql): an implementation about loading jobs demonstrating how the graph loads 108T raw data. 
* [`Queries`](tigergraph/queries):  queries expressed in the [GSQL language](https://www.tigergraph.com/gsql/) with modification to ensure that most queries can activate the whole 108TB dataset
* [`Reproducing 108T SNB BI Experiment`](tigergraph/benchmark_on_cluster/README.md): step-by-step instructions of how to reproduce the 108T SNB BI experiment

 ## Query Modifications 
All the `original dynamic vertex types are replaced with tripled vertex types`. For example, in bi-11:
```
#Original FROM clause
FROM persons:p1 -(KNOWS:e)- (Person):p2 

#Modified FROM clause
FROM persons:p1 -(KNOWS:e)- (Person1|Person2|Person3):p2 
```
Dynamic vertex involved in edge definition are duplicated and indexed. Therefore, the `edge name can remain the same` in queries. For example, here are how we define modifed edge CONTAINE_OF and REPLYP_OF.
```
CREATE DIRECTED EDGE CONTAINER_OF (FROM Forum1, TO Post1 | FROM Forum2, TO Post2 | FROM Forum3, TO Post3) WITH REVERSE_EDGE="CONTAINER_OF_REVERSE"

CREATE DIRECTED EDGE REPLY_OF (FROM Comment1, TO Comment1 | FROM Comment2, TO Comment2 | FROM Comment3, TO Comment3 | FROM Comment1, TO Post1 | FROM Comment2, TO Post2 | FROM Comment3, TO Post3) WITH REVERSE_EDGE="REPLY_OF_REVERSE"
```
Most bi queries are modified based on the above implementation and can touch the whole 108TB dataset except 4 querie. Queries bi-14, bi-15, bi-17, and bi-18 only activate 36T data due to a limitation in TigerGraph v3.7.0 where the VERTEX<> clause does not support multiple vertex type specifications. These queries only feature one singular dynamic seed vertex in parameter field and do not traverse to the joint static vertex, thus leaving other duplicated dynamic vertices untraversed.




