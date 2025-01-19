USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c11() {

   MATCH (a:Account)
   WITH a.name AS name
   WHERE name STARTS WITH "J"
   RETURN name


}

install query c11

run query c11()


