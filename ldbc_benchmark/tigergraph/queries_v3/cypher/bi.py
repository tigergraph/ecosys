#!/usr/bin/env python3
import argparse
from neo4j import GraphDatabase, time
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
parser.add_argument('-r','--results', default=Path('results'), type=Path,
    help='directory to write results (default: ./results)')

args = parser.parse_args()

result_dir = Path(args.results) 
result_dir.mkdir(parents=True, exist_ok=True)
#time_dir = Path('elapsed_time')
#time_dir.mkdir(parents=True, exist_ok=True)

#@unit_of_work(timeout=300)
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
    print(f'{query_id}\t{len(result)}\t{duration:1.2f}')
    writeResult(result, result_dir / f'bi{query_id}.txt')
    #with open(time_dir / f'bi{query_id}.txt', 'w') as f:
    #    f.write(str(duration))
    
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

def main():
    if args.queries == 'all' or '':
        actual = {str(i) for i in range(1,21)}
    elif args.queries.startswith('not:'):
        all = {str(i) for i in range(1,21)}
        exclude = set(args.queries[4:].split(','))
        actual = all - exclude
    elif args.queries.startswith('reg:'):
        r = re.compile(args.queries[4:])
        actual = list(filter(r.match, [str(i) for i in range(20)]))
    else:
        actual = args.queries.split(',')
    queries = [str(i) for i in range(1,21) if str(i) in actual]
    print('Queries:'+','.join(queries))
    print('BI\tNrows\ttime')
    driver = GraphDatabase.driver("bolt://localhost:7687")

    # read parameters
    allParameters = readJson('../parameters/sf1.json')
    dataType = readJson('../parameters/dataType.json')
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

    with driver.session() as session:
        for query_variant in queries: #["1", "2", "3", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"]:
            query_num = re.sub("[^0-9]", "", query_variant)
            query_file = open(f'queries/bi-{query_variant}.cypher', 'r')
            query_spec = query_file.read()
            run_query(session, query_variant, query_spec, allParameters['bi'+str(query_variant)])
                

    driver.close()

if __name__ == '__main__':
    main()

