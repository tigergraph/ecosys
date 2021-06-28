#!/usr/bin/env python3
import argparse
from neo4j import GraphDatabase, time, unit_of_work
from datetime import datetime
from neo4j.time import DateTime, Date
import time
import pytz
import csv
import re
import ast
import os
from pathlib import Path

parser = argparse.ArgumentParser(description='Cypher driver')
parser.add_argument('-q','--queries', default='all', type=str,
    help='querie numbers to run (default: all), numbers separated by comma. i.e., "1,2"')
parser.add_argument('-p','--parameter', default=Path('../parameters/sf1.json'), type=Path,
    help='JSON file containing the input parameters')
parser.add_argument('-d','--datatype', default=Path('../parameters/dataType.json'), type=Path,
    help='JSON file containing containing the data types')    
parser.add_argument('-o','--output', default=Path('results'), type=Path,
    help='directory to write results (default: ./results)')

@unit_of_work(timeout=600)
def query_fun(tx, query_spec, query_parameters):
    result = tx.run(query_spec, query_parameters)
    values = []
    for record in result:
        values.append(record.values())
    return values

def writeResult(result, filename):
    with open(filename, 'w') as f:
        for row in result:
            # convert neo4j dateTime to string
            for i,c in enumerate(row):
                if isinstance(c, DateTime): 
                    row[i] = f"{c.year}-{c.month:02d}-{c.day:02d} {int(c.hour):02d}:{int(c.minute):02d}:{int(c.second):02d}"
            f.write(str(row)+'\n')

def run_query(session, query_id, query_spec, query_parameters):
    #print(f'Q{query_id}: {query_parameters}')
    start = time.time()
    result = session.read_transaction(query_fun, query_spec, query_parameters)
    end = time.time()
    duration = end - start
    return result, duration

    
def convert_to_datetime(timestamp):
    dt = datetime.strptime(timestamp, '%Y-%m-%dT%H:%M:%S')
    return DateTime(dt.year, dt.month, dt.day, 0, 0, 0, pytz.timezone('GMT'))

def convert_to_date(timestamp):
    dt = datetime.strptime(timestamp, '%Y-%m-%d')
    return Date(dt.year, dt.month, dt.day)

def readJson(paramFile):
    with open(paramFile) as f: 
        txt = f.read().replace('\n','')
        return ast.literal_eval(txt)

def parse_queries(qstr):
    if qstr == 'all' or '':
        actual = {str(i) for i in range(1,21)}
    elif qstr.startswith('not:'):
        all = {str(i) for i in range(1,21)}
        exclude = set(qstr[4:].split(','))
        actual = all - exclude
    elif qstr.startswith('reg:'):
        r = re.compile(qstr[4:])
        actual = list(filter(r.match, [str(i) for i in range(20)]))
    else:
        actual = qstr.split(',')
    return  [str(i) for i in range(1,21) if str(i) in actual]
    

def stat():
    driver = GraphDatabase.driver("bolt://localhost:7687")
    # statistics 
    with driver.session() as session:
        query_file = open(f'stat.cypher', 'r')
        query_spec = query_file.read()
        stat,_ = run_query(session, 'stat', query_spec, None)
    driver.close()
    return stat[0]

def eval(queries, parameter, datatype, output):
    output.mkdir(parents=True, exist_ok=True)
    #time_dir = Path('elapsed_time')
    #time_dir.mkdir(parents=True, exist_ok=True)
    
    driver = GraphDatabase.driver("bolt://localhost:7687")
    # read parameters
    allParameters = readJson(parameter)
    dataType = readJson(datatype)
    # replace the date in bi1 to datetime
    # allParameters['bi1'] = {'datetime':allParameters['bi1']['date']}
    def convert(k,v):
        if dataType[k] in ["ID", "LONG"]:
            return int(v)
        if dataType[k] == "DATE":
            return convert_to_date(v)
        if dataType[k] == "DATETIME":
            return convert_to_datetime(v)
        if dataType[k] == "STRING":
            return v
        if dataType[k] == "STRING[]":
            return v.split(';')
        else:
            raise Exception(f'Error: Datatype of {k}:{v} is unknown.')
    for query, parameters in allParameters.items():
        allParameters[query] = {k: convert(k,v) for k, v in parameters.items()}

    # run queries
    print('Queries:'+','.join(queries))
    print('BI\tNrows\ttime')
    durations = []
    with driver.session() as session:
        for query_variant in queries: #["1", "2", "3", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"]:
            query_num = re.sub("[^0-9]", "", query_variant)
            query_file = open(f'queries/bi-{query_variant}.cypher', 'r')
            query_spec = query_file.read()
            result, duration = run_query(session, query_variant, query_spec, allParameters['bi'+str(query_variant)])
            writeResult(result, output / f'bi{query_variant}.txt')
            print(f'{query_variant}\t{len(result)}\t{duration:1.2f}')
            durations.append(duration)
            #with open(time_dir / f'bi{query_id}.txt', 'w') as f:
            #    f.write(str(duration))
    all_duration = [0]*20
    for q,d in zip(queries,durations):
        all_duration[int(q)-1] = d
    driver.close()
    return all_duration

if __name__ == '__main__':
    args = parser.parse_args()
    queries = parse_queries(args.queries)
    eval(queries, args.parameter, args.datatype, args.output)

