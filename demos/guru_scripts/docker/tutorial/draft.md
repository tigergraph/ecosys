## Introduction

`1-block SELECT`  is a feature that provides a concise syntax for querying data in a style similar to SQL or Cypher. This syntax allows users to write queries easily, retrieve data based on conditions, and supports various operations such as filtering, aggregation, sorting, and pagination.

----------

## Basic Syntax

The basic syntax structure of `1-block SELECT` is as follows:

```python
SELECT <output_variable> FROM <pattern> WHERE <condition> <additional_clauses>
```
### Usage Examples

#### Aggregation SELECT

```python
GSQL > SELECT COUNT(s) FROM (s:_)
GSQL > SELECT COUNT(*) FROM (s:Account:City)
GSQL > SELECT COUNT(DISTINCT t) FROM (s:Account)-[e]->(t)
GSQL > SELECT COUNT(e), STDEV(e.amount), AVG(e.amount) FROM (s:Account)-[e:transfer|isLocatedIn]->(t)
```

#### SELECT with filters

##### Using WHERE clause

```python
GSQL > SELECT s FROM (s:Account {name: "Scott"})
GSQL > SELECT s FROM (s:Account WHERE s.isBlocked)
GSQL > SELECT s FROM (s:Account) WHERE s.name IN ("Scott", "Steven")
GSQL > SELECT s, e, t FROM (s:Account) -[e:transfer]-> (t:Account) WHERE s <> t
```

##### Using HAVING

```python
GSQL > SELECT s FROM (s:Account) -[e:transfer]-> (t:Account) having s.isBlocked
GSQL > SELECT s FROM (s:Account) -[e:transfer]-> (t:Account) having s.isBlocked AND s.name = "Steven"
```

#### SELECT with Sorting and Limiting

```python
GSQL > SELECT s FROM (s:Account) ORDER BY s.name LIMIT 3 OFFSET 1
GSQL > SELECT s.name, e.amount as amt, t FROM (s:Account) -[e:transfer]-> (t:Account) ORDER BY amt, s.name LIMIT 1
GSQL > SELECT DISTINCT type(s) FROM (s:Account:City) ORDER BY type(s)
```

#### Using some expression

```python
# Using mathematical expressions
GSQL > SELECT s, e.amount*0.01 AS amt FROM (s:Account {name: "Scott"})- [e:transfer]-> (t)

# Using CASE expression
GSQL > BEGIN
GSQL > SELECT s, CASE WHEN e.amount*0.01 > 80 THEN true ELSE false END AS status 
GSQL > FROM (s:Account {name: "Scott"})- [e:transfer]-> (t)
GSQL > END
```
----------

## LET...IN SELECT... Syntax

The ```LET...IN SELECT ...``` construct in GSQL is used to define and work with variables and accumulators. It allows for the creation of temporary variables or accumulators in the LET block, which can be referenced later in the SELECT block, enabling more powerful and flexible queries.

**Syntax**
```python  
LET  
 <variable_definitions>;
IN  
 SELECT <query_block>
```  

**Introduction**

- ```LET```: This keyword starts the block where you define variables and accumulators.
    - Variable: Inside the `LET` block, you can define variables with basic types such as `STRING`, `INT`, `UINT`, `BOOL`, `FLOAT`, `DATETIME` and `DOUBLE`. However, container types like `SET`, `LIST`, and `MAP` are not supported at the moment.
    - Accumulator: Support base type and container type accumulators.
- ```IN```: The `IN` block follows the `LET` block and contains the `SELECT` query where the variables and accumulators defined in the `LET` block can be used.
- ```SELECT block```: The `SELECT` block is a GSQL query where the previously defined variables and accumulators are applied.

----------

### **Usage in Different Environments**


**GSQL Console (Interactive Mode)**

-   **Multi-line Queries:**
    -  **Must** be enclosed in `BEGIN...END`.
```python
GSQL > BEGIN 
GSQL > LET
GSQL >  DOUBLE a = 500.0; 
GSQL >  STRING b = "Jenny"; 
GSQL >  BOOL c = false; 
GSQL > IN 
GSQL >  SELECT s, e.amount AS amt, t GSQL > FROM (s:Account) - [e:transfer]-> (t:Account) 
GSQL >  WHERE s.isBlocked = c AND t.name <> b 
GSQL >  HAVING amt > a; 
GSQL > END
```

- **Single-line Queries:**
    - `BEGIN...END` is **not required**.
```python
GSQL > LET STRING n = "Jenny"; IN SELECT s, count(t) as cnt FROM (s:Account {name:n}) - [:transfer*0..2]-> (t:Account);
```
**Cloud GSQL Editor or Script File (`.gsql` file):**

-   `BEGIN...END` is **never required**, regardless of whether the query spans multiple lines or is written in a single line.

----------

### **Variable Usage in GSQL**

**variables** allow storing values for later use in queries. These variables are defined in the `LET` block and referenced in the `SELECT` block.

-   Variables **must** be terminated with a semicolon (`;`).
-   Supported base types:
    -   `STRING`, `INT`, `UINT`, `BOOL`, `FLOAT`, `DATETIME`, and `DOUBLE`.
-   Variables help make queries more flexible, reusable, and readable.
----------

**Example: Using Variables in a Query**
```python  
LET   
  DOUBLE a = 500.0;  //Define a threshold amount for filtering transactions
  STRING b = "Jenny"; //Define a name for filtering account holders
  BOOL c = false; //Define a boolean flag to filter blocked accounts
IN  
  SELECT s, e.amount as amt, t FROM (s:Account) - [e:transfer]-> (t:Account) 
  WHERE s.isBlocked = c 
    AND t.name <> b //Exclude transactions where the recipient’s name is "Jenny"
  HAVING amt > a;
```

**Explanation**

- Variable Declaration (`LET` block)
- Query Execution (`IN SELECT` block)
    -   The query filters transactions based on the defined variables.
    -   Only transactions above **500.0** are included (`HAVING amt > a`).
    -   Accounts where the recipient's name is **not "Jenny"** are included (`t.name <> b`).
    -   Blocked accounts are excluded (`s.isBlocked = c`).

----------


### **Accumulator Usage in GSQL**

In GSQL, **accumulators** are special variables used to store and update values during query execution. They are commonly utilized for aggregating sums, counts, sets, and other values across query results. Accumulators can be categorized into **local accumulators** (specific to individual vertices) and **global accumulators** (aggregating values across all selected nodes).

----------

### **Restrictions on SELECT Clause with Accumulators**

When accumulators are used in a query, the `SELECT` clause is subject to the following restrictions:

**Only One Node Alias Allowed:**

**If accumulators are used**, the `SELECT` clause must return only **one** node alias.

-  ✅ Source node alias **Allowed:**  `SELECT s FROM (s:Account)-[e:transfer]-(t:Account)`

-  ✅ Target node alias **Allowed:**  `SELECT t FROM (s:Account)-[e:transfer]-(t:Account)`

-  ❌ Relationship alias  **Not Allowed:**  `SELECT e FROM (s:Account)-[e:transfer]-(t:Account)`

-  ❌ Multiple node alias **Not Allowed:** `SELECT s, t FROM (s:Account)-[e:transfer]-(t:Account)`
  

**Functions, Expressions Not Allowed:**

**When using accumulators**, functions and expressions cannot be used in the `SELECT` clause. 

-  ❌ **Functions not allowed:**  `SELECT count(s) FROM (s:Account)-[e:transfer]-(t:Account)`

-  ❌ **Expressions not allowed:**  `SELECT (s.isBlocked OR t.isBlocked) AS flag FROM (s:Account)-[e:transfer]-(t:Account)`

----------

**Local Accumulator Example**

**Definition:**

A **local accumulator** (prefixed with `@`) is associated with a specific vertex and is stored as part of its attributes. When a node is retrieved in a `SELECT` statement, its corresponding local accumulator values are displayed alongside its properties.

**Query:**

```python
LET  
  SetAccum<string> @transferNames;  
IN  
  SELECT s FROM (s:Account where s.isBlocked) -[:transfer*1..3]- (t:Account)  
       ACCUM  
         s.@transferNames += t.name;
```
**Output:**
```json
{
  "version": {
    "edition": "enterprise",
    "api": "v2",
    "schema": 0
  },
  "error": false,
  "message": "",
  "results": [
    {
      "Result_Vertex_Set": [
        {
          "v_id": "Steven",
          "v_type": "Account",
          "attributes": {
            "name": "Steven",
            "isBlocked": true,
            "@transferNames": [
              "Jenny",
              "Paul",
              "Scott",
              "Steven",
              "Ed"
            ]
          }
        }
      ]
    }
  ]
}
```

**Explanation:**

-   The **local accumulator** `@transferNames` collects the names of accounts connected to `"Steven"` via `transfer` edges (up to 3 hops).
-   In the output, `@transferNames` is **stored as part of node's attributes**, alongside other attributes -`name` and `isBlocked`.

----------

**Global Accumulator Example**

**Definition:**

A **global accumulator** (prefixed with `@@`) aggregates values across all selected nodes and is **printed separately** from node attributes in the query result. All global accumulators declared in the `LET` block will be included in the output.

**Query:**
```python
LET   
  double ratio = 0.01;  
  SumAccum<double> @totalAmt;  // local accumulator for total amount 
  SumAccum<INT> @@cnt;  // global accumulator for count
IN  
  SELECT s FROM (s:Account {name:"Ed"}) - [e:transfer]-> (t:Account) 
  ACCUM s.@totalAmt += ratio * e.amount, // Accumulate total amount for s 
  @@cnt += 1; // Accumulate count of transfers
```
**Output:**
```json
{
  "version": {
    "edition": "enterprise",
    "api": "v2",
    "schema": 0
  },
  "error": false,
  "message": "",
  "results": [
    {
      "Result_Vertex_Set": [
        {
          "v_id": "Ed",
          "v_type": "Account",
          "attributes": {
            "name": "Ed",
            "isBlocked": false,
            "@totalAmt": 15
          }
        }
      ]
    },
    {
      "@@cnt": 1
    }
  ]
}
```
**Explanation:**

-   The **local accumulator** `@totalAmt` accumulates the weighted sum of transfer amounts and is **stored as an attribute of "Ed"**.
-   The **global accumulator** `@@cnt` counts the total number of transfers and is **printed separately** in the result.
-   This ensures **per-node values are displayed within node attributes, while global aggregates appear in the final output**.

----------

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

```python
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
