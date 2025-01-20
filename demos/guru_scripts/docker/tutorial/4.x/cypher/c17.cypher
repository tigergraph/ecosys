USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c17(){
  MATCH (s:Account {name: "Paul"})
  RETURN s AS srcAccount
  UNION
  MATCH (s:Account)
  WHERE s.isBlocked
  RETURN s AS srcAccount
}

install query c17

run query c17()
