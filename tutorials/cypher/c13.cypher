USE GRAPH financialGraph  
CREATE OR REPLACE OPENCYPHER QUERY c13(){  
  MATCH (src)-[e:transfer]-> (tgt1)  
  MATCH (tgt1)-[e:transfer]-> (tgt2)  
  WITH src.name AS srcAccountName, COUNT(tgt2) AS tgt2Cnt   
  RETURN srcAccountName, tgt2Cnt  
  ORDER BY tgt2Cnt DESC, srcAccountName DESC  
  SKIP 1  
  LIMIT 3 
} 

install query c13

run query c13()
