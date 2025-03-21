use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY insertEdge(VERTEX<Account> s, VERTEX<Account> t, DATETIME dt, UINT amt){
  CREATE (s) -[:transfer {date: $dt, amount: $amt}]-> (t)
}

# Create two `transfer` relationships from "Abby" to "Ed"
interpret query insertEdge("Abby", "Ed", "2025-01-01", 100)
interpret query insertEdge("Abby", "Ed", "2025-01-09", 200)
