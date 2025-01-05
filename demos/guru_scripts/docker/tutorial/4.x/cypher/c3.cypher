USE GRAPH financialGraph

# create a query
CREATE OR REPLACE OPENCYPHER QUERY c3(string acctName) {

      MATCH  (a:Account {name: $acctName})-[e:transfer]->(b:Account)
      RETURN b, sum(e.amount)

}

# compile and install the query as a stored procedure
install query c3

# run the compiled query
run query c3("Scott")
