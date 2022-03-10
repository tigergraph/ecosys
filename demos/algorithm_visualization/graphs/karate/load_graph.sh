gsql -g karate load_karate.gsql
gsql -g karate "RUN LOADING JOB load_karate USING vfile=\"data/v_info.csv\""
gsql -g karate "RUN LOADING JOB load_karate USING efile=\"data/e_list.csv\""