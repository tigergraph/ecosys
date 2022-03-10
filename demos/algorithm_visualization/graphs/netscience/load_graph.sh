gsql -g netscience load_netscience.gsql
gsql -g netscience "RUN LOADING JOB load_netscience USING efile=\"data/e_list.csv\""