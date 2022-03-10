
# Weakly Connected Components

#### [Weakly Connected Components Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/connected_components/weakly_connected_components/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Weakly Connected Components Documentation](https://docs.tigergraph.com/graph-algorithm-library/community/connected-components)

## Available Weakly Connected Components Algorithms 

* [`tg_wcc`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/connected_components/weakly_connected_components/tg_wcc.gsql)

* [`tg_wcc_small_world`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/connected_components/weakly_connected_components/tg_wcc_small_world.gsql)

## Installation 

### Replace `<Weakly Connected Components Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Weakly Connected Components Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Weakly Connected Components Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Weakly Connected Components Algorithm>
```