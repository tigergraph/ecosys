USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c15(){
  MATCH (src)-[e:transfer]->(tgt)
  WHERE src.name in ["Jenny", "Paul"]
  WITH src AS srcAccount, COLLECT(e.amount) AS amounts
  RETURN srcAccount, amounts
}

install query c15

run query c15()
