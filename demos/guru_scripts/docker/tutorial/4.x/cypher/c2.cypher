#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c2() {
  // MATCH a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node.
  // return will print out all the bound Account vertices in JSOn format.
  MATCH (a:Account)
  WHERE a.name = "Scott"
  RETURN a

}


install query c2

# run the compiled query
run query c2()
