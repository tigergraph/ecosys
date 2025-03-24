USE GRAPH financialGraph  

CREATE OR REPLACE OPENCYPHER QUERY updateAccountAttr(STRING name="Abby"){
  MATCH (s:Account {name: $name})
  SET s.isBlocked = false
}

# Update the `isBlocked` attribute of the `Account` node with name "Abby" to false
interpret query updateAccountAttr()
