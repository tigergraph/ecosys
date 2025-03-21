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
GSQL >  SELECT s, e.amount AS amt, t
GSQL >  FROM (s:Account) - [e:transfer]-> (t:Account) 
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

```python
MATCH (s:Account)- [e1:transfer] ->()- [e2:transfer]-> (t)
WITH s.name as srcName, COUNT(distinct e1) as cnt1, COUNT(distinct e2) as cnt2
WHERE cnt1 > 1
RETURN srcName, cnt1, cnt2
ORDER BY cnt1 DESC, cnt2
LIMIT 3
```

---


## TABLE Syntax

In GSQL, the `TABLE` syntax is used to define intermediate or temporary tables that store query results during execution. These tables are not persistent and exist only within the scope of a query. They help structure and organize data before producing the final result.

### SELECT INTO TABLE statement

The `SELECT INTO TABLE` statement in GSQL is used to retrieve data and store it into a new table. This allows you to perform queries and store results for further operations or transformations.

#### Syntax

```python
SELECT column1, column2, ... INTO newTable FROM pattern 
[WHERE condition] 
[HAVING condition] 
[ORDER BY column1 [ASC|DESC], column2 [ASC|DESC], ...] 
[LIMIT number] 
[OFFSET number]
;
```
-   `column1, column2, ...`: Specifies the columns to retrieve.
-   `newTable`: The name of the new table where the query results will be stored.
-   `pattern`: Defines the data pattern, which can be a node, relationship, or a combination of both.
-   `WHERE condition`: Optional. Used to filter rows that satisfy specific conditions.
-   `HAVING condition`: Optional. Used to filter aggregated results.
-   `ORDER BY`: Optional. Specifies how the results should be sorted (either `ASC` for ascending or `DESC` for descending).
-   `LIMIT`: Optional. Limits the number of rows returned.
-   `OFFSET`: Optional. Specifies the number of rows to skip.

#### Example Usage:

```python
CREATE OR REPLACE QUERY selectExample2() SYNTAX v3 {  
  SELECT s.name AS acct, SUM(e.amount) AS totalAmt INTO T1  
  FROM (s:Account)- [e:transfer]-> (t:Account)  
  WHERE not s.isBlocked  
  HAVING totalAmt > 1000  
  ORDER BY totalAmt DESC  
  LIMIT 5 OFFSET 0  
  ;  
  
  PRINT T1;  
}
```
#### Explanation:

-   In this example, we are retrieving data from the `Account` nodes `s` and `t` connected by the `transfer` relationship.
-   The query aggregates the `amount` of the transfer and stores the result into a new table `T1`.
-   The `HAVING` clause filters the results to only include those with a `totalAmt` greater than 1000, and the results are ordered by `totalAmt` in descending order, limiting the output to the top 5 entries.
-   Finally, the content of table `T1` is printed.

---

### INIT statement

The `INIT` statement in GSQL is used to initialize a constant value and assign it to a table. This allows you to create a table with predefined constant values, which can be used for further operations or queries.

#### Syntax

```python
INIT tableName value1 AS column1, value2 AS column2, ...;
```
-   `tableName`: The name of the table to be initialized.
-   `value1, value2, ...`: The constant values to be assigned to the table.
-   `column1, column2, ...`: The names of the columns in the table that correspond to the values.

---

#### Example Usage:

```python
CREATE OR REPLACE QUERY initExample(int intVal = 10) syntax v3{  
  // Initialize a table with different constant values.  
  INIT T1 1 as col_1, true as col_2, [0.1, -1.1] as col_3;  
  PRINT T1;  
  
  // Initialize a table with variables and function calls  
  DATETIME date = now();  
  INIT T2 date as col_1, intVal as col_2, SPLIT("a,b,c", ",") as col_3;  
  PRINT T2;  
}
```

#### Explanation:

-   **Table Initialization with Constant Values**: The first `INIT` statement creates table `T1` with three columns (`col_1`, `col_2`, `col_3`). The values `1`, `true`, and `[0.1, -1.1]` are assigned to `col_1`, `col_2`, and `col_3`
- **Table Initialization with Variables**: The second `INIT` statement initializes table `T2`. It uses the current date and time (`now()`) for `col_1`, the input variable `intVal` for `col_2`, and splits a string `"a,b,c"` into an array and assigns it to `col_3`.

---

### ORDER TABLE

The `ORDER` statement in GSQL is used to sort tables based on one or more columns, with optional `LIMIT` and `OFFSET` clauses. This allows efficient ordering and retrieval of a subset of data.

#### Syntax

```python
ORDER tableName BY column1 [ASC|DESC], column2 [ASC|DESC] ... LIMIT number OFFSET number;
```
-   `tableName`: The table to be ordered.
-   `column1, column2, ...`: Columns to sort by.
-   `ASC | DESC`: Sorting order (ascending by default).
-   `LIMIT number`: Restricts the number of rows in the result.
-   `OFFSET number`: Skips a number of rows before returning results.

---

#### Example Usage:

```python
CREATE OR REPLACE QUERY orderExample(INT page=1) syntax v3{  
  
  SELECT s.name as acct, max(e.amount) as maxTransferAmt INTO T1  
       FROM (s:Account)- [e:transfer]-> (t:Account)  
    ;  
  
  ORDER T1 BY maxTransferAmt DESC, acct LIMIT 3 OFFSET 1 * page;  
  
  PRINT T1;  
}
```

#### Explanation
 - Selects account names and their maximum transfer amounts.
 - Sorts by `maxTransferAmt` (descending) and `acct` (ascending).
 - Returns 3 rows per page, skipping `page` rows for pagination.

---
 
### FILTER statement

The `FILTER` statement works by applying the condition to the specified `target_table`, modifying its rows based on the logical expression provided. Filters are applied sequentially, so each subsequent filter operates on the results of the previous one.

#### Syntax

```python
FILTER <target_table> ON <condition>;
```

#### Example Usage:

```python
CREATE OR REPLACE QUERY filterExample() SYNTAX v3 {
   SELECT s.name as srcAccount, e.amount as amt, t.name as tgtAccount INTO T
      FROM (s:Account) - [e:transfer]-> (t)
      ;

   FILTER T ON srcAccount = "Scott" OR amt > 10000;

   PRINT T;

   FILTER T ON srcAccount <> "Scott";

   PRINT T;
}
```

#### Explanation:

-   The first `FILTER` statement retains rows where `srcAccount` is "Scott" or `amt` is greater than `10000`.
-   The second `FILTER` statement removes rows where `srcAccount` is "Scott".
-   The `PRINT` statements display the intermediate and final results after filtering.

 ----------
 
 ### PROJECT Statement
 
The `PROJECT` statement reshapes a table by creating new derived columns based on expressions. These columns can result from arithmetic operations, string concatenation, or logical conditions. The `PROJECT` statement is particularly useful for preparing data for further analysis without altering the original table.

#### Syntax

 **1. Transforming Table Data**
 ```python
PROJECT tableName ON
   columnExpression AS newColumnName,
   ...
INTO newTableName;
```

 **2. Extracting Vertex Sets**
 
 Converts a table column containing vertex objects into a vertex set.
 
 ```python
 PROJECT tableName ON VERTEX COLUMN vertexColumnName INTO VSET;
 ```


#### Example 1: Transforming Table Data

```python
CREATE OR REPLACE QUERY projectExample() syntax v3{  
   SELECT s.name as srcAccount, p.number as phoneNumber, sum(e.amount) as amt INTO T1  
      FROM (s:Account {name: "Scott"}) - [e:transfer]-> (t),  
           (s) - [:hasPhone]- (p);  
  
   PRINT T1;  
  
   PROJECT T1 ON  
      T1.srcAccount + ":" + T1.phoneNumber as acct,  
      T1.amt * 2 as doubleAmt,  
      T1.amt % 7 as mod7Amt,  
      T1.amt > 10000 as flag  
   INTO T2;  
  
   PRINT T2;  
}
```

#### Explanation:

The `PROJECT` statement transforms the data by adding new calculated columns:

 - `acct`: Concatenates the account name (`srcAccount`) with the phone number (`phoneNumber`) into a single string.
 - `doubleAmt`: Doubles the value of `amt`.
 - `mod7Amt`: Computes the remainder when `amt` is divided by 7.
 - `flag`: Creates a boolean flag indicating whether `amt` is greater than `10,000`.

The `PROJECT` statement does not modify the original table (`T1`) but instead creates a new table (`T2`) with the transformed data. This ensures that the original data remains unchanged for future use.

#### Example 2: Extracting Vertex Sets from a Table

```python
CREATE OR REPLACE QUERY projectExample2() syntax v3{  
   SELECT tgt as tgtAcct, phone as tgtPhone INTO T1  
      FROM (s:Account {name: "Scott"}) - [e:transfer]-> (tgt:Account) - [:hasPhone] - (phone);  
  
   PRINT T1;  
  
   PROJECT T1 ON VERTEX COLUMN  
      tgtAcct INTO vSet1,  
      tgtPhone INTO vSet2  
      ;  
  
   VS_1 =  SELECT s FROM (s:vSet1);  
   VS_2 =  SELECT s FROM (s:vSet2);  
  
   PRINT VS_1, VS_2;  
}
```
**Explanation:**
**Create an intermediate table (`T1`)**: `T1` contains `tgtAcct` (target account) and `tgtPhone` (phone number linked to the account).
        
**Extract vertex sets using `PROJECT`**:

 - `PROJECT T1 ON VERTEX COLUMN tgtAcct INTO vSet1`: Extracts `tgtAcct` vertices into `vSet1`.
 - `PROJECT T1 ON VERTEX COLUMN tgtPhone INTO vSet2`: Extracts `tgtPhone` vertices into `vSet2`.

---

### JOIN statement

In GSQL queries, the `JOIN` operation is commonly used to combine data from multiple tables (or nodes and relationships). Depending on the specific requirements, different types of `JOIN` operations are used. Common `JOIN` types include `INNER JOIN`, `CROSS JOIN`, `SEMIJOIN`, and `LEFT JOIN`.

#### INNER JOIN

The `INNER JOIN` statement combines rows from both tables where the join condition is true. Only rows that have matching values in both tables are returned.

**Syntax:**
```python
JOIN <target_table1> table1_alias WITH <target_table2> table2_alias
  ON <condition>
PROJECT 
	<table1_alias columnExpression> as columnName1, 
	<table2_alias columnExpression> as columnName2,
	...
INTO newTableName;
```

**Example Usage:**
```python
CREATE OR REPLACE QUERY innerJoinExample(STRING accountName = "Scott") syntax v3{  
   SELECT s.name as srcAccount, sum(e.amount) as amt INTO T1  
      FROM (s:Account {name: accountName}) - [e:transfer]-> (t);  
  
   SELECT s.name, t.number as phoneNumber INTO T2  
      FROM (s:Account) - [:hasPhone]- (t:Phone);  
  
   JOIN T1 t1 WITH T2 t2  
     ON t1.srcAccount == t2.name  
   PROJECT  
     t1.srcAccount + ":" + t2.phoneNumber as acct,  
     t1.amt as totalAmt  
   INTO T3;  
  
   PRINT T3;  
}
```
**Explanation:**

-   **`INNER JOIN`** combines data from `T1` and `T2` based on matching `srcAccount` and `name`. Only rows with a match in both tables are returned, which results in a joined set of data containing the account name and the total transfer amount.

---

#### CROSS JOIN

The `CROSS JOIN` statement combines each row from the first table with all rows from the second table, producing the Cartesian product. This type of join does not require a condition and can potentially result in a large number of rows. If you want to eliminate duplicate rows from the result, you can use the `DISTINCT` keyword to return only unique combinations.

**Syntax:**
```python
JOIN <target_table1> table1_alias WITH <target_table2> table2_alias
PROJECT 
	<table1_alias columnExpression> as columnName1, 
	<table2_alias columnExpression> as columnName2,
	...
INTO newTableName;
```

**Example Usage:**
```python
CREATE OR REPLACE QUERY crossJoinExample(STRING accountName = "Scott") syntax v3{  
   SELECT s.name as srcAccount, sum(e.amount) as amt INTO T1  
      FROM (s:Account {name: accountName}) - [e:transfer]-> (t);  
  
   SELECT s.name, t.number as phoneNumber INTO T2  
      FROM (s:Account) - [:hasPhone]- (t:Phone);  
  
   JOIN T1 t1 WITH T2 t2  
   PROJECT distinct  
     t1.srcAccount + ":" + t2.phoneNumber as acct,  
     t1.amt as totalAmt  
   INTO T3;  
  
   PRINT T3;  
}
```
**Explanation:**

-   **`CROSS JOIN`** produces a Cartesian product between `T1` and `T2`. In this example, every `srcAccount` will be paired with every phone number from `T2`, resulting in all combinations of accounts and phone numbers.
-   **`DISTINCT`** is used to remove any duplicate combinations from the result. Without `DISTINCT`, you might get repeated rows if there are multiple matching rows in `T2` for each row in `T1`.
---

#### SEMIJOIN

The `SEMIJOIN` statement filters rows from the first table based on whether they have a matching row in the second table. It returns rows from the first table where the join condition is true, but **only columns from the left (first) table can be accessed**. The right table's columns are not included in the result.

**Syntax:**
```python
SEMIJOIN <target_table1> table1_alias WITH <target_table2> table2_alias
      ON <condition>
 PROJECT 
	<table1_alias columnExpression> as columnName1,
	<table1_alias columnExpression> as columnName2,
	...
INTO newTableName;
```

**Example Usage:**
```python
CREATE OR REPLACE QUERY semiJoinExample(STRING accountName = "Scott") syntax v3{  
   SELECT s.name as srcAccount, sum(e.amount) as amt INTO T1  
      FROM (s:Account) - [e:transfer]-> (t);  
  
   SELECT s.name, t.number as phoneNumber INTO T2  
      FROM (s:Account {name: accountName}) - [:hasPhone]- (t:Phone);  
  
   SEMIJOIN T1 t1 WITH T2 t2  
     ON t1.srcAccount == t2.name  
   PROJECT  
     t1.srcAccount as acct,  
     t1.amt as totalAmt  
   INTO T3;  
  
   PRINT T3;  
}
```

**Explanation:**

-   **`SEMIJOIN`** returns rows from `T1` where there is a matching row in `T2`, but only the columns from `T1` are included in the result. Even though there is a match between the two tables on `srcAccount` and `name`, **the result only includes columns from the left table (`T1`)**. This is useful when you want to check for the existence of matching rows without including data from the second table.

---

#### LEFT JOIN

The `LEFT JOIN` statement combines rows from both tables, but ensures that all rows from the left table (first table) are included, even if there is no matching row in the right table (second table). If no match exists, the right table's columns will have `NULL` values.

**Syntax:**

```python
LEFT JOIN <target_table1> table1_alias WITH <target_table2> table2_alias
  ON <condition>
PROJECT 
	<table1_alias columnExpression> as columnName1, 
	<table2_alias columnExpression> as columnName2,
	...
INTO newTableName;
```

**Example Usage:**

```python
CREATE OR REPLACE QUERY leftJoinExample(STRING accountName = "Scott") syntax v3{  
   SELECT s.name as srcAccount, sum(e.amount) as amt INTO T1  
      FROM (s:Account {name: accountName}) - [e:transfer]-> (t);  
  
   SELECT s.name, t.number as phoneNumber INTO T2  
      FROM (s:Account) - [:hasPhone]- (t:Phone)  
      WHERE s.name <> accountName;  
  
   LEFT JOIN T1 t1 WITH T2 t2  
     ON t1.srcAccount == t2.name  
   PROJECT  
     t1.srcAccount  as acct,  
     t2.phoneNumber as phoneNum,  
     t1.amt as totalAmt  
   INTO T3;  
  
   PRINT T3;  
}
```

**Explanation:**

-   **`LEFT JOIN`** returns all rows from `T1` (the left table), even if there is no matching row in `T2` (the right table). If no match is found, the columns from `T2` will be filled with `NULL`. In this example, even accounts without a phone number will appear in the result, with `phoneNumber` as `NULL`.

---

### UNION statement

The `UNION` statement in GSQL combines the results of two tables into a new table, ensuring **no duplicate rows** in the output by default. This operation is useful when merging datasets from different sources or when performing set operations on table results.

#### Syntax

```python
UNION table1 WITH table2 [WITH table3 ...] INTO newTable;
```
-   `table1`, `table2`, ...: Input tables that need to be combined.
-   `newTable`: The resulting table that stores the merged data.
-   The input tables **must have the same schema** (i.e., the same number of columns with matching data types).
-   Important Note: After UNION, the original tables (table1, table2, etc.) are destroyed and cannot be used in subsequent query operations.

#### Example Usage:

```python
CREATE OR REPLACE QUERY unionExample(STRING accountName = "Scott") syntax v3{  
   // Select accounts that transferred money
   SELECT s as acct INTO T1  
      FROM (s:Account {name: accountName}) - [e:transfer]-> (t);  
  
   // Select accounts by name
   SELECT s as acct INTO T2  
      FROM (s:Account {name: accountName})  
      ;  
   
   // Combine both results into table T3
   UNION T1 WITH T2 INTO T3;  
  
   PRINT T3;  
}
```

#### Explanation:

**Selecting Data Into Temporary Tables**

 - `T1`: Selects accounts that have transferred money.
 - `T2`: Selects accounts that match the given name.

**Performing the UNION Operation**
 `UNION T1 WITH T2 INTO T3;`：Merges results from `T1` and `T2`, removing duplicates.

**Printing the Final Result**
    `PRINT T3;` outputs the combined dataset.
    
---

### UNION ALL statement

The `UNION ALL` statement functions similarly to `UNION`, but **does not remove duplicate rows**. This operation is useful when preserving all records from input tables, even if they are identical.

#### Syntax

```python
UNION ALL table1 WITH table2 [WITH table3 ...] INTO newTable;
```

#### Example Usage:

```python
CREATE OR REPLACE QUERY unionAllExample(STRING accountName = "Scott") syntax v3{  
   SELECT s as acct INTO T1  
      FROM (s:Account {name: accountName}) - [e:transfer]-> (t);  
  
   SELECT s as acct INTO T2  
      FROM (s:Account {name: accountName})  
      ;  
  
   // Combine both results into table T3, keeping all duplicate rows
   UNION ALL T1 WITH T2 INTO T3;  
  
   PRINT T3;  
}
```

#### Explanation:

Using `UNION ALL` can improve performance when duplicate elimination is unnecessary, as it avoids the extra computation required to filter out duplicates.

---

### UNWIND statement

The `UNWIND` statement in GSQL is used to expand a list into multiple rows, allowing iteration over list elements and their combination with other tables. This is particularly useful when applying transformations or computations based on a set of values.

There are two main forms of `UNWIND`:

1.  **Expanding a fixed list** to initialize a new table.
2.  **Expanding a list per row** in an existing table.

#### Syntax

**Expanding a Fixed List (UNWIND INIT)**

```python
UNWIND [value1, value2, ...] AS columnName INTO newTable;
```

-   `[value1, value2, ...]`: A list of values to be expanded.
-   `columnName`: The alias for each value in the generated table.
-   `newTable`: The resulting table that stores the expanded rows.

**Expanding a List Per Row (UNWIND TABLE)**

```python
UNWIND tableName ON list AS columnName INTO newTable;
```

-   `tableName`: The input table whose rows will be expanded.
-   `list`: A list to be expanded for each row of `tableName`.
-   `columnName`: The alias for each value in the expanded list.
-   `newTable`: The resulting table that stores expanded rows while preserving columns from `tableName`.

#### Expanding a fixed list to init a new table Example:

```python
CREATE OR REPLACE QUERY unwindExample() syntax v3{  

  // Creates table `T1`, where each value from the list `[0.9, 1.0, 1.1]` is inserted as a separate row under the column `ratio`.
  UNWIND [0.9, 1.0, 1.1] AS ratio INTO T1;  
  PRINT T1;  
  
  SELECT s.name as acct, sum(e.amount) as totalAmt INTO T2  
       FROM (s:Account)- [e:transfer]-> (t:Account)  
      WHERE s.isBlocked  
    ;  
  
  // 1. Joins `T1` (containing ratios) with `T2` (containing account transfer sums).
  // 2. Computes a new column "resAmt = totalAmt * ratio" to adjust transfer amounts based on different ratios.
  // 3. Stores the result in new table T3.
  JOIN T1 t1 WITH T2 t2  
  PROJECT t2.acct as acct, t1.ratio as ratio, t2.totalAmt * t1.ratio as resAmt  
  INTO T3;  
  
  PRINT T3;  
}
```
#### Explanation:

-   The list `[0.9, 1.0, 1.1]` is expanded **independently** into a new table (`T1`).
-   Later, `T1` is **joined** with `T2` to apply multipliers to `totalAmt`.


#### Expanding a list for each row in an existing table Example:

```python
SET opencypher_mode = true
CREATE OR REPLACE QUERY unwindExample2() syntax v3{

  SELECT s.name as acct, [0.9, 1.0, 1.1] as ratioList, sum(e.amount) as totalAmt INTO T1
       FROM (s:Account)- [e:transfer]-> (t:Account)
      WHERE s.isBlocked
    ;

  UNWIND T1 ON ratioList AS ratio INTO T2;

  PRINT T2;
}
```
#### Explanation:

-   Instead of creating a separate table first (`T1`), the list column `ratioList` is **expanded per row of `T1`** directly into `T2`.
-   The columns from `T1` (like `acct` and `totalAmt`) are preserved in `T2`, with additional rows for each `ratio`.

---

## OpenCypher Data Modification Syntax

OpenCypher offers comprehensive support for performing data modification (Create, Update, Delete) operations on graph data. It provides an intuitive syntax to handle node and relationship manipulation, including their attributes.

### Insert Data

The `CREATE` clause in OpenCypher is used to **add** new nodes or relationships to the graph. If the node or relationship does not already exist, it will be created. If it exists, it will be replaced with the new data.

#### Insert Node
The following query creates a new `Account` node with properties `name` and `isBlocked`:
```python
CREATE OR REPLACE OPENCYPHER QUERY insertVertex(STRING name, BOOL isBlocked){
  CREATE (p:Account {name: $name, isBlocked: $isBlocked})
}

install query insertVertex

# This will create an `Account` node with `name="Abby"` and `isBlocked=true`.
run query insertVertex("Abby", true)
```

#### Insert Relationship
The following query creates a `transfer` edge between two `Account` nodes with properties `date` and `amount`

```python
CREATE OR REPLACE OPENCYPHER QUERY insertEdge(VERTEX<Account> s, VERTEX<Account> t, DATETIME dt, UINT amt){
  CREATE (s) -[:transfer {date: $dt, amount: $amt}]-> (t)
}

install query insertEdge

# This will create a `transfer` relationship from "Abby" to "Ed"
run query insertEdge("Abby", "Ed")
```
---

### Delete Data

The `DELETE` clause in OpenCypher is used to **remove nodes and relationships** from the graph. When deleting a node, all its associated relationships will also be deleted.

#### Delete a single node

When you delete a node, if it has relationships, all of its relationships will also be deleted.

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteOneVertex(STRING name="Abby"){
  MATCH (s:Account {name: $name})
  DELETE s
}

install query deleteOneVertex

# delete "Abby"
run query deleteOneVertex("Abby")
```

#### Delete all nodes of the specified type

You can delete all nodes of a particular label type.

**Single Label Type**

```python
### single type
CREATE OR REPLACE OPENCYPHER QUERY deleteAllVertexWithType01(){
  MATCH (s:Account)
  DELETE s
}

install query deleteAllVertexWithType01

# Delete all nodes with the label `Account`
run query deleteAllVertexWithType01()
```

**Multiple Label Types**

```python
### multiple types
CREATE OR REPLACE OPENCYPHER QUERY deleteVertexWithType02(){
  MATCH (s:Account:Phone)
  DELETE s
}

install query deleteVertexWithType02

# Delete all nodes with the label `Account` or `Phone`
run query deleteVertexWithType02()
```

#### Delete all nodes

This query deletes all nodes in the graph.

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteAllVertex(){
  MATCH (s)
  DELETE s
}

install query deleteAllVertex

run query deleteAllVertex()
```

#### Delete relationships

You can delete relationships based on specific conditions.

**Delete `transfer` Relationships with a Date Filter**

This query deletes all `transfer` relationships where the date is earlier than the specified filter date.

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteEdge(STRING name="Abby", DATETIME filterDate="2024-02-01"){
  MATCH (s:Account {name: $name}) -[e:transfer] -> (t:Account)
  WHERE e.date < $filterDate
  DELETE e
}

install query deleteEdge

run query deleteEdge()
```

**Delete all outgoing edges of a specific account**

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteAllEdge(STRING name="Abby"){
  MATCH (s:Account {name: $name}) -[e] -> ()
  DELETE e
}

install query deleteAllEdge

# Delete all outgoing relationships from the node with the name "Abby"
run query deleteAllEdge()
```

---

### Update Data

Updating data in OpenCypher allows you to modify node and relationship attributes. The primary mechanism for updating attributes is the `SET` clause, which is used to assign or change the properties of nodes or relationships.

#### Update vertex attributes

You can update the attributes of a node. In this example, the `isBlocked` attribute of the `Account` node is set to `false` for a given account name.

```python
CREATE OR REPLACE OPENCYPHER QUERY updateAccountAttr(STRING name="Abby"){
  MATCH (s:Account {name: $name})
  SET s.isBlocked = false
}

install query updateAccountAttr

# Update the `isBlocked` attribute of the `Account` node with name "Abby" to false
run query updateAccountAttr()
```

#### Update edge attributes

You can also update the attributes of a relationship. In this example, the `amount` attribute of a `transfer` relationship is updated for a specified account, as long as the target account is not blocked.

```python
CREATE OR REPLACE OPENCYPHER QUERY updateTransferAmt(STRING startAcct="Jenny", UINT newAmt=100){
  MATCH (s:Account {name: $startAcct})- [e:transfer]-> (t)
  WHERE not t.isBlocked
  SET e.amount = $newAmt
}

install query updateTransferAmt

run query updateTransferAmt("Jenny", 300)
```


---




