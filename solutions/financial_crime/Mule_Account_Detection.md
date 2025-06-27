# Narratives
Mule account detection in financial crime is a critical endeavor aimed at identifying bank accounts that are used to receive and disperse money from illicit activities. These mule accounts, which may be involved either knowingly or unknowingly in these operations, present a significant challenge for financial institutions. The rapid movement of funds through an extensive and seemingly unconnected network of accounts, spread across numerous financial institutions, complicates the tracking and halting of such illicit transactions. Financial institutions are therefore tasked with detecting this activity promptly to prevent further fund transfer and ensure the return of assets to their rightful owners. TigerGraph's solutions address this challenge through a suite of sophisticated graph algorithms that enable real-time monitoring, feature engineering for machine learning, and anomaly detection to trace illegal funds effectively. Community Detection is employed to unveil clusters within the transaction network, highlighting groups of accounts that work in concert to move illicit funds. The Centrality (PageRank) algorithm identifies key accounts that act as central nodes in the distribution network, crucial for disrupting the flow of illicit money. Closeness (Shortest Path) analysis reveals the most direct routes for money laundering, aiding in the trace back to the source. Lastly, Deep Link Analysis uncovers hidden connections between accounts, providing a comprehensive understanding of the network's structure and operation. Together, these algorithms form the backbone of TigerGraph's approach to dismantling the complex networks of mule accounts, safeguarding the financial system against the movement of illicit funds.

# Components
This repository includes multiple components:

- `Schema` - Definition of database schema.
- `Queries` - Collection of GSQL queries.
- `Mock Data` - Sample data.
- `Loading Jobs` - two loading jobs: one for cloud integration and the other for on-prem local deployment.
- `Insights Applications` - ROI Dashboard and Analytical Insights Applicaitions.
- `README.md` - This usage guide.
- `setup.sh` - Automated setup script.

# ML Features
### Here is a list of graph features we provide in the solution kit:

1. **Community Detection**: 
   We use the weighted Weakly Connected Components (WCC) algorithm to detect communities within the graph. This includes updating the size of each community and assigning community IDs, which help enhance the machine learning models for identifying mule accounts.

2. **PageRank Score**: 
   The PageRank score for each account vertex in the account network is calculated. This score represents the importance or influence of each account, aiding the machine learning models in detecting mule accounts.

3. **Shortest Path Length**: 
   We calculate the shortest path length from each account to identified mule accounts. This metric helps determine the proximity of each account to known mule accounts, providing valuable features for the models.

4. **IP Sharing**: 
   The number of mule accounts that share the same IP address for each account is calculated. This feature helps identify suspicious accounts connected through the same IP address.

5. **Multi-Hop IP Analysis**: 
   We calculate the number of IP addresses linked to any mule accounts within a specified number of hops. This feature identifies potentially fraudulent IP connections, enhancing the machine learning models.

6. **Device Sharing**: 
   The number of mule accounts that share the same device for each account is calculated. This aids in identifying suspicious accounts connected through the same device.

7. **Multi-Hop Device Analysis**: 
   We calculate the number of devices linked to any mule accounts within a specified number of hops, helping to identify potentially fraudulent device connections.

8. **Transfer Ratio**: 
   The ratio of the total amount of money transferred to and from mule accounts relative to the total transfer amount for each account is calculated. This includes the ratio of incoming and outgoing transfers, identifying suspicious transfer patterns.

9. **Multi-Hop Mule Account Analysis**: 
   We calculate the total number of mule accounts within a specified number of hops from each account. This helps assess the proximity and density of mule accounts in the network.

# Instructions

1. **Schema Creation**: This schema is a subset of the Super Schema in financial_crime/library/schema. To create the schema, first run `gsql /home/tigergraph/solution_kits/financial_crime/library/schema/general_global_financial_crime_super_schema.gsql` to generate global vertex and edge types. Then, user can run `/home/tigergraph/solution_kits/financial_crime/mule_account_detection/schema/create_schema_bottomup.gsql` to create the graph. 
2. **Data Loading**: Load data into the schema by running the data loading job with the `local_loading_job.gsql` script. 
3. **Query Installation**: Completes the setup by installing necessary queries through the `install_queries.sh` script.

## Query Execution Order and Explanations

### Step 1: Insert Edges for Merchant and Card Networks

To initiate the `wcc` and `pagerank` algorithms on the Merchant and Card network, it's essential to first execute the following two queries:

- `account_account_with_weights`

### Step 2: Form Communities

The following query runs weighted wcc algorithm using the weights on Account_Account edge.

- `tg_wcc_account_with_weights`


Subsequent queries can be run following the completion of the aforementioned two.

### Step 3: Feature Engineering Queries

The feature engineering queries generate feature values to feed the downstream ML model. These queries aggregate the values and propagate the features as attributes of the `Account` vertex. To use the ML model documented in the `model` folder, the following feature engineering queries need to be executed. The right column lists the attribute name corresponding to the query.
| Query                               | Attribute Name(Feature)                 | Type   |
|-------------------------------------|----------------------------|--------|
| `tg_wcc_account_with_weights`       | `com_size` & `com_id`      | INT    |
| `tg_pagerank_wt_account`            | `pagerank`                 | FLOAT  |
| `tg_shortest_path_length_account`   | `shortest_path_length`     | INT    |
| `number_of_mule_accounts_on_same_IP`| `ip_collision`             | INT    |
| `n_hop_fraud_count_ip`              | `fraud_ip`                 | INT    |
| `number_of_mule_accounts_on_same_device` | `device_collision`         | INT    |
| `n_hop_fraud_count_device`          | `fraud_device`             | INT    |
| `ratio_of_mule_account_transfer`    | `trans_in_mule_ratio (out)`| FLOAT  |
| `n_hop_number_of_total_mule_account`| `mule_cnt`                 | INT    |

### Step 4: Investigation Queries and Information Retrieval Queries

These queries can be executed at any time and have no dependencies on the previous queries. The investigation queries are designed to help investigate certain accounts, facilitating deep analysis. These queries include:

- `single_party_PII`: Provides personally identifiable information for a party involved in transactions.
- `attributes_to_party_traversal`: Traverses from transaction attributes to the parties involved.
- `party_full_address`: Retrieves the full address details for a party involved in transactions.


## Mock Data

The `data` folder is populated with sample data files. These files are crafted to closely mimic real-world scenarios, providing a realistic context for testing and demonstration purposes.



## Insights Applications

There are two insights applications:
- **ROI Dashboard**
  - Shows the transaction fraud losses.
  - Live total fraud losses and amount saved by TigerGraph.
  - Live fraud distribution.
  - ML performance.

- **Mule Account Detection**
  - The application has 5 pages:
    - Account network pagerank.
    - WCC Community.
    - Shortest Path.
    - IP Sharing Analysis.
    - Device Sharing Analysis.
 