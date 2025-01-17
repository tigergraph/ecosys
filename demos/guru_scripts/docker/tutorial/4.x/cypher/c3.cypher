USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c3(string accntName) {
  //to invoke a parameter, use $param. E.g., $accntName
  MATCH (a:Account {name: $accntName})-[e:transfer]->(b:Account)
  RETURN b, sum(e.amount) AS totalTransfer
  
}

#compile and install the query as a stored procedure
install query c3

#run the query
run query c3("Scott")
    
