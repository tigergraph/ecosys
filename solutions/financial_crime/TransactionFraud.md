# Narratives
Credit card transaction fraud detection identifies and prevents unauthorized or deceptive transactions in real-time. It analyzes transaction data, including cardholder information, transaction details, and historical patterns, to detect anomalies and suspicious activity indicative of fraud. TigerGraph models complex relationships and patterns among entities such as cardholders, merchants, transactions, and geographic locations. This enable detection of fraudulent networks and patterns that may be difficult to uncover using traditional relational databases. Using graph algorithms enable organizations to detect and respond to fraudulent transactions quickly and efficiently, ultimately reducing financial losses and protecting consumers from fraudulent activity.

# Components
This repository includes multiple components:

- `Schema` - Definition of database schema.
- `Queries` - Collection of GSQL queries.
- `Query Installation Automation Script` - Scripts for automating the installation of queries.
- `Mock Data` - Sample data.
- `Loading Jobs` - Scripts for data loading tasks.
- `Insights Applications (JSON)` - Applications for data analysis and visualization, configured in JSON.
- `Machine Learning Model` - Source code for the machine learning model.
- `Unit Test` - Test cases for ensuring code reliability and correctness.



# Instructions

The `setup.sh` script is designed to streamline the initial setup process by sequentially executing the following steps:

1. **Schema Creation**: This schema is a subset of the Super Schema in financial_crime/library/schema. To create the schema, first run `gsql /home/tigergraph/solution_kits/financial_crime/library/schema/general_global_financial_crime_super_schema.gsql` to generate global vertex and edge types. Then, user can run `/home/tigergraph/solution_kits/financial_crime/transaction_fraud/schema/create_schema_bottomup.gsql` to create the graph. 
2. **Data Loading**: Load data into the schema by running the data loading job with the `2_load_data.gsql` script. 
3. **Query Installation**: Completes the setup by installing necessary queries through the `3_install_queries.sh` script.

## Installation Note for Queries

When installing the queries, there is **one specific query** that requires special attention:

- `mer_shortest_path_length` is a subquery of `all_shortest_path_length`.

To successfully install `all_shortest_path_length`, the `mer_shortest_path_length` query **must be installed first**.

The script `3_install_queries.sh` has already been configured to handle this dependency. This note is intended for users who wish to **manually install** these queries.



## Query Execution Order and Explanations

### Step 1: Insert Edges for Merchant and Card Networks

To initiate the `wcc` and `pagerank` algorithms on the Merchant and Card network, it's essential to first execute the following two queries:

- `merchant_merchant_with_weights`
- `card_card_with_weights`

### Step 2: Form Communities

The provided solution kit includes two variations of the `wcc` community algorithm. The first employs the default community algorithm found in the built-in algorithm library (i.e. `tg_wcc_card` and `tg_wcc_merchant`), while the second leverages edge weight-based `wcc` to enhance community detection according to the dataset. If user has own dataset, the first option is recommended. Users have the flexibility to select either option for clustering purposes. For this solution kit, we use `weighted wcc` for community detection, so the following two queries need to be executed in step 2:

- `tg_wcc_card_weight_based`
- `tg_wcc_merchant_weight_based`

Subsequent queries can be run following the completion of the aforementioned two.

### Step 3: Feature Engineering Queries

The feature engineering queries generate feature values to feed the downstream ML model. These queries aggregate the values and propagate the features as attributes of the `Payment_Transaction` vertex. To use the ML model documented in the `model` folder, the following feature engineering queries need to be executed. The right column lists the attribute name of `Payment_Transaction` corresponding to the query.

| Query                                          | Attribute                            | Type   |
|------------------------------------------------|--------------------------------------|--------|
| `community_size`                               | `mer_com_size / cd_com_size`         | INT    |
| `tg_pagerank_wt_merchant`                      | `mer_pagerank`                       | FLOAT  |
| `tg_pagerank_wt_card`                          | `cd_pagerank`                        | FLOAT  |
| `mer_shortest_path_length`                     | `shortest_path_length`               | INT    |
| `all_shortest_path_length`                     | `shortest_path_length`               | INT    |
| `card_merchant_max_amount_within_interval`     | `max_txn_amt_interval`               | FLOAT  |
| `gender`                                       | `gender`                             | STRING |
| `age`                                          | `age`                                | INT    |
| `city_pop`                                     | `city_pop`                           | INT    |
| `occupation`                                   | `occupation` (job is a reserved keyword) | STRING |
| `NA` (original attribute value)                | `unix_time`                          | INT    |
| `card_merchant_max_txn_count_in_interval`      | `max_txn_cnt_interval`               | INT    |
| `number_of_repeated_card`                      | `cnt_repeated_card`                  | INT    |
| `community_transaction_count`                  | `com_mer_txn_cnt / com_cd_txn_cnt`   | INT    |
| `community_transaction_total_amount`           | `com_mer_txn_total_amt/ com_cd_txn_total_amt` | FLOAT |
| `community_average_amount`                     | `com_mer_txn_avg_amt/ com_cd_txn_avg_amt` | FLOAT |
| `community_maximum_amount`                     | `com_mer_txn_max_amt/com_cd_txn_max_amt` | FLOAT |
| `community_minimum_amount`                     | `com_mer_txn_min_amt/com_cd_txn_min_amt` | FLOAT |
| `merchant_category`                            | `mer_cat`                            | STRING |
| `merchant_category_transaction_count`          | `mer_cat_cnt`                        | INT    |
| `merchant_category_transaction_total_amount`   | `mer_cat_total_amt`                  | FLOAT  |
| `merchant_category_transaction_avg_amount`     | `mer_cat_avg_amt`                    | FLOAT  |
| `merchant_category_transaction_maximum_amount` | `mer_cat_max_amt`                    | FLOAT  |
| `merchant_category_transaction_minimum_amount` | `mer_cat_min_amt`                    | FLOAT  |
| `degrees`                                      | `indegree` / `outdegree`             | INT    |

### Step 4: Investigation Queries and Information Retrieval Queries

These queries can be executed at any time and have no dependencies on the previous queries. The investigation queries are designed to help investigate certain merchants, cards, and transactions, facilitating deep analysis. These queries include:

- `merchant_with_single_large_transaction`: Identifies merchants with a single, notably large transaction.
- `merchant_has_frequent_transactions`: Finds merchants with a high frequency of transactions.
- `merchant_has_large_total_amount`: Locates merchants with large total transaction amounts.
- `card_with_single_large_transaction`: Identifies cards involved in a single, notably large transaction.
- `card_has_frequent_transactions`: Finds cards with a high frequency of transactions.
- `card_has_large_total_amount`: Locates cards with large total transaction amounts.
- `single_card_lookup`: Allows for the lookup of a single card's transaction history.
- `single_transaction_lookup`: Enables the lookup of a single transaction's details.
- `single_merchant_lookup`: Allows for the lookup of a single merchant's transaction history.
- `single_party_PII`: Provides personally identifiable information for a party involved in transactions.
- `attributes_to_party_traversal`: Traverses from transaction attributes to the parties involved.
- `party_full_address`: Retrieves the full address details for a party involved in transactions.
- `merchant_transactions_stats`: Provides statistical analysis of transactions per merchant.
- `card_transactions_stats`: Offers statistical analysis of transactions per card.
- `merchant_category_transaction_stats`: Delivers transaction statistics categorized by merchant types.

## Mock Data

The `data` folder is populated with sample data files. These files are crafted to closely mimic real-world scenarios, providing a realistic context for testing and demonstration purposes.

## ML Model and Insights Application

- You can find the instructions for training the ML model and its performance metrics within the documentation located in the model folder.
- The Insights Applications are available as JSON files in the meta folder.

### Insights Applications

There are two insights applications:
- **ROI Dashboard**
  - Shows the transaction fraud losses.
  - Live total fraud losses and amount saved by TigerGraph.
  - Live fraud distribution.
  - ML performance.

- **Transaction Fraud**
  - The application has 5 pages:
    - Merchant network pagerank.
    - Shortest path to frauds with user-specified limit.
    - Community.
    - Card With Large Total Transaction Amount.
    - Card with high transaction frequency.
  - **Note:** For "Card With Large Total Transaction Amount" and "Card with high transaction frequency," the graph only shows fraudulent transactions to avoid overcrowded visualizations.
