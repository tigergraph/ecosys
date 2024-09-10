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
Copy the files to your container.

- [account.csv](./data/account.csv)
- [city.csv](./data/city.csv)
- [phone.csv](./data/phone.csv)
- [hasPhone.csv](./data/hasPhone.csv)
- [locate.csv](./data/locate.csv)
- [transfer.csv](./data/transfer.csv)
  
Next, run the following in your container's bash command line. 
```
gsql load.gsql
```

# Query

## Node Pattern
## Edge Pattern
## Path Pattern


# Support
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/



