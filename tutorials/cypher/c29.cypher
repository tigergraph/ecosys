use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY deleteAllEdge(STRING name="Abby"){
  MATCH (s:Account {name: $name}) -[e] -> ()
  DELETE e
}

# Delete all outgoing relationships from the node with the name "Abby"
interpret query deleteAllEdge()
