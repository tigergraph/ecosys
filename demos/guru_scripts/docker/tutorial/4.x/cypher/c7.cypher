USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c7(datetime low, datetime high, string accntName) {

   // below we use variable length path.
   // *1.. means 1 to more steps of the edge type "transfer"
   // select the reachable end point and bind it to vertex alias "b"
   // note:
   // 1. the path has "shortest path" semantics. If you have a path that is longer than the shortest,
   // we only count the shortest. E.g., scott to scott shortest path length is 4. Any path greater than 4 will
   // not be matched.
   // 2. we can not put an alias to bind the edge in the the variable length part -[:transfer*1..]->, but
   // we can bind the end points (a) and (b) in the variable length path, and group by on them.
   MATCH (a:Account {name: $accntName})-[:transfer*1..]->(b:Account)
   RETURN a, b, count(*) AS path_cnt 
   

}

install query c7

run query c7("2024-01-01", "2024-12-31", "Scott")
