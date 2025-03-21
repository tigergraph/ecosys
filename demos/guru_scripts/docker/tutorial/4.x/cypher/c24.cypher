use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY deleteOneVertex(STRING name="Abby"){
  MATCH (s:Account {name: $name})
  DELETE s
}

# delete "Abby"
interpret query deleteOneVertex("Abby")
