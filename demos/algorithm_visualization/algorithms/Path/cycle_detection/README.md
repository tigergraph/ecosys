
# Cycle Detection

#### [Cycle Detection Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/cycle_detection/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Cycle Detection Documentation](https://docs.tigergraph.com/graph-algorithm-library/path/cycle-detection)

## Available Cycle Detection Algorithms 

* [`tg_cycle_detection_count`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/cycle_detection/tg_cycle_detection_count.gsql)

* [`tg_cycle_detection`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Path/cycle_detection/tg_cycle_detection.gsql)

## Installation 

### Replace `<Cycle Detection Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Cycle Detection Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Cycle Detection Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Cycle Detection Algorithm>
```