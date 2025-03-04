USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c10() {

MATCH (a:Account)
WITH a.isBlocked AS Blocked, COUNT(a) AS blocked_count
RETURN Blocked, blocked_count

}

install query c10

run query c10()

