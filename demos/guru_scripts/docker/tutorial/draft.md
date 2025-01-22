## Introduction

`1-block SELECT`  is a feature that provides a concise syntax for querying data in a style similar to SQL or Cypher. This syntax allows users to write queries easily, retrieve data based on conditions, and supports various operations such as filtering, aggregation, sorting, and pagination.

----------

## Basic Syntax

The basic syntax structure of `1-block SELECT` is as follows:

```
SELECT <output_variable> FROM <pattern> WHERE <condition> <additional_clauses>
```
### Usage Examples

#### Aggregation SELECT

```
GSQL > SELECT COUNT(s) FROM (s:_)
GSQL > SELECT COUNT(*) FROM (s:Account:City)
GSQL > SELECT COUNT(DISTINCT t) FROM (s:Account)-[e]->(t)
GSQL > SELECT COUNT(e), STDEV(e.amount), AVG(e.amount) FROM (s:Account)-[e:transfer|isLocatedIn]->(t)
```

#### SELECT with filters

##### Using WHERE clause

```
GSQL > SELECT s FROM (s:Account {name: "Scott"})
GSQL > SELECT s FROM (s:Account WHERE s.isBlocked)
GSQL > SELECT s FROM (s:Account) WHERE s.name IN ("Scott", "Steven")
GSQL > SELECT s, e, t FROM (s:Account) -[e:transfer]-> (t:Account) WHERE s <> t
```

##### Using HAVING

```
GSQL > SELECT s FROM (s:Account) -[e:transfer]-> (t:Account) having s.isBlocked
GSQL > SELECT s FROM (s:Account) -[e:transfer]-> (t:Account) having s.isBlocked AND s.name = "Steven"
```

#### SELECT with Sorting and Limiting

```
GSQL > SELECT s FROM (s:Account) ORDER BY s.name LIMIT 3 OFFSET 1
GSQL > SELECT s.name, e.amount as amt, t FROM (s:Account) -[e:transfer]-> (t:Account) ORDER BY amt, s.name LIMIT 1
GSQL > SELECT DISTINCT type(s) FROM (s:Account:City) ORDER BY type(s)
```

#### Using Expression

```
# Using mathematical expressions
GSQL > SELECT s, e.amount*0.01 AS amt FROM (s:Account {name: "Scott"})- [e:transfer]-> (t)

# Using CASE expression
GSQL > BEGIN
GSQL > SELECT s, CASE WHEN e.amount*0.01 > 80 THEN true ELSE false END AS status 
GSQL > FROM (s:Account {name: "Scott"})- [e:transfer]-> (t)
GSQL > END
```

## LET...IN... Syntax

The ```LET...IN``` construct in GSQL is used to define variables or accumulators in the LET block and use them in a SELECT query within the IN block. This enables more flexible and powerful queries.

Syntax
```
LET
  <variable_definitions>
IN
  SELECT block
```

**Accumulator Usage Example**

```
LET 
  double ratio = 0.01;
  SumAccum<double> @totalAmt;  // local accumulator
  SumAccum<INT> @@cnt;  // global accumulator
IN
  SELECT s FROM (s:Account) - [e:transfer]-> (t:Account)
    ACCUM s.@totalAmt += ratio * e.amount,
          @@cnt += 1
    ORDER BY s.@totalAmt DESC
```

**Variable Usage Example**

```
LET 
  DOUBLE a = 500.0;
  STRING b = "Jenny";
  BOOL c = false;
IN
  SELECT s, e.amount as amt, t FROM (s:Account) - [e:transfer]-> (t:Account)
     WHERE s.isBlocked = c AND t.name <> b
    HAVING amt > a;
```

## OpenCypher Easy Usage Syntax

The system also supports simple usage of OpenCypher queries, allowing users to interact with the graph data using familiar Cypher syntax.

**Simple Queries**

Single-line queries can be directly executed in the GSQL console:

```
GSQL > use graph financialGraph
GSQL > MATCH (s:Account {name: "Jenny"})-[e]->(t) RETURN COUNT(e) AS cnt
```

**Multi-line Queries**

For multi-line queries, use the ```BEGIN...END``` block in the GSQL console:

```
GSQL > use graph financialGraph
GSQL > BEGIN
GSQL > MATCH (s:Account {name: "Jenny"})-[e]->(t)
GSQL > RETURN COUNT(e) AS cnt
GSQL > END
```

### Example of OpenCypher

This query counts the distinct transfer relationships for each account and returns the top 3 accounts with more than one transfer, ordered by the number of transfers.

```
MATCH (s:Account)- [e1:transfer] ->()- [e2:transfer]-> (t)
WITH s.name as srcName, COUNT(distinct e1) as cnt1, COUNT(distinct e2) as cnt2
WHERE cnt1 > 1
RETURN srcName, cnt1, cnt2
ORDER BY cnt1 DESC, cnt2
LIMIT 3
```
