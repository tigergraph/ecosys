gsql -g wiki_vote load_wiki_vote.gsql
gsql -g wiki_vote "RUN LOADING JOB load_wiki_vote USING efile=\"data/e_list.csv\""