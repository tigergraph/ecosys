USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c5(datetime low, datetime high, string accntName) {

  // a path pattern in ascii art () -[]->()-[]->()
  MATCH (a:Account {name: $accntName})-[e:transfer]->()-[e2:transfer]->(b:Account)
  WHERE e.date >= $low AND e.date <= $high and e.amount >500 and e2.amount>500
  RETURN b.isBlocked, b.name    
 
}

install query c5

run query c5("2024-01-01", "2024-12-31", "Scott")
