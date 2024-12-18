# Sample Graph To Start With <a name="top"></a>
![Financial Graph](./FinancialGraph.jpg)

# Content
This OpenCypher tutorial contains
- [Setup Environment](#setup-environment)
- [Setup Schema](#setup-schema)
- [Load Data](#load-data)
  - [Load Sample Data from S3 Bucket](#load-sample-data-from-s3-bucket)
  - [Load from Local Files](#load-from-local-files)
- [Cypher Syntax Overview](#cypher-syntax-overview)
  - [Pattern Matching](#pattern-matching)
    - [MATCH Clause](#match-clause)
    - [OPTIONAL MATCH Clause](#optional-match-clause)
  - [Filtering Results](#filtering-results)
    - [WHERE Clause](#where-clause)
  - [Query Segmentation](#query-segmentation)
    - [WITH Clause](#with-clause)
  - [Sorting and Limiting Results](#sorting-and-limiting-results)
    - [ORDER BY, SKIP, and LIMIT Clauses](#order-by-skip-and-limit-clauses)
  - [Working with Lists](#working-with-lists)
    - [UNWIND Clause](#unwind-clause)
  - [Combining Results](#combining-results)
    - [UNION Clause](#union-clause)
    - [UNION ALL Clause](#union-all-clause)
  - [Conditional Logic](#conditional-logic)
    - [CASE Expression](#case-expression)
  - [Functions](#functions)
    - [Aggregation Functions](#aggregation-functions)

# Setup Environment

Follow [Docker setup](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your Docker environment.

[Go back to top](#top)

# Setup Schema

Copy [ddl.gsql](./script/ddl.gsql) to your container. Next, run the following in your container's bash command line:

```bash
gsql ddl.gsql
```

[Go back to top](#top)

# Load Data

You can choose one of the following methods.

### Load sample data from our publicly accessible s3 bucket

1. Copy [load.gsql](./script/load.gsql) to your container.
2. Next, run the following in your container's bash command line:

    ```bash
    gsql load.gsql
    ```

   Or, in GSQL Shell editor, copy the content of [load.gsql](./script/load.gsql), and paste it into the GSQL shell editor to run.

### Load from local file in your container

1. Copy the following data files to your container:
  - [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/account.csv)
  - [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/phone.csv)
  - [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/city.csv)
  - [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/hasPhone.csv)
  - [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/locate.csv)
  - [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/transfer.csv)

2. Copy [load2.gsql](./script/load2.gsql) to your container. Modify the script with your local file path. Next, run the following in your container's bash command line:

    ```bash
    gsql load2.gsql
    ``` 

   Or, in GSQL Shell editor, copy the content of [load2.gsql](./script/load2.gsql), and paste it into the GSQL shell editor to run.

[Go back to top](#top)

# OpenCypher Syntax Overview

OpenCypher is a popular open-source declarative query language for property graphs. More about openCypher can be found at [openCypher.org](https://opencypher.org/). Below is a categorized introduction to essential openCypher clauses and expressions in TigerGraph.

---

## Pattern Matching

### MATCH Clause

`MATCH` clause is used to find patterns in the graph by matching vertices, edges, and paths based on specified criteria.

#### Syntax:

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY matchExample(STRING acctName="Jenny"){
  MATCH (srcAccount:Account {name: $acctName})-[e:transfer]->(tgtAccount:Account)-[:isLocatedIn]->(location:City)
  RETURN srcAccount, e AS transferDetails, tgtAccount, location AS locationCity
}
```

#### Key Points:
- `MATCH` clause: finds a pattern specified by ASCII art, where alternating node() and edge -[]-> pattern. E.g., a 2-hop pattern ()-[]->()-[]->(). 
- Specify edges with directions: `->`, `<-`, or `-`.
- Use JSON format to specify node or edge filters. E.g.,  `{name: $acctName}` filters the source `Account` vertex whose name attribute equals the value $acctName. 

### Multiple MATCH Clauses and Associations

#### Using Common Aliases to Link Patterns

In some queries, multiple `MATCH` clauses are used to match different parts of the graph, and edges between them are established using common aliases. The result is a graph traversal that connects different parts based on shared elements.

### Example 1

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY multipleMatchExample01(){
  MATCH (s:Account {name: "Paul"})-[:transfer]->(mid)
  MATCH (mid)-[:transfer]->(t)
  RETURN t
}
```

### Example 1: Data Tables

#### Step 1: First MATCH Result

The first `MATCH` filters the `Account` node with `name = "Paul"` and the `transfer` edges to intermediate `mid`.

| **s (Source Account)** | **mid (Intermediate Account)** |
|------------------------|--------------------------------|
| Paul                   | Steven                         |
| Paul                   | Jenny                          |

#### Step 2: Second MATCH Result

The second `MATCH` clause uses the common alias `mid` to find the resulting accounts (`r`) connected by `transfer` edges.

| **mid (Intermediate Account)** | **r (Resulting Account)** |
|--------------------------------|---------------------------|
| Steven                         | Jenny                     |
| Jenny                          | Scott                     |

#### Final Result

The final result returns all `r` (resulting accounts) that are connected to the intermediate accounts (`mid`).

| **r (Resulting Account)** |
|---------------------------|
| Jenny                     |
| Scott                     |

---

#### Using WHERE to Explicitly Connect Patterns

An alternative to using common aliases is to connect patterns using the WHERE clause. This method can provide more flexibility, especially when the relationship between patterns is more complex or requires specific conditions.

### Example 2

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY multipleMatchExample02(){
  MATCH (s:Account {name: "Paul"})-[:transfer]->(mid)
  MATCH (ss)-[:transfer]->(t)
  WHERE ss = mid
  RETURN t
}
```

### Example 2: Data Tables

#### Step 1: First MATCH Result (table T)

After the first `MATCH`, you get a table (`T`) containing `Account` vertices with the name "Paul" and their corresponding transfer edges to intermediate vertices (`mid`).

| **T.s** | **T.mid** |
|---------|-----------|
| Paul    | Steven    |
| Paul    | Jenny     |


#### Step 2: Second MATCH Result (table T_1)

The second `MATCH` fetches all `Account` vertices (ss) and their transfer edges to another set of `Account` vertices (`t`), producing the table (`T_1`).

| **T_1.ss** | **T_1.t** |
|------------|-----------|
| Jenny      | Scott     |
| Steven     | Jenny     |
| Ed         | Paul      |
| Paul       | Jenny     |
| Scott      | Ed        |
| Scott      | Ed        |
| Scott      | Ed        |


#### Final Result (table T_2)

After applying the `WHERE ss = mid`, a `join` is performed between the two tables (`T` and `T_1`) where the `mid` in `T` matches the `ss` in `T_1`. The result is a final table (`T_2`):

| **T.mid** | **T_1.ss** | **T_1.t** |
|-----------|------------|-----------|
| Steven    | Steven     | Jenny     |
| Jenny     | Jenny      | Scott     |


---
[Go back to top](#top)

### OPTIONAL MATCH Clause

`OPTIONAL MATCH` matches patterns against your graph, just like `MATCH` does. The difference is that if no matches are found, `OPTIONAL MATCH` will use a `null` for missing parts of the pattern.

#### Syntax:

```graphql
CREATE OR REPLACE OPENCYPHER QUERY q(){
  MATCH (pattern)
  OPTIONAL MATCH (pattern)
  WHERE <condition>
  RETURN <result>
}
```

#### Query Example:

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY multipleOptionalMatchExample(){
  MATCH (srcAccount:Account)
  WHERE srcAccount.isBlocked
  OPTIONAL MATCH (srcAccount)-[e1:transfer]->(midAccount:Account)
  OPTIONAL MATCH (midAccount)-[e2:transfer]->(finalAccount:Account)
  RETURN srcAccount, e1 AS firstTransfer, midAccount, e2 AS secondTransfer, finalAccount
}
```

#### Explanation:
- The first `OPTIONAL MATCH` finds transfers from the source account to an intermediate account (`midAccount`).
- The second `OPTIONAL MATCH` finds transfers from the intermediate account to a final account (`finalAccount`).
- If either pattern is not found, the unmatched elements (`e1`, `e2`, `midAccount`, `finalAccount`) will be `null`.

---

### Example: Data Tables

#### Step 1: Base MATCH Result
The `MATCH` clause retrieves `Account` vertices based on the condition `isBlocked` (e.g., accounts 'Jenny' and 'Scott').

| **srcAccount** |
|----------------|
| Jenny          |
 | Scott          |

#### Step 2: First OPTIONAL MATCH Result
The first `OPTIONAL MATCH` retrieves transfers from 'Jenny' and 'Scott' to intermediate accounts (`midAccount`). This is the left table `srcAccount` joined with the right table `midAccount` based on the `transfer` edge. If no `transfer` exists, `e1` and `midAccount` will be `null`.

| **srcAccount** | **e1 (First Transfer)** | **midAccount** |
|----------------|-------------------------|----------------|
| Jenny          | Transfer1               | Steven         |
| Scott          | NULL                    | NULL           |

#### Step 3: Second OPTIONAL MATCH Result
The second `OPTIONAL MATCH` retrieves transfers from the `midAccount` to the `finalAccount`. Here, the `midAccount` (from Step 2) is evaluated to find related final accounts (`finalAccount`). If no `transfer` exists, `e2` and `finalAccount` will be `null`.

| **midAccount** | **e2 (Second Transfer)** | **finalAccount** |
|----------------|--------------------------|------------------|
| Steven         | NULL                     | NULL             |
| NULL           | NULL                     | NULL             |

#### Final Result
Combining the results, the query returns:

| **srcAccount** | **e1 (First Transfer)** | **midAccount** | **e2 (Second Transfer)** | **finalAccount** |
|----------------|-------------------------|----------------|--------------------------|------------------|
| Jenny          | Transfer1               | Steven         | NULL                     | NULL             |
| Scott          | NULL                    | NULL           | NULL                     | NULL             |

### Summary of OPTIONAL MATCH

The `OPTIONAL MATCH` clause in Cypher performs multi-table associations using a Left Lateral Join. This means that for each element in the left table (the first matched pattern), the right table (the second pattern) is evaluated. If no corresponding match is found in the right table, the unmatched elements are returned as `null`.

---
[Go back to top](#top)

## Filtering Results

### WHERE Clause

The `WHERE` clause in OpenCypher is used to filter results based on specific conditions. It narrows down the set of vertices and edges that match a given pattern, ensuring only relevant data is returned.

#### Syntax:

```graphql
CREATE OR REPLACE OPENCYPHER QUERY q(){
  MATCH (pattern)
  WHERE <condition>
  WITH <intermediate_result_or_aggregation>
  WHERE <condition>
  RETURN <result>
}
```

#### Query Example:

- **After `MATCH` or `OPTIONAL MATCH`**:
  - When `WHERE` follows a `MATCH` or `OPTIONAL MATCH` clause, it filters the pattern results to include only those that satisfy the conditions specified in the `WHERE` clause.
  - Example 1: Restrict transfers to those where the source account is blocked and the transfer date is after a given date.

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY whereExample1(datetime date=to_datetime("2024-01-01")){
  MATCH (src)-[e:transfer]-> (tgt)
  WHERE src.isBlocked
    AND e.date > $date
  RETURN src AS srcAccount, e AS transferDetails, tgt AS tgtAccount
}
```
**After `WITH` clause**:
  - `WHERE` clause can be placed after a `WITH` clause to filter intermediate results. This allows you to apply conditions based on aggregated or calculated data.
  - Example 2: Filter accounts with more than one transfer.

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY whereExample2(){
  MATCH (src)-[e:transfer]-> (tgt)
  WHERE NOT src.isBlocked
  WITH
    src, COUNT(e) AS transferCnt
  WHERE transferCnt > 1
  RETURN src AS srcAccount, transferCnt
}
```

#### Key Points:
- Supports logical operators like `AND`, `OR`, `NOT`.
- Supports string matching, such as `STARTS WITH`, `ENDS WITH`.
- Support `null` checks using `IS NULL` or `IS NOT NULL`.
- Allows filtering by attributes.
- Supports list filtering using `IN` to check if a value exists within a list.

---
[Go back to top](#top)

## Query Segmentation

### WITH Clause

The `WITH` clause in OpenCypher is used to divide a query into logical segments, allowing intermediate results to be processed and passed to subsequent parts of the query. It is especially useful for performing transformations, aggregations, or applying conditions step by step.

#### Syntax:

```graphsql
CREATE OR REPLACE OPENCYPHER QUERY q() {
  MATCH (pattern)
  WITH < intermediate result> 
  WHERE <condition>
  WITH < intermediate result>
  RETURN <result>
}
```

#### Query Example:

**Example 1: Conditional Logic in `WITH`**

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY withExample1(){
  MATCH (src)-[e:transfer]-> (tgt)
  WITH
    src, COUNT(e) AS transferCnt
  WITH
    src,
    CASE
      WHEN transferCnt > 3 THEN true
      ELSE false
    END AS isTgt
  RETURN src AS srcAccount, isTgt
}
```
**Explanation**

- **First Query Segment**:  The `WITH` clause aggregates transfers using `COUNT(e)` and stores the result as `transferCnt` while passing the src forward.
- **Second Query Segment**:  A second WITH clause applies a `CASE expression` to evaluate whether `transferCnt` exceeds 3.

**Example 2: Filtering and Sorting with `WITH`**

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY withExample2(){
  MATCH (src)-[e:transfer]->(tgt)
  WITH src, COUNT(e) AS transferCnt
  WHERE transferCnt > 1
  ORDER BY transferCnt DESC
  LIMIT 5
  RETURN src AS sourceAccount, transferCnt
}
```
**Explanation**
- This query aggregates the transfer count for each `src` account, filters the accounts with more than one transfer, orders them by the transfer count in descending order, and limits the result to the top 5 accounts.

#### Key Points:
- The `WITH` clause is useful for:
  - Performing aggregations like `COUNT()`, `SUM()`, `AVG()`, etc. 
  - Applying conditions and filtering intermediate results.
  - Breaking down a complex query into manageable, logical segments.
  - Using `ORDER BY` and `LIMIT` to control the order and number of results before passing them on.
  - Performing calculations or conditional transformations with expressions.

---
[Go back to top](#top)

## Sorting and Limiting Results

### ORDER BY, SKIP, and LIMIT Clauses

`ORDER BY` is a sub-clause following `RETURN` or `WITH`, and it specifies that the output should be sorted and how.
`SKIP` defines from which record to start including the records in the output.
`LIMIT` constrains the number of records in the output.

**Example Usage**:

**Example 1**: Using `ORDER BY`, `SKIP`, `LIMIT` After `WITH`

```graphql  
USE GRAPH financialGraph  
CREATE OR REPLACE OPENCYPHER QUERY OrderSkipLimitExample01(){  
  MATCH (src)-[e:transfer]-> (tgt1)  
  MATCH (tgt1)-[e:transfer]-> (tgt2)  
  WITH src.name AS srcAccountName, COUNT(tgt2) AS tgt2Cnt  
  ORDER BY tgt2Cnt DESC, srcAccountName DESC  
  SKIP 1  
  LIMIT 3  
  RETURN srcAccountName, tgt2Cnt  
} 
```  

**Explanation**:  
In this example, the query first uses `MATCH` and `WITH` to gather `srcAccountName` and `tgt2Cnt`. Then, it uses `ORDER BY` to sort the results by `tgt2Cnt` in descending order and `srcAccountName` in descending order. Afterward, `SKIP 1` skips the first result from the sorted list, and `LIMIT 3` restricts the output to the next 3 records. The final result returns `srcAccountName` and `tgt2Cnt`.


**Example 2** :  Using `ORDER BY`, `SKIP`, `LIMIT` After `RETURN`

```graphql  
USE GRAPH financialGraph  
CREATE OR REPLACE OPENCYPHER QUERY OrderSkipLimitExample02(){  
  MATCH (src)-[e:transfer]-> (tgt1)  
  MATCH (tgt1)-[e:transfer]-> (tgt2)  
  WITH src.name AS srcAccountName, COUNT(tgt2) AS tgt2Cnt   
  RETURN srcAccountName, tgt2Cnt  
  ORDER BY tgt2Cnt DESC, srcAccountName DESC  
  SKIP 1  
  LIMIT 3 
} 
```  

**Explanation**:  
This example is similar to the first one, but `ORDER BY`, `SKIP`, and `LIMIT` are applied after `RETURN`. This means the sorting, skipping, and limiting operations are performed on the final result set. The query first aggregates data using `MATCH` and `WITH`, then sorts the results by `tgt2Cnt` and `srcAccountName`, skips the first record, and limits the output to the next 3 results. Finally, `srcAccountName` and `tgt2Cnt` are returned.

#### Key Points:

- **ORDER BY**:
  - Default sorting is ascending (`ASC`), use `DESC` for descending order.
  - Can sort by multiple fields.
- **SKIP**:
  - Skips the first `N` records
  - Useful for pagination.
- **LIMIT**:
  - Restricts the number of records returned.
  - Often used with `SKIP` for pagination.

---  
[Go back to top](#top)

## Working with Lists

### UNWIND Clause

The `UNWIND` clause is used to transform a list into individual rows, allowing you to process each item in the list as a separate element in the query result.

#### Syntax:

```
UNWIND <list_expression> AS <variable>
```

#### Query Example:

**Example 1: Basic `UNWIND` Usage**

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY unwindExample01(){
  MATCH (src)-[e:transfer]-> (tgt1)
  UNWIND [1, 2, 3] AS x
  WITH src AS srcAccount, e.amount * x AS res
  RETURN srcAccount, res
}
```

**Explanation**:

-   The `UNWIND` clause expands the list `[1, 2, 3]` into individual values (`x`).
-   The `WITH` clause calculates the result (`res`) by multiplying `e.amount` by each value of `x`.
-   Finally, the `RETURN` clause outputs the `srcAccount` and the calculated `res` for each combination of `src` and the values of `x`.

**Example 2: Combining `UNWIND` with `COLLECT()`**

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY unwindExample02(){
  MATCH (src)-[e:transfer]->(tgt1)
  WITH src AS srcAccount, COLLECT(e.amount) AS amounts
  UNWIND amounts AS amount
  WITH srcAccount, amount, amount * 2 AS doubleAmount
  RETURN srcAccount, COLLECT(doubleAmount) AS doubledAmounts
}
```

**Explanation**:
- The `COLLECT(e.amount)` gathers all transfer amounts into a list for each `src` vertex.
- The `UNWIND` clause expands the amounts list into individual rows (`amount`).
- The query calculates `doubleAmount` by multiplying each amount by 2.
- The `COLLECT(doubleAmount)` groups the doubled amounts back into a list (`doubledAmounts`).

#### Key Points:
- Often used to process arrays from query results.
- Combine with `COLLECT()` to group rows back into lists.

---
[Go back to top](#top)

### Combining Results
The `UNION` and `UNION ALL` clauses in OpenCypher are used to combine results from multiple queries into a single result set. While both serve the same basic purpose, they differ in how they handle duplicate rows.

#### UNION Clause

The `UNION` clause combines the results of multiple queries and removes any duplicate rows.

**Syntax:**

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY unionExample(){
  MATCH (s:Account {name: "Paul"})
  RETURN s AS srcAccount
  UNION
  MATCH (s:Account)
  WHERE s.isBlocked
  RETURN s AS srcAccount
}
```

**Key Points:**
- Automatically removes duplicates between result sets.
- All queries must return the same number of columns with the same or compatible data types.
- Slower than `UNION ALL` due to the deduplication process.

#### UNION ALL Clause

The UNION clause combines the results of multiple queries and removes any duplicate rows.

**Syntax:**

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY unionExample(){
  MATCH (s:Account {name: "Paul"})
  RETURN s AS srcAccount
  UNION ALL
  MATCH (s:Account)
  WHERE s.isBlocked
  RETURN s AS srcAccount
}
```

**Key Points:**
- Does not remove duplicates, including all rows from the combined queries.
- All queries must return the same number of columns with the same or compatible data types.
- Faster than `UNION` since no deduplication is performed.

---
[Go back to top](#top)

## Conditional Logic

### CASE Expression

The `CASE` expression in OpenCypher allows you to implement conditional logic within a query, enabling dynamic result customization based on specific conditions.

#### Syntax:

```graphsql
CASE
  WHEN <condition1> THEN <result1>
  WHEN <condition2> THEN <result2>
  ...
  ELSE <default_result>
END
```
#### Query Example:

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY caseExprExample(){
  MATCH (s:Account {name: "Steven"})- [:transfer]-> (t)
  WITH
    s.name AS srcAccount,
    t.name AS tgtAccount,
    CASE
       WHEN s.isBlocked = true THEN 0,
       ELSE 1
    END AS tgt
  RETURN
    srcAccount, SUM(tgt) as tgtCnt
}
```

#### Example Explanation:

-   The `MATCH` clause identifies the `Account` node with the name "Steven" and finds all `transfer` edges from this vertex to others.
-   The `CASE` expression evaluates whether `s.isBlocked` is true. If it is, the result is `0`, otherwise, the result is `1`.
-   The `WITH` clause aggregates the conditional results, storing the result of the `CASE` expression as `tgt`.
-   The `RETURN` clause outputs the `srcAccount` and the total count of `tgt`.

#### Key Points:
- Supports `WHEN ... THEN ...` for conditionally assigning values and ELSE for default cases.
- Helps dynamically adjust query results based on certain conditions, making it useful for various data transformations and aggregations.

---
[Go back to top](#top)

## Functions

### Aggregation Functions

Aggregation functions in OpenCypher allow you to perform calculations over a set of values, summarizing or transforming the data into a single result. These functions are typically used in combination with the `WITH` or `RETURN` clauses to compute aggregate values based on certain criteria.

#### Common Aggregation Functions:

- `COUNT()`: Counts the number of items in a given set.
- `SUM()`: Computes the sum of numeric values.
- `AVG()`: Calculates the average of numeric values.
- `MIN()`: Finds the smallest value in a set.
- `MAX()`: Finds the largest value in a set.
- `COLLECT()`: Groups values into a list.
- `STDEV()`: Computes the standard deviation of values.
- `STDEVP()`: Computes the population standard deviation of values.

#### Example Usage:

```graphql
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY aggregateExample(){
  MATCH (src)-[e:transfer]->(tgt)
  WITH src.name AS srcAccount, 
       COUNT(DISTINCT tgt) AS transferCount, 
       SUM(e.amount) AS totalAmount,
       STDEV(e.amount) AS stdevAmmount
  RETURN srcAccount, transferCount, totalAmount, stdevAmmount
}
```

#### Example Explanation:

In this example:

-   The `MATCH` clause finds all `transfer` edges between `src` and `tgt` nodes.
-   The `WITH` clause uses several aggregation functions:
  -   `COUNT(DISTINCT tgt)` counts the number of distinct `transfer` vertices.
  -   `SUM(e.amount)` calculates the total amount transferred for each source account.
  -   `STDEV(e.amount)` calculates the standard deviation of the amounts transferred for each source account.

#### Key Points:

-   Aggregation functions are used to summarize or analyze data, such as counting, summing, or averaging values.
-   They work within the `WITH` clause to aggregate data before passing it along to further parts of the query.
-   Functions like `COLLECT()` can be used to group values into lists, while functions like `SUM()` and `AVG()` perform mathematical calculations on grouped data.

[Go back to top](#top)
