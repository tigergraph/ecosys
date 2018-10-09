gsql schema.gsql
gsql load.gsql
gsql 'USE GRAPH purchase_fruit RUN LOADING JOB load_purchase USING f="./purchase.csv"'
gsql all_graph.gsql
gsql -g purchase_fruit 'install query all_graph'
gsql recommend_fruit.gsql
gsql -g purchase_fruit 'install query recommend_fruit'

