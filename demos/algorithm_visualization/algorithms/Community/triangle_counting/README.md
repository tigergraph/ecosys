
# Triangle Counting

#### [Triangle Counting Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/triangle_counting/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Triangle Counting Documentation](https://docs.tigergraph.com/graph-algorithm-library/community/triangle-counting)

## Available Triangle Counting Algorithms 

* [`tg_tri_count_fast`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/triangle_counting/tg_tri_count_fast.gsql)

* [`tg_tri_count`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Community/triangle_counting/tg_tri_count.gsql)

## Installation 

### Replace `<Triangle Counting Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Triangle Counting Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Triangle Counting Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Triangle Counting Algorithm>
```