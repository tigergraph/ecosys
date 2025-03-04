USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c16(){
   MATCH (src)-[e:transfer]->(tgt)
   WHERE src.name in ["Jenny", "Paul"]
   WITH src AS srcAccount, COLLECT(e.amount) AS amounts //collect will create ammounts list for each srcAccount
   UNWIND amounts as amount //for each source account row, inflate the row to a list of rows with each element in the amounts list
   WITH srcAccount, amount*2 AS doubleAmount
   RETURN srcAccount, doubleAmount
}

install query c16

run query c16()
