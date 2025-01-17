USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c7 (datetime low, datetime high, string accntName) {

   // a path pattern in ascii art () -[]->()-[]->()
   // think the MATCH clause is a matched table with columns (a, e, b, e2, c)
   // you can use SQL syntax to group by on the matched table
   // Below query find 2-hop reachable account c from a, and group by the path a, b, c
   // find out how much each hop's total transfer amount.
   MATCH (a:Account)-[e:transfer]->(b)-[e2:transfer]->(c:Account)
   WHERE e.date >= $low AND e.date <= $high
   RETURN a, b, c, sum(DISTINCT e.amount) AS hop_1_sum,  sum(DISTINCT e2.amount) AS hop_2_sum   
   
}

install query c7

run query c7("2024-01-01", "2024-12-31", "Scott")
