gsql -g polblog load_polblog.gsql
gsql -g polblog "RUN LOADING JOB load_polblog USING vfile=\"data/v_info.csv\""
gsql -g polblog "RUN LOADING JOB load_polblog USING efile=\"data/e_list.csv\""