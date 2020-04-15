All queries could be installed by running "bash install.sh", which automatically cp all the necessary files and install all queries.

1) circle detection uses both batchCircleDetection.gsql which calls the single version of the query circleDetection.gsql. it search for the subgraph first and then do the path filter to find all valid circles.

2) oneStepTransSearch.gsql is the code used for Frequent Transaction model. This is just a special version of one step neighbor. oneDay node is used as the time index for the searching.

3) twoStepTransSearch.gsql is the code used for Frequent Two Step Transaction  model. It is a batch queries for two step neighbors but with path related filters, where each vertex keep a list of its neighbor and send this list to its neighbors so that everyone knows its two step neighbor.
