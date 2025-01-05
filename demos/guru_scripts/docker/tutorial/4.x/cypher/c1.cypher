#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c1() {
  // MATCH a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node. 
  // return will print out all the bound Account vertices in JSOn format.
  MATCH (a:Account)
  RETURN a
}

# To run the query, we need to install it first.
# Compile and install the query as a stored procedure
install query c1

# run the compiled query
run query c1()
