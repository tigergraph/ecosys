use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY deleteAllVertex(){
  MATCH (s)
  DELETE s
}

interpret query deleteAllVertex()
