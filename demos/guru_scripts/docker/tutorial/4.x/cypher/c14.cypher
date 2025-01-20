USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c14(){
   MATCH (src)-[e:transfer]-> (tgt1)
   WHERE src.name in ["Jenny", "Paul"]
   UNWIND [1, 2, 3] AS x //the "Jenny" row will be expanded to [Jenny, 1], [Jenny,2], [Jenny, 3]. Same fashion applies to the "Paul" row.
   WITH src AS srcAccount, e.amount * x AS res
   RETURN srcAccount, res
}

install query c14

run query c14()
