
# Degree

#### [Degree Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/degree/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Degree Documentation](https://docs.tigergraph.com/graph-algorithm-library/)

## Available Degree Algorithms 

* [`tg_degree_cent`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/degree/tg_degree_cent.gsql)

## Installation 

### Replace `<Degree Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Degree Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Degree Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Degree Algorithm>
```
