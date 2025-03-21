use graph financialGraph

### multiple types
CREATE OR REPLACE OPENCYPHER QUERY deleteVertexWithType02(){
  MATCH (s:Account:Phone)
  DELETE s
}

# Delete all nodes with the label `Account` or `Phone`
interpret query deleteVertexWithType02()
