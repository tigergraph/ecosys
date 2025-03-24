use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY insertVertex(STRING name, BOOL isBlocked){
  CREATE (p:Account {name: $name, isBlocked: $isBlocked})
}

# This will create an `Account` node with `name="Abby"` and `isBlocked=true`.
interpret query insertVertex("Abby", true)
