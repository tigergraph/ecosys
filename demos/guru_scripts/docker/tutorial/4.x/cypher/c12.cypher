USE GRAPH financialGraph  

CREATE OR REPLACE OPENCYPHER QUERY c12(){  
  MATCH (src)-[e:transfer]-> (tgt1)  
  MATCH (tgt1)-[e:transfer]-> (tgt2)  
  WITH src.name AS srcAccountName, COUNT(tgt2) AS tgt2Cnt  
  ORDER BY tgt2Cnt DESC, srcAccountName DESC  
  SKIP 1  
  LIMIT 3  
  RETURN srcAccountName, tgt2Cnt  
} 

install query c12

run query c12()
