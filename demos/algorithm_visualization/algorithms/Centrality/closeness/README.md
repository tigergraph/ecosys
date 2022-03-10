
# Closeness

#### [Closeness Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/closeness/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Closeness Documentation](https://docs.tigergraph.com/graph-algorithm-library/centrality/closeness-centrality)

## Available Closeness Algorithms 

* [`tg_closeness_cent_approx`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/closeness/tg_closeness_cent_approx.gsql)

* [`tg_closeness_cent`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/closeness/tg_closeness_cent.gsql)

## Installation 

### Replace `<Closeness Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Closeness Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Closeness Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Closeness Algorithm>
```