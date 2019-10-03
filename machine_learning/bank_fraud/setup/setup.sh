#!/bin/bash
gsql bank_schema_v2.gsql
gsql -g Bank bank_load_v2.gsql
gsql -g Bank "DROP QUERY ALL"
gsql -g Bank ../query/avg_amt_per_transaction.gsql
gsql -g Bank ../query/avg_num_of_diff_merchants_per_day.gsql
gsql -g Bank ../query/avg_num_of_transactions_per_day.gsql
gsql -g Bank ../query/clear_att_map.gsql
#gsql -g Bank ../query/has_fraud_history.gsql
gsql -g Bank ../query/num_of_fraud.gsql
gsql -g Bank ../query/num_of_fraud_merchants.gsql
gsql -g Bank ../query/print_transaction_features.gsql
gsql -g Bank ../query/top_categories.gsql
gsql -g Bank ../query/top_merchants.gsql
gsql -g Bank ../query/sub_fraud_in_nHops.gsql
gsql -g Bank ../query/fraud_in_nHops.gsql
gsql -g Bank install query all
