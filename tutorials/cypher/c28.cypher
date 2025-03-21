use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY deleteEdge(STRING name="Abby", DATETIME filterDate="2024-02-01"){
  MATCH (s:Account {name: $name}) -[e:transfer] -> (t:Account)
  WHERE e.date < $filterDate
  DELETE e
}

interpret query deleteEdge()
