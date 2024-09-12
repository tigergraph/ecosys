# Sample Graph To Start With 
![Financial Graph](./FinancialGraph.jpg)

# Content
This GSQL tutorial contains 

- Setup schema (model)
- Load data
- Query Examples


# Setup Schema
Copy [ddl.gsql](./script/ddl.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql ddl.gsql
```

# Load Data
Copy [load.gsql](./script/load.gsql) to your container. 
Copy the following data files to your container.

- [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/account.csv)
- [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/city.csv)
- [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/phone.csv)
- [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/hasPhone.csv)
- [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/locate.csv)
- [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/transfer.csv)
  
Next, run the following in your container's bash command line. 
```
gsql load.gsql
```

# Query Examples

## Node Pattern
Copy [q1.gsql](./script/q1.gsql) to your container. 

```sql
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q1 () SYNTAX v3 {

  # select from a node pattern-- symbolized by ()
  # v is a vertex set variable, holding the selected vertex set
  v = SELECT a
      FROM (a:Account);

  # output vertex set variable v in JSON format
  PRINT v;
}

#compile and install the query as a stored procedure
install query q1

#run the query
run query q1()
```

You can group by on the matched node table, just as you group by a table and aggregate in SQL. 

```sql
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q1a () SYNTAX v3 {

  SELECT a.isBlocked, count(*) INTO T
  FROM (a:Account)
  GROUP BY a.isBlocked;

  PRINT T;
}

#compile and install the query as a stored procedure
install query q1a

#run the query
run query q1a()
```
## Edge Pattern
Copy [q2.gsql](./script/q2.gsql) to your container. 

```sql

USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q2 (string accntName) SYNTAX v3 {

  // declare an local sum accumulator; you can keep adding values into it
  // "local accumulator" means each vertex will have an accumulator of
  // the declared type and can be accumulated into based on the 
  // FROM clause pattern.
  SumAccum<int> @totalTransfer = 0;

  // match an edge pattern-- symbolized by ()-[]->()
  // v is a vertex set variable holding the selected vertex set
  v = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->(b:Account)
      //for each matched row, do the following accumulation
      ACCUM  b.@totalTransfer += e.amount;

  //output each v and their static attribute and runtime accumulators' state
  PRINT v;

}

#compile and install the query as a stored procedure
install query q2

#run the query
run query q2("Scott")
```

## Path Pattern
Copy [q3.gsql](./script/q3.gsql) to your container. 

```sql
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE QUERY q3 (datetime low, datetime high, string accntName) SYNTAX v3 {

  // a path pattern in ascii art () -[]->()-[]->()
  R = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->()-[e2:transfer]->(b:Account)
      WHERE e.date >= low AND e.date <= high and e.amount >500 and e2.amount>500;

      PRINT R;

  // below we use variable length path.
  // *1.. means 1 to more steps of the edge type "transfer"
  // select the reachable end point and bind it to vertex alias "b"
  R = SELECT b
      FROM (a:Account {name: accntName})-[:transfer*1..]->(b:Account);

      PRINT R;

}

install query q3

run query q3("2024-01-01", "2024-12-31", "Scott")
```

# Support
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/



