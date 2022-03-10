
# Influence Maximization

#### [Influence Maximization Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/influence_maximization/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Influence Maximization Documentation](https://docs.tigergraph.com/graph-algorithm-library/)

## Available Influence Maximization Algorithms 

* [`tg_influence_maximization_greedy`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/influence_maximization/tg_influence_maximization_greedy.gsql)

* [`tg_influence_maximization_CELF`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/influence_maximization/tg_influence_maximization_CELF.gsql)

## Installation 

### Replace `<Influence Maximization Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Influence Maximization Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Influence Maximization Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Influence Maximization Algorithm>
```
