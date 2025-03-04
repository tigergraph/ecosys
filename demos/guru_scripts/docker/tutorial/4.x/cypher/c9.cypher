USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c9() {

  MATCH (a:Account)
  WHERE a.name STARTS WITH "J"
  WITH a
  RETURN a.name

}

install query c9

run query c9()
