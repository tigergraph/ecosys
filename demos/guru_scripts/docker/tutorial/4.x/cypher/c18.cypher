USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c18(){
  MATCH (s:Account {name: "Steven"})
  RETURN s AS srcAccount
  UNION ALL
  MATCH (s:Account)
  WHERE s.isBlocked
  RETURN s AS srcAccount
}

install query c18
run query c18()
