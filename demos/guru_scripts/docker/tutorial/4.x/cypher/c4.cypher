USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c4() {

  //think the MATCH clause is a matched table with columns (a, e, b)
  //you can use SQL syntax to group by the source and target account, and sum the total transfer amount
  MATCH (a:Account)-[e:transfer]->(b:Account)
  RETURN a, b, sum(e.amount) AS transfer_total

}

#compile and install the query as a stored procedure
install query c4

#run the query
run query c4()
