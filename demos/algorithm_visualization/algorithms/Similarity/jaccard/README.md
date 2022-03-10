
# Jaccard

#### [Jaccard Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Similarity/jaccard/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Jaccard Documentation](https://docs.tigergraph.com/graph-algorithm-library/similarity/jaccard-similarity-of-neighborhoods-batch)

## Available Jaccard Algorithms 

* [`tg_jaccard_batch`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Similarity/jaccard/tg_jaccard_batch.gsql)

* [`tg_jaccard_nbor_ap_sub`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Similarity/jaccard/tg_jaccard_nbor_ap_sub.gsql)

* [`tg_jaccard_nbor_ap`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Similarity/jaccard/tg_jaccard_nbor_ap.gsql)

* [`tg_jaccard_nbor_ss`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Similarity/jaccard/tg_jaccard_nbor_ss.gsql)

## Installation 

### Replace `<Jaccard Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Jaccard Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Jaccard Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Jaccard Algorithm>
```