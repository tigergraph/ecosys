
# Pagerank

#### [Pagerank Changelog](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/pagerank/CHANGELOG.md) | [Discord](https://discord.gg/vFbmPyvJJN) | [Community](https://community.tigergraph.com) | [TigerGraph Starter Kits](https://github.com/zrougamed/TigerGraph-Starter-Kits-Parser)

## [TigerGraph Pagerank Documentation](https://docs.tigergraph.com/graph-algorithm-library/centrality/pagerank)

## Available Pagerank Algorithms 

* [`tg_pagerank_wt`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/pagerank/tg_pagerank_wt.gsql)

* [`tg_pagerank`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/pagerank/tg_pagerank.gsql)

* [`tg_pagerank_pers`](https://github.com/tigergraph/gsql-graph-algorithms/blob/master/algorithms/Centrality/pagerank/tg_pagerank_pers.gsql)

## Installation 

### Replace `<Pagerank Algorithm>` with desired algorithm listed above 

#### Via TigerGraph CLI

```bash
$ tg box algos install <Pagerank Algorithm>
```

#### Via GSQL terminal

```bash
GSQL > BEGIN
# Paste <Pagerank Algorithm> code after BEGIN command
GSQL > END 
GSQL > INSTALL QUERY <Pagerank Algorithm>
```