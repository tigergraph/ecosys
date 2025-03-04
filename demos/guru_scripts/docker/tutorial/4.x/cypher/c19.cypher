USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c19(){
  MATCH (s:Account {name: "Steven"})- [:transfer]-> (t)
  WITH
    s.name AS srcAccount,
    t.name AS tgtAccount,
    CASE
       WHEN s.isBlocked = true THEN 0
       ELSE 1
    END AS tgt
  RETURN srcAccount, SUM(tgt) as tgtCnt
}

install query c19
run query c19()
