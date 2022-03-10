
# Strongly Connected Components

#### [Strongly Connected Components Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/connected_components/strongly_connected_components/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Strongly Connected Components Documentation](https://docs.tigergraph.com/graph-algorithm-library/community/strongly-connected-components-1)

## Available Strongly Connected Components Algorithms 

* [`tg_scc`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/connected_components/strongly_connected_components/tg_scc.gsql)

* [`tg_scc_small_world`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/connected_components/strongly_connected_components/tg_scc_small_world.gsql)

## Installation 

### Replace `<Strongly Connected Components Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Strongly Connected Components Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Strongly Connected Components Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Strongly Connected Components Algorithm>
```