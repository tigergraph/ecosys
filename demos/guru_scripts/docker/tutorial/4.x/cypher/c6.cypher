USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c6 (string accntName) {

  // a path pattern in ascii art () -[]->()-[]->()
  MATCH (a:Account {name: $accntName})-[:transfer*1..]->(b:Account)
  RETURN a, b     

}

install query c6

run query c6("Scott")
