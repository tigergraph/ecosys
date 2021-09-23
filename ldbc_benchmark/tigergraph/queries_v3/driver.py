#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
import time
from timeit import default_timer as timer
import ast
import requests
import re
from datetime import datetime, timedelta
from random import randrange, choice
from glob import glob


def get_parser():
    query_help = 'Query numbers (default:all). A list split by comma e.g. "1,2", or "not:bi3,bi4" to exclude some queries, or "reg:[1-4]" to use a regular expression'	
    machine_dir_help = 'The machine (default:ANY) and directory to load data from, e.g. "/home/tigergraph/data" or "ALL:/home/tigergraph/data"'
    parameter_help = '"auto" to find parameter in the output folder and generate one if not exist. Or a parameter file path (default:auto).'
    suffix_help = 'suffix of the file (default: is none and read directories)'
    # The top-level parser.
    main_parser = argparse.ArgumentParser(description='Various utilities working with GSQL for LDBC SNB.')
    main_parser.set_defaults(func=lambda _: main_parser.print_usage())
    main_subparsers = main_parser.add_subparsers(dest='cmd')
    # ./driver.py load [schema/query/data/all] 
    load_parser = main_subparsers.add_parser('load', help='Load the schema, queries, or data')
    load_parser.set_defaults(func=lambda _: load_parser.print_usage())
    load_subparsers = load_parser.add_subparsers(dest='cmd_load')

    load_schema_parser = load_subparsers.add_parser('schema', help='Load the schema')
    load_schema_parser.set_defaults(func=cmd_load_schema)

    load_query_parser = load_subparsers.add_parser('query', help='Install queries')
    load_query_parser.set_defaults(func=cmd_load_query)

    load_data_parser = load_subparsers.add_parser('data', help='Load data')
    load_data_parser.set_defaults(func=cmd_load_data)

    load_all_parser = load_subparsers.add_parser('all', help='Load the schema, queries, and data')
    load_all_parser.set_defaults(func=cmd_load_all)

    # ./driver.py run [-p parameter] [-q queries]
    run_parser = main_subparsers.add_parser('run', help='Run the workloads')
    run_parser.set_defaults(func=cmd_run)
    # ./driver refresh [machine:dir]
    refresh_parser = main_subparsers.add_parser('refresh', help='insert and delete data')
    refresh_parser.set_defaults(func=cmd_refresh)
    # ./driver.py compare [-q queries]
    
    # ./driver all [machine:dir]
    all_parser = main_subparsers.add_parser('all', help='Do all of the above.')
    all_parser.set_defaults(func=cmd_all)
    
    for parser in [load_data_parser, load_all_parser, refresh_parser, all_parser]:
        parser.add_argument('machine_dir', type=str, help=machine_dir_help)
        parser.add_argument('--suffix', type=str, default='', help=suffix_help)
        parser.add_argument('--header', action='store_true', help='whether data has the header')


    for parser in [load_query_parser, load_all_parser]:
        parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
    
    for parser in [run_parser, refresh_parser, all_parser]:
        parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
        parser.add_argument('-n', '--nruns', type=int, default=1, help='number of runs')
        parser.add_argument('-o','--output', default=Path('results'), type=Path, help='directory to write results (default: results)')
        parser.add_argument('-v', '--verbose', action='store_true', help='print for every query')
        parser.add_argument('-s', '--sleep', type=float, default=0, help='factor times elapsed time to sleep between runs')
        parser.add_argument('-p', '--parameter', type=str, default='auto', help=parameter_help)

    for parser in [refresh_parser, all_parser]:
        parser.add_argument('-b','--begin', type=str, default='2012-09-13', help='begin date (inclusive)')
        parser.add_argument('-e','--end', type=str, default='2012-12-31', help='end date (exclusive))')
        parser.add_argument('-r', '--read_freq', type=int, default=30, help='read frequency in days')
        
    compare_parser = main_subparsers.add_parser('compare', help='Compare the results')
    compare_parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
    compare_parser.add_argument('-s', '--source', type=Path, default=Path('results'), help='direcotry of the source results (default: results)')
    compare_parser.add_argument('-t', '--target', type=Path, default=Path('cypher/results'), help='direcotry of the target results (default: cypher/results)')
    compare_parser.set_defaults(func=cmd_compare)
    
    gen_parser = main_subparsers.add_parser('gen_para', help='auto generate parameter')
    gen_parser.set_defaults(func=cmd_gen)
    gen_parser.add_argument('-o', '--output', type=Path, default=Path('param.json'), help='output parameter file path')
    gen_parser.add_argument('-v', '--verbose', action='store_true', help='print for every query')
    
    # ./driver stat
    stat_parser = main_subparsers.add_parser('stat', help='print statistics of the graph')        
    stat_parser.set_defaults(func=cmd_stat)
    stat_parser = main_subparsers.add_parser('quick_stat', help='print statistics of the graph')        
    stat_parser.set_defaults(func=quick_stat)
    return main_parser

class ResultWithElapsed:
    def __init__(self, result, elapsed):
        self.result = result
        self.elapsed = elapsed

class Query:
    ENDPOINT = 'http://127.0.0.1:9000/query/ldbc_snb/'
    HEADERS = {'GSQL-TIMEOUT': '36000000'}
    def __init__(self, name, transform_parameter=None):
        self.name = name
        self.transform_parameter = transform_parameter
    def run(self, parameter):
        if self.transform_parameter is not None:
            parameter = self.transform_parameter(parameter)
        start = timer()
        response = requests.get(self.ENDPOINT + self.name, headers=self.HEADERS, params=parameter).json()
        end = timer()

        if response['error']:
            raise Exception(str(response['message']))

        return ResultWithElapsed(response['results'], end - start)

class Workload:
    def __init__(self, name, transform, queries = None):
        self.name = name
        if not queries:
            self.queries = [Query(self.name)]
        else:
            self.queries = queries
        self.transform_result = transform

    def run(self, parameter):
        results = [query.run(parameter) for query in self.queries]
        result = self.transform_result([r.result for r in results])
        elapsed = sum(r.elapsed for r in results)
        return ResultWithElapsed(result, elapsed)

class ResultTransform:
    def __init__(self):
        self.transforms = []

    def __call__(self, results):
        for transform in self.transforms:
            results = transform(results)
        return results

    def __getitem__(self, key):
        def transform_result(results):
            return results[key]
        self.transforms.append(transform_result)
        return self

    def change_key(self, key_map):
        def transform_result(results):
            return {key_map.get(k, k): v for k, v in results.items()}
        self.transforms.append(transform_result)
        return self

    def del_keys(self, keys):
        def transform_result(results):
            return [
                {k: v for k, v in result.items() if k not in keys}
                for result in results
            ]
        self.transforms.append(transform_result)
        return self

    def change_keys(self, key_map):
        def transform_result(results):
            return [
                {key_map.get(k, k): v for k, v in result.items()}
                for result in results
            ]
        self.transforms.append(transform_result)
        return self

STATIC_NAMES = [
    'Organisation',
    'Organisation_isLocatedIn_Place',
    'Place',
    'Place_isPartOf_Place',
    'Tag',
    'TagClass',
    'TagClass_isSubclassOf_TagClass',
    'Tag_hasType_TagClass',
]
DYNAMIC_VERTICES = [
    'Comment',
    'Forum',
    'Person',
    'Post',
]
DYNAMIC_EDGES = [
    'Comment_hasCreator_Person',
    'Comment_hasTag_Tag',
    'Comment_isLocatedIn_Country',
    'Comment_replyOf_Comment',
    'Comment_replyOf_Post',
    'Forum_containerOf_Post',
    'Forum_hasMember_Person',
    'Forum_hasModerator_Person',
    'Forum_hasTag_Tag',
    'Person_hasInterest_Tag',
    'Person_isLocatedIn_City',
    'Person_knows_Person',
    'Person_likes_Comment',
    'Person_likes_Post',
    'Person_studyAt_University',
    'Person_workAt_Company',
    'Post_hasCreator_Person',
    'Post_hasTag_Tag',
    'Post_isLocatedIn_Country',
]
DYNAMIC_NAMES = DYNAMIC_VERTICES + DYNAMIC_EDGES

SCRIPT_DIR_PATH = Path(__file__).resolve().parent
total = 21
WORKLOADS = [Workload(f'bi{i}', ResultTransform()[0][0]['@@result']) for i in range(1,total)]
WORKLOADS[7] = Workload('bi8', ResultTransform()[0][0]['@@result'].del_keys(['totalScore']))
WORKLOADS[9] = Workload('bi10', ResultTransform()[0][0]['result'])
WORKLOADS[10] = Workload('bi11', ResultTransform()[0][0])
WORKLOADS[15] = Workload('bi16', ResultTransform()[0][0]['@@result'].del_keys(['totalMessageCount']))
WORKLOADS[18] = Workload('bi19', ResultTransform()[1][0]['@@result'], queries = [Query('bi19_add_weighted_edges'), Query('bi19'), Query('bi19_delete_weighted_edges')])

DEL_VERTICES = ["Comment","Post","Forum","Person"]
DEL_EDGES = ['Person_likes_Post',
    'Person_likes_Comment',
    'Forum_hasMember_Person',
    'Person_knows_Person',
]
DEL_WORKLOADS = [Workload(f'del_{v}', ResultTransform()[0][0]) for v in DEL_VERTICES]
STAT_WORKLOADS = [
    Workload('quick_stat', ResultTransform()[0][0]), 
    Workload('stat', ResultTransform()[0][0])]
GEN_WORKLOADS = {
    0 : Workload('gen', ResultTransform()[0][0]),
    10: Workload('gen_bi10', ResultTransform()[0][0]),
    15: Workload('gen_bi15', ResultTransform()[0][0]),
    16: Workload('gen_bi16', ResultTransform()[0][0]),
    18: Workload('gen_bi18', ResultTransform()[0][0]),
    19: Workload('gen_bi19', ResultTransform()[0][0]),
    20: Workload('gen_bi20', ResultTransform()[0][0]),
}

'''Loads parameters for a given file'''
def load_parameters(file):
    with open(file) as f: 
        txt = f.read().replace('\n','')
        return ast.literal_eval(txt)

def cmd_load_schema(args):
    '''Loads the schema.'''
    subprocess.run(f"gsql {str(SCRIPT_DIR_PATH/'schema.gsql')}", shell=True)
    subprocess.run(f"gsql {str(SCRIPT_DIR_PATH/'load.gsql')}", shell=True)
    subprocess.run(f"gsql {str(SCRIPT_DIR_PATH/'dml'/'insert.gsql')}", shell=True)
    subprocess.run(f"gsql {str(SCRIPT_DIR_PATH/'dml'/'del_Edge.gsql')}", shell=True)
    
"""
Load data
    - job: load_static, load_dynamic, delete_dynamic
    - machine : machine spec
    - data_dir: file path
    - tag : dynamic or static
    - names: STATIC_NAMES DYNAMIC_NAMES DEL_EDGES
    - suffix : str
    - date : None
""" 
def load_data(job, machine, data_dir, tag, names, suffix, date = None):
    file_paths = [(data_dir /tag / name) for name in names]
    if date:
        file_paths = [ f/date.strftime('batch_id=%Y-%m-%d') for f in file_paths ]
    gsql = f'RUN LOADING JOB {job} USING '
    if suffix: suffix = '.' + suffix
    gsql += ', '.join(f'file_{name}="{machine}{file_path}{suffix}"' for name, file_path in zip(names, file_paths))
    subprocess.run(f'gsql -g ldbc_snb \'{gsql}\'', shell=True)

def cmd_load_data(args):
    '''Loads data from the given data_dir path.'''
    for f in args.data_dir.glob('**/_SUCCESS'):
        f.unlink()
    t0 = timer()
    header = '_with_header' if args.header else ''
    load_data('load_static' + header, args.machine, args.data_dir/'initial_snapshot', 'static', STATIC_NAMES, args.suffix)
    load_data('load_dynamic' + header, args.machine, args.data_dir/'initial_snapshot', 'dynamic', DYNAMIC_VERTICES+DYNAMIC_EDGES, args.suffix)
    t1 = timer()
    print(f'loading time is {t1-t0}')

def cmd_load_query(args):
    '''copy user defined function to tigergraph path'''
    udf_file = SCRIPT_DIR_PATH/'ExprFunctions.hpp'
    udf_str = f'"{str(udf_file)}"'
    subprocess.run(f"gsql 'PUT ExprFunctions FROM {udf_str}'", shell=True)
    '''Loads queries from the given workloads.'''
    for workload in args.workload:
        workload_path = (SCRIPT_DIR_PATH / 'queries' / f'{workload.name}.gsql').resolve()
        subprocess.run(f"gsql -g ldbc_snb {workload_path}", shell=True)

    for vertex in DEL_VERTICES:
        del_query = (SCRIPT_DIR_PATH / 'dml' / f'del_{vertex}.gsql').resolve()
        subprocess.run(f"gsql -g ldbc_snb {del_query}", shell=True)
    # stat.gsql to check the number of vertices and edges
    stat_query = (SCRIPT_DIR_PATH / 'stat.gsql').resolve()
    subprocess.run(f"gsql -g ldbc_snb {stat_query}", shell=True)
    gen_query = (SCRIPT_DIR_PATH / 'parameters'/'gen_para.gsql').resolve()
    subprocess.run(f"gsql -g ldbc_snb {gen_query}", shell=True)
    
    all_workloads = args.workload + DEL_WORKLOADS + STAT_WORKLOADS + list(GEN_WORKLOADS.values())
    queries_to_install = [
        query.name 
        for workload in all_workloads 
        for query in workload.queries]
    gsql =  ", ".join(queries_to_install)
    subprocess.run(f"gsql -g ldbc_snb 'INSTALL QUERY {gsql}'", shell=True)

def cmd_load_all(args):
    '''Loads the schema, data and queries.'''
    cmd_load_schema(args)
    cmd_load_data(args)
    cmd_load_query(args)
"""
    Print graph satistics
"""
def quick_stat():
    t0 = timer()
    stat = STAT_WORKLOADS[0].run(None).result
    t1 = timer()
    print(stat)
    return list(stat.values())

def cmd_stat(args):
    # this command flushes delta records in memory to disk. Without this command, outdegree function can return a lagged value. 
    print('update delta ...')
    subprocess.run('curl -s -H "GSQL-TIMEOUT: 2500000" "http://127.0.0.1:9000/rebuildnow"', shell=True)
    print()
    print('done update delta ...')
    t0 = timer()
    stat = STAT_WORKLOADS[1].run(None).result
    t1 = timer()
    stat2 = {}
    for k,v in stat.items():
        if k.startswith('@@'): stat2[k[2:]] = v
        else: stat2[k] = v
    print(f'stats({t1-t0:.2f}):', stat2)
    return stat2


    
"""
generate parameters automatically
"""
def cmd_gen(args, output=None):
    if not output:
        output = args.output
    parameters = load_parameters(Path('parameters/sf1.json'))
    dataType = load_parameters(Path('parameters/dataType.json'))
    gen = {}
    for i, workload in GEN_WORKLOADS.items():
        t0 = timer()
        gen[i] = workload.run(None).result
        t1 = timer()
        if args.verbose: print(f'done {workload.name} in {t1-t0:.2f} s')
        
    while gen[10]['personId']==0:
        gen[10] = GEN_WORKLOADS[10].run(None).result
        print('rerun gen_bi10')
    while gen[15]['person2Id']==0:
        gen[15] = GEN_WORKLOADS[15].run(None).result
        print('rerun gen_bi15')
    while len(gen[19]['@@cityIds'])==0:
        gen[19] = GEN_WORKLOADS[19].run(None).result
        print('rerun gen_bi19')
    while len(gen[20]['@@person2Ids'])==0:
        gen[20] = GEN_WORKLOADS[20].run(None).result
        print('rerun gen_bi20')
    country = gen[0]['@@country']
    tag = gen[0]['@@tag']
    tagclass = gen[0]['@@tagclass']
    def genValue(name, Dtype):
        if name == 'country': return country[randrange(3)]
        if name == 'tag': return tag[randrange(3)]
        if name == 'tagClass': return tagclass[randrange(3)]
        if name == 'startDate': 
            date = datetime(2010, 9, 1) + timedelta(days=randrange(365))
            return date.strftime("%Y-%m-%dT00:00:00")
        if Dtype == 'LONG':
            return randrange(100)
        if Dtype == 'DATETIME':
            date = datetime(2010, 9, 1) + timedelta(days=randrange(2*365))
            return date.strftime("%Y-%m-%dT00:00:00")
    for q, para in parameters.items():
        for name,_ in para.items():
            if name in ["minPathDistance", "maxPathDistance","lengthThreshold", "languages","maxKnowsLimit"]:
                continue
            parameters[q][name] = genValue(name, dataType[name])
    
    date = datetime.strptime(parameters['bi9']['startDate'], "%Y-%m-%dT%H:%M:%S") + timedelta(days=5) 
    parameters['bi9']['endDate'] = date.strftime("%Y-%m-%dT00:00:00")
    date = datetime.strptime(parameters['bi15']['startDate'], "%Y-%m-%dT%H:%M:%S") + timedelta(days=365) 
    
    for k in gen[10].keys(): parameters['bi10'][k] = gen[10][k]
    parameters['bi1']['datetime'] = (datetime(2010,2,1) + timedelta(days=randrange(200))).strftime("%Y-%m-%dT00:00:00")
    parameters['bi4']['date'] = (datetime(2012,3,1) + timedelta(days=randrange(200))).strftime("%Y-%m-%dT00:00:00")
    
    i = randrange(3)
    # bi 14 is too expensive, we choose two country manually
    parameters['bi14']['country1'] = 'Singapore' #country[i]
    parameters['bi14']['country2'] =  'Morocco' #country[(i+1)%3]
    parameters['bi15']['endDate'] = date.strftime("%Y-%m-%dT00:00:00")
    for k in gen[15].keys(): parameters['bi15'][k] = gen[15][k]
    for k in gen[16].keys(): parameters['bi16'][k] = gen[16][k]
    parameters['bi16']['dateA'] = gen[16]['dateA'].replace(' ','T')
    parameters['bi16']['dateB'] = gen[16]['dateB'].replace(' ','T')
    parameters['bi17']['delta'] = randrange(12,16)
    parameters['bi18'] = gen[18]
    parameters['bi19']['city1Id'] = gen[19]['@@cityIds'][0]
    parameters['bi19']['city2Id'] = gen[19]['@@cityIds'][1]
    parameters['bi20']['company'] = gen[20]['company']
    parameters['bi20']['person2Id'] = choice(gen[20]['@@person2Ids'])
    
    stream = str(parameters)
    stream = re.sub(r"'bi", r"\n'bi", stream)
    with open(output,'w') as f:
        f.write(stream)
    print(f'Generating parameter to {str(output)}')

def median(time_list):
    return sorted(time_list)[len(time_list)//2]

def writeResult(result, filename):
    with open(filename, 'w') as f:
        if isinstance(result, dict): 
            f.write(str(list(result.values()))+'\n')
            return  
        for row in result:
            row = [v for k,v in row.items()]
            f.write(str(row)+'\n')

"""
return 
    alltime -  list of length 20 storing the running time 
"""        
def cmd_run(args, output = None):
    if not output:
        output = args.output
    os.makedirs(output, exist_ok=True)

    #os.makedirs('elapsed_time',exist_ok=True)
    print(f"run for {args.nruns} times ...")
    print("Q\tNrow\tMedian time\t")
    alltime = []
    if args.parameter == 'auto':
        parameter = output/'param.json'
        if not parameter.exists():
            t0 = timer()
            cmd_gen(args, parameter)
            t1 = timer()
            alltime.append(t1-t0)
            print(f'param_gen\t20\t{t1-t0:.2f}')
            if args.sleep: time.sleep((t1-t0)*args.sleep)
        else:
            alltime.append(0)    
    else:
        parameter = Path(args.parameter).resolve()
        alltime.append(0)
    parameters = load_parameters(parameter)
    parameters['bi1'] = {'date':parameters['bi1']['datetime']}
    

    for workload in args.workload:
        time_list = []
        result = workload.run(parameters[workload.name])
        time_list.append(result.elapsed)    
        if args.sleep: time.sleep(result.elapsed * args.sleep)
        # write the query results to log/bi[1-20]
        result_file = output/ (workload.name+'.txt')  
        writeResult(result.result, result_file)
        #with open(Path('elapsed_time')/ (workload.name+'.txt'), 'w') as f:
        #    f.write(str(result.elapsed))
        
        for i in range(args.nruns-1):
            result = workload.run(parameters[workload.name])
            time_list.append(result.elapsed)    
            if args.sleep: time.sleep(result.elapsed * args.sleep)
            #with open(Path('elapsed_time')/ (workload.name+'.txt'), 'a') as f:
            #    f.write(','+str(result.elapsed))
        median_time = median(time_list)
        alltime.append(median_time)
        print(f'{workload.name}\t{len(result.result)}\t{median_time:.2f}')
    print(f'Results are written to {str(output)}')
    return alltime 

def cmd_compare(args):
    source = args.source
    target = args.target
    compare_top = True
    for datePath in source.iterdir():
        if not datePath.is_dir(): continue
        print(f"Comparing for {datePath.name}")
        compare_batch(args.workload, source/datePath.name, target/datePath.name)
        compare_top = False
    if compare_top: compare_batch(args.workload, source, target)
        
def compare_batch(workloads, source, target):
    for workload in workloads:
        Pass = True
        file1 = source/(workload.name+'.txt')
        file2 = target/(workload.name+'.txt')
        if not file1.exists():
            print(f'{workload.name}: file {str(file1)} does not exist') 
            continue
        if not file2.exists():
            print(f'{workload.name}: file {str(file2)} does not exist') 
            continue        
        # compare number of lines
        if len(open(file1).readlines()) != len(open(file2).readlines()): 
            print(f'{workload.name}: number of lines is different') 
            continue

        with open(file1, 'r') as f1, open(file2, 'r') as f2:
            for i, (row1,row2) in enumerate(zip(f1,f2)):
                row1 = ast.literal_eval(row1) 
                row2 = ast.literal_eval(row2)
                if len(row1) != len(row2):
                    print(f'{workload.name} line {i}: number of columns are different {len(row1)} != {len(row2)}')
                    Pass = False

                for j, (col1,col2) in enumerate(zip(row1,row2)):
                    if (isinstance(col1, float) and abs(col1-col2)<1e-3):
                        continue
                    if isinstance(col1,str) and re.match("\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}",col1):
                        t1 = datetime.strptime(col1, "%Y-%m-%d %H:%M:%S")
                        t2 = datetime.strptime(col2, "%Y-%m-%d %H:%M:%S")
                        if abs((t1-t2).total_seconds()) < 2:
                            continue
                    if col1!=col2:
                        print(f'{workload.name} line {i} column {j}: {col1} != {col2}')
                        Pass = False
                        break
                                        
                if not Pass: 
                    break
            if Pass: 
                print(f'{workload.name}: Pass')

""" 
refresh data and run queries
"""
def toStr(x_list):
    if isinstance(x_list[0], int) or isinstance(x_list[0], str):
        return ','.join([f'{x}' for x in x_list])
    if isinstance(x_list[0], float):
        return ','.join([f'{x:.2f}' for x in x_list])

def cmd_refresh(args):
    os.makedirs(args.output, exist_ok=True)
    timelog = args.output/'timelog.csv'
    
    stat_name = ['nComment', 'nPost', 'nForum', 'nPerson', 'HAS_TAG', 'LIKES', 'KNOWS', 'REPLY_OF']
    begin = datetime.strptime(args.begin, '%Y-%m-%d')
    end = datetime.strptime(args.end, '%Y-%m-%d')    
    delta = timedelta(days=1)
    date = begin 
    tot_ins_time = 0
    tot_del_time = 0
    dateStr = date.strftime('%Y-%m-%d')
    output = args.output/dateStr

    logf = open(timelog, 'w')
    cols = ['date'] + stat_name + ['ins','del','gen'] + [f'bi{i}' for i in range(1,21)]
    logf.write(','.join(cols)+'\n')
    stat_dict = cmd_stat(args)
    batch_log = f'{dateStr},' + toStr([stat_dict[n] for n in stat_name]) 
    batch_log += ',' + toStr([tot_ins_time, tot_del_time])
    header = '_with_header' if args.header else ''
    if args.read_freq > 0: 
        # run query 
        durations = cmd_run(args, output = output)
        batch_log += ',' + toStr(durations)
    logf.write(batch_log+'\n')
    logf.flush()
    while date < end:
        print('======== insertion for ' + date.strftime('%Y-%m-%d') + '========')
        t0 = timer()
        load_data('insert_vertex' + header, args.machine, args.data_dir/'inserts', 'dynamic', DYNAMIC_VERTICES, args.suffix, date)
        load_data('insert_edge' + header, args.machine, args.data_dir/'inserts', 'dynamic', DYNAMIC_EDGES, args.suffix, date)
        t1 = timer()
        tot_ins_time += t1-t0
        if args.verbose: logf.write('insert,' + toStr(quick_stat())+ f',{t1-t0}\n')
        
        print('======== deletion for ' + date.strftime('%Y-%m-%d') + '========')
        for vertex,workload in zip(DEL_VERTICES, DEL_WORKLOADS):
            t0 = timer()
            path = args.data_dir/'deletes'/'dynamic'/vertex/date.strftime('batch_id=%Y-%m-%d') 
            if not path.exists(): continue
            for fp in path.glob('*.csv'):
                if fp.is_file(): 
                    result = workload.run({'file':str(fp)})
                    print(f'Deleting {vertex}: {result.result}')
            t1 = timer()
            tot_del_time += t1 - t0
            if args.verbose: logf.write(f'{vertex},' + toStr(quick_stat())+ f',{t1-t0}\n')
        t0 = timer()
        load_data('delete_edge' + header, args.machine, args.data_dir/'deletes', 'dynamic', DEL_EDGES, args.suffix, date)
        t1 = timer()
        tot_del_time += t1 - t0
        if args.verbose: logf.write('delete_edge,' + toStr(quick_stat())+ f',{t1-t0}\n')
        
        date += delta    
        dateStr = date.strftime('%Y-%m-%d')
        output = args.output/dateStr
            
        # run query 
        if args.read_freq and (date - begin).days % args.read_freq == 0: 
            stat_dict = cmd_stat(args)
            batch_log = f'{dateStr},' + toStr([stat_dict[n] for n in stat_name]) 
            batch_log += ',' + toStr([tot_ins_time, tot_del_time])
            batch_log += ',' + toStr(cmd_run(args, output = output))
        else:
            batch_log = f'{dateStr},' + toStr(quick_stat()) 
            batch_log += ',' + toStr([tot_ins_time, tot_del_time])
        
        logf.write(batch_log+'\n')
        logf.flush()
        if args.read_freq == 0 or (date - begin).days % args.read_freq == 0:
            tot_ins_time = 0
            tot_del_time = 0
        
    logf.close()

def cmd_all(args):
    cmd_load_all(args)
    cmd_refresh(args)

""" 
Parse args.queries to args.workload
"""
def parse_queries(args):
    if not hasattr(args, 'queries'): 
        args.workload = WORKLOADS
        return
    all = [str(i) for i in range(1,total)]
    alls = set(all)
    if args.queries == 'all':
        actual = alls
    # exclude cases
    elif args.queries.startswith('not:'):
        exclude = args.queries[4:].split(',')
        actual = alls - set(exclude)
    # regular expression
    elif args.queries.startswith('reg:'):
        r = re.compile(args.queries[4:])
        actual = list(filter(r.match, all))
    else:
        actual = set(args.queries.split(','))
    args.workload = [WORKLOADS[i-1] for i in range(1,total) if str(i) in actual]
    print('Workload:', ','.join([w.name for w in args.workload]))

""" 
Parse args.machine_dir to args.machine and args.data_dir
"""
def parse_machine_dir(args):
    if not hasattr(args, 'machine_dir'): return
    if ':' in args.machine_dir:
        mds = args.machine_dir.split(':')
        if len(mds) != 2:
            raise Exception("<data_dir> should be in format: 'machine:data_dir'")
        args.machine =  mds[0] + ':'
        args.data_dir = Path(mds[1]).expanduser()
    else:
        args.machine, args.data_dir = 'ANY:', Path(args.machine_dir).expanduser()
    if args.cmd == 'refresh' or not args.machine in ['m1', 'ALL']: return
    #check if path exists
    missing = []
    check_list = []
    for tag in ['static', 'dynamic']:
        names = STATIC_NAMES if tag == 'static' else DYNAMIC_NAMES
        for name in names:
            file_path = (args.data_dir/'initial_snapshot'/tag/name)
            if not args.suffix and not file_path.is_dir():
                missing.append(file_path.name)
            if args.suffix and not file_path.is_file():
                missing.append(file_path.name)
    if len(missing) > 0:
        tmp = 'files' if args.suffix else 'directory'
        raise ValueError(f'The directory "{args.data_dir}" is missing {tmp} {", ".join(missing)}.')

def check_args(args):
    parse_queries(args)    
    parse_machine_dir(args)

def main():
    args = get_parser().parse_args()
    check_args(args)
    args.func(args)


if __name__ == '__main__':
    main()