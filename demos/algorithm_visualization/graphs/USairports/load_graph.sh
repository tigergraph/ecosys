gsql -g USairport load_USairport.gsql
gsql -g USairport "RUN LOADING JOB load_USairport USING vfile=\"data/v_info.csv\""
gsql -g USairport "RUN LOADING JOB load_USairport USING efile=\"data/e_list.csv\""