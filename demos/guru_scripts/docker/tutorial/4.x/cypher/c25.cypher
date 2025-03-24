use graph financialGraph

### single type
CREATE OR REPLACE OPENCYPHER QUERY deleteAllVertexWithType01(){
  MATCH (s:Account)
  DELETE s
}

# Delete all nodes with the label `Account`
interpret query deleteAllVertexWithType01()
