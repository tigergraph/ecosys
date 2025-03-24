USE GRAPH financialGraph


CREATE OR REPLACE OPENCYPHER QUERY updateTransferAmt(STRING startAcct="Jenny", UINT newAmt=100){
  MATCH (s:Account {name: $startAcct})- [e:transfer]-> (t)
  WHERE not t.isBlocked
  SET e.amount = $newAmt
}

interpret query updateTransferAmt(_, 300)
