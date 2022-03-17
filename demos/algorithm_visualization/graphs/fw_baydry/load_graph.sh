gsql -g fw_baydry load_fw_baydry.gsql
gsql -g fw_baydry "RUN LOADING JOB load_fw_baydry USING vfile=\"data/v_info.csv\""
gsql -g fw_baydry "RUN LOADING JOB load_fw_baydry USING efile=\"data/e_list.csv\""