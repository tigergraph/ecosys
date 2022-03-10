
# Shortest Path

#### [Shortest Path Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/shortest_path/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Shortest Path Documentation](https://docs.tigergraph.com/graph-algorithm-library/path/single-source-shortest-path-weighted)

## Available Shortest Path Algorithms 

* [`tg_shortest_ss_any_wt`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/shortest_path/tg_shortest_ss_any_wt.gsql)

* [`tg_shortest_ss_no_wt`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/shortest_path/tg_shortest_ss_no_wt.gsql)

* [`tg_shortest_ss_pos_wt`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/shortest_path/tg_shortest_ss_pos_wt.gsql)

## Installation 

### Replace `<Shortest Path Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Shortest Path Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Shortest Path Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Shortest Path Algorithm>
```