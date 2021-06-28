#!/usr/bin/env python3
from neo4j import GraphDatabase, time
from datetime import datetime, date
from dateutil.relativedelta import relativedelta
from timeit import default_timer as timer
import time
import pytz
import csv
import re
import sys
import os
from pathlib import Path
import argparse
from bi import parse_queries, eval, stat

main_parser = argparse.ArgumentParser(description='Utilities working with Cypher for LDBC SNB with insertion/deletion.')
main_parser.set_defaults(func=lambda _: main_parser.print_usage())
main_parser.add_argument('data_dir', type=Path, help='data directory, e.g. $NEO4J_HOME/import/sf1/csv/bi/composite-projected-fk') #  hard coded now in dml
main_parser.add_argument('-b', '--begin', type=str, default='2012-09-13', help='start date')
main_parser.add_argument('-e', '--end', type=str, default='2012-12-31', help='end date')
main_parser.add_argument('-r', '--read_freq', type=int, default=30, help='read frequency in days')
main_parser.add_argument('-o', '--output', type=Path, default=Path('results'), help='result folder')
main_parser.add_argument('-q', '--queries', type=str, default='all', 
    help='querie numbers to run (default: all), numbers separated by comma. i.e., "1,2"') 
main_parser.add_argument('-p', '--parameter', type=str, default='auto', help='parameter, default is auto.') # can only read parameters now
main_parser.add_argument('-d','--datatype', default=Path('../parameters/dataType.json'), type=Path,
    help='JSON file containing containing the data types')    
main_parser.add_argument('-v', '--verbose', action='store_true', help='print for every query')

def write_txn_fun(tx, query_spec, batch, csv_file):
    result = tx.run(query_spec, batch=batch, csv_file=csv_file)
    return result.value()

def run_update(session, query_spec, batch, csv_file):
    start = time.time()
    result = session.write_transaction(write_txn_fun, query_spec, batch, csv_file)
    end = time.time()
    duration = end - start

    num_changes = result[0]
    return num_changes


if len(sys.argv) < 2:
    print("Usage: batches.py <DATA_DIRECTORY>")
    exit(1)

# to ensure that all inserted edges have their endpoints at the time of their insertion, we insert nodes first and edges second
insert_nodes = ["Comment", "Forum", "Person", "Post"]
insert_edges = ["Comment_hasCreator_Person", "Comment_hasTag_Tag", "Comment_isLocatedIn_Country", "Comment_replyOf_Comment", "Comment_replyOf_Post", "Forum_containerOf_Post", "Forum_hasMember_Person", "Forum_hasModerator_Person", "Forum_hasTag_Tag", "Person_hasInterest_Tag", "Person_isLocatedIn_City", "Person_knows_Person", "Person_likes_Comment", "Person_likes_Post", "Person_studyAt_University", "Person_workAt_Company", "Post_hasCreator_Person", "Post_hasTag_Tag", "Post_isLocatedIn_Country"]
insert_entities = insert_nodes + insert_edges

delete_nodes = ["Comment", "Post", "Forum", "Person"]
delete_edges = ["Forum_hasMember_Person", "Person_knows_Person", "Person_likes_Comment", "Person_likes_Post"]
delete_entities = delete_nodes + delete_edges

insert_queries = {}
for entity in insert_entities:
    with open(f"dml/ins-{entity}.cypher", "r") as insert_query_file:
        insert_queries[entity] = insert_query_file.read()

delete_queries = {}
for entity in delete_entities:
    with open(f"dml/del-{entity}.cypher", "r") as delete_query_file:
        delete_queries[entity] = delete_query_file.read()

def toStr(x_list):
    if isinstance(x_list[0], int) or isinstance(x_list[0], str):
        return ','.join([f'{x}' for x in x_list])
    if isinstance(x_list[0], float):
        return ','.join([f'{x:.2f}' for x in x_list])

def main(args):
    data_dir = args.data_dir
    network_start_date = datetime.strptime(args.begin, '%Y-%m-%d')
    network_end_date = datetime.strptime(args.end, '%Y-%m-%d')
    batch_size = relativedelta(days=1)
    
    tot_ins_time = 0
    tot_del_time = 0
    driver = GraphDatabase.driver("bolt://localhost:7687")
    session = driver.session()
    batch_id = network_start_date.strftime('%Y-%m-%d')       
    datatype = Path('../parameters/dataType.json')
    queries = parse_queries(args.queries)
    output = args.output/batch_id
    
    timelog = args.output/'timelog.csv'
    stat_name = ['nComment', 'nPost', 'nForum', 'nPerson', 'HAS_TAG', 'LIKES', 'KNOWS', 'REPLY_OF']
    logf = open(timelog, 'w')
    header = ['date'] + stat_name + ['ins','del'] + [f'bi{i}' for i in range(1,21)]
    logf.write(','.join(header)+'\n')
    batch_log = batch_id + ',' + toStr(stat()) + ',' + toStr([tot_ins_time, tot_del_time])
    if args.read_freq > 0:
        all_duration=eval(queries, output/'param.json', datatype, output)
        batch_log += ',' + toStr(all_duration)    
    logf.write(batch_log+'\n')
    logf.flush()
    date = network_start_date
    while date < network_end_date:
        # format date to yyyy-mm-dd
        batch_id = date.strftime('%Y-%m-%d')
        batch_dir = f"batch_id={batch_id}"
        print(f"#################### {batch_dir} ####################")
        t0 = timer()
        print("## Inserts")
        for entity in insert_entities:
            batch_path = f"{data_dir}/inserts/dynamic/{entity}/{batch_dir}"
            if not os.path.exists(batch_path):
                continue

            print(f"{entity}:")
            for csv_file in [f for f in os.listdir(batch_path) if f.endswith(".csv")]:
                print(f"- inserts/dynamic/{entity}/{batch_dir}/{csv_file}")
                num_changes = run_update(session, insert_queries[entity], batch_dir, csv_file)
                if num_changes == 0:
                    print("!!! No changes occured")
                else:
                    print(f"> {num_changes} changes")
                print()
            if args.verbose: logf.write(entity+ ',' + toStr(stat())+ '\n')
            
        t1 = timer()
        print("## Deletes")
        for entity in delete_entities:
            batch_path = f"{data_dir}/deletes/dynamic/{entity}/{batch_dir}"
            if not os.path.exists(batch_path):
                continue

            print(f"{entity}:")
            for csv_file in [f for f in os.listdir(batch_path) if f.endswith(".csv")]:
                print(f"- deletes/dynamic/{entity}/{batch_dir}/{csv_file}")
                num_changes = run_update(session, delete_queries[entity], batch_dir, csv_file)
                if num_changes == 0:
                    print("!!! No changes occured")
                else:
                    print(f"> {num_changes} changes")
                print()
            if args.verbose: logf.write(entity+ ',' + toStr(stat())+ '\n')
        
        t2 = timer()
        
        tot_ins_time += t1 - t0
        tot_del_time += t2 - t1
        date = date + batch_size
        batch_id = date.strftime('%Y-%m-%d')
        output = args.output/batch_id
        batchlog = f'{batch_id},'  + toStr(stat())
        batchlog += ',' + toStr([tot_ins_time, tot_del_time])
        if args.read_freq and (date - network_start_date).days % args.read_freq == 0:
            all_duration=eval(queries, output/'param.json', datatype, output)
            batchlog += ',' + toStr(all_duration)   
        logf.write(batchlog+'\n')
        logf.flush()
        tot_ins_time = 0
        tot_del_time = 0
    logf.close()
    session.close()
    driver.close()

if __name__ == '__main__':
    args = main_parser.parse_args()
    main(args)