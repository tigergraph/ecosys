Sample Graph To Start With 
==============================
![Financial Graph](../tutorial/FinancialGraph.jpg)

Contents
==============
This GSQL 101 tutorial contains 

- friendship.csv: the edge data
- person.csv: the vertex data
- gsql101-step1.gsql: the gsql script to setup the schema and load the data
- gsql101-step2.sh: the bash script to run built-in queries
- gsql101-step3.gsql: the gsql script to develop parameterized queries


How To Run
===============
We recommend you install jq and redirect the REST call result to jq before it is output. 
# define scheme & load data
gsql gsql101-step1.gsql
# run built-in queries
sh gsql101-step2.sh
# develop parameterized queries
gsql gsql101-step3.gsql


Documents
==============
https://docs.tigergraph.com/start/gsql-101


Support
===============
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/


- 
