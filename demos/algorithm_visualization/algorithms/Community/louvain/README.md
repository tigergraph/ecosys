
# Louvain

#### [Louvain Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/louvain/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Louvain Documentation](https://docs.tigergraph.com/graph-algorithm-library/community/louvain-method-with-parallelism-and-refinement)

## Available Louvain Algorithms 

* [`tg_louvain`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/louvain/tg_louvain.gsql)

## Installation 

### Replace `<Louvain Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Louvain Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Louvain Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Louvain Algorithm>
```