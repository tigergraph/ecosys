use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c21(String accntName){
  MATCH (srcAccount:Account {name: $accntName})
  OPTIONAL MATCH (srcAccount)- [e:transfer]-> (tgtAccount:Account)
  WHERE srcAccount.isBlocked
  RETURN srcAccount, tgtAccount
}

install query c21
run query c21("Jenny")
