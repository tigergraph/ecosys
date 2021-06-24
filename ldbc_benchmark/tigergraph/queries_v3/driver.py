#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from timeit import default_timer as timer
import ast
import requests
import re
from datetime import datetime, timedelta
from random import randrange

default_parameter = Path('parameters/sf1/initial.json')

def get_parser():
    query_help = 'Query numbers (default:"all"). A list split by comma e.g. "1,2", or "not:bi3,bi4" to exclude some queries, or "reg:[1-4]" to use a regular expression'	
    machine_dir_help = 'The machine (optional) and directory to load data from, e.g. "/home/tigergraph/data" or "ALL:/home/tigergraph/data"'
    parameter_help = 'Parameter file in json (default:parameters/sf1/2012-09-13.json).'
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

    for parser in [load_data_parser, load_all_parser]:
        parser.add_argument('machine_dir', type=str, help=machine_dir_help)
        parser.add_argument('-s','--suffix', type=str, default='', help=suffix_help)

    for parser in [load_query_parser, load_all_parser]:
        parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
    
    # ./driver.py run [-p parameter] [-q queries]
    run_parser = main_subparsers.add_parser('run', help='Run the workloads')
    run_parser.add_argument('-p', '--parameter', type=Path, default=default_parameter, help='Parameter file in json.')
    run_parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
    run_parser.add_argument('-n', '--nruns', type=int, default=1, help='number of runs')
    run_parser.add_argument('-o','--output', default=Path('results'), type=Path, help='directory to write results (default: results)')
    run_parser.set_defaults(func=cmd_run)

    # ./driver.py compare [-q queries]
    compare_parser = main_subparsers.add_parser('compare', help='Compare the results')
    compare_parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
    compare_parser.add_argument('-s', '--source', type=Path, default=Path('results'), help='direcotry of the source results (default: results)')
    compare_parser.add_argument('-t', '--target', type=Path, default=Path('cypher/results'), help='direcotry of the target results (default: cypher/results)')
    compare_parser.set_defaults(func=cmd_compare)
    
    # ./driver refresh [machine:dir]
    refresh_parser = main_subparsers.add_parser('refresh', help='insert and delete data')
    refresh_parser.set_defaults(func=cmd_refresh)
    all_parser = main_subparsers.add_parser('all', help='Do all of the above.')
    all_parser.set_defaults(func=cmd_all)
    for parser in [refresh_parser, all_parser]:
        parser.add_argument('machine_dir', type=str, help=machine_dir_help)
        #parser.add_argument('-i', '--insert', action='store_false', help='turn off inserts')
        #parser.add_argument('-d', '--delete', action='store_false', help='turn off delete')
        parser.add_argument('-s','--suffix', type=str, default='', help=suffix_help)
        parser.add_argument('-b','--begin', type=str, default='2012-09-14', help='begin date (inclusive)')
        parser.add_argument('-e','--end', type=str, default='2012-10-20', help='end date (exclusive))')
        parser.add_argument('-p', '--parameter', type=str, default='auto', help=parameter_help)
        parser.add_argument('-q', '--queries', type=str, default='all', help=query_help)
        parser.add_argument('-r', '--read_freq', type=int, default=1, help='read frequency in days')
        parser.add_argument('-n', '--nruns', type=int, default=1, help='number of runs')    
        parser.add_argument('-o', '--output', type=Path, default=Path('result'), help='output folder')
    
    # ./driver gen_para [machine:dir]
    gen_parser = main_subparsers.add_parser('gen_para', help='auto generate parameter')
    gen_parser.set_defaults(func=cmd_gen)
    gen_parser.add_argument('-o', '--output', type=Path, default=Path('param.json'), help='output parameter file path')
    # Running all from loading to running.
    return main_parser

def list2str(xs):
    return ','.join([str(x) for x in xs])

class ResultWithElapsed:
    def __init__(self, result, elapsed):
        self.result = result
        self.elapsed = elapsed

class Query:

    ENDPOINT = 'http://127.0.0.1:9000/query/ldbc_snb/'
    HEADERS = {'GSQL-TIMEOUT': '7200000'}

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
    def __init__(self, name, queries, transform_result):
        self.name = name
        self.queries = queries
        self.transform_result = transform_result

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

DYNAMIC_NAMES = [
    'Comment',
    'Comment_hasCreator_Person',
    'Comment_hasTag_Tag',
    'Comment_isLocatedIn_Country',
    'Comment_replyOf_Comment',
    'Comment_replyOf_Post',
    'Forum',
    'Forum_containerOf_Post',
    'Forum_hasMember_Person',
    'Forum_hasModerator_Person',
    'Forum_hasTag_Tag',
    'Person',
    'Person_hasInterest_Tag',
    'Person_isLocatedIn_City',
    'Person_knows_Person',
    'Person_likes_Comment',
    'Person_likes_Post',
    'Person_studyAt_University',
    'Person_workAt_Company',
    'Post',
    'Post_hasCreator_Person',
    'Post_hasTag_Tag',
    'Post_isLocatedIn_Country',
]
DEL_NAMES = [
    'Person_likes_Post',
    'Person_likes_Comment',
    'Forum_hasMember_Person',
    'Person_knows_Person',
]

SCRIPT_DIR_PATH = Path(__file__).resolve().parent
total = 21
WORKLOADS = [Workload(f'bi{i}', [Query(f'bi{i}')], ResultTransform()[0][0]['@@result']) for i in range(1,total)]
WORKLOADS[7] = Workload('bi8', [Query('bi8')], ResultTransform()[0][0]['@@result'].del_keys(['totalScore']))
WORKLOADS[9] = Workload('bi10', [Query('bi10')], ResultTransform()[0][0]['result'])
WORKLOADS[10] = Workload('bi11', [Query('bi11')], ResultTransform()[0][0])
WORKLOADS[15] = Workload('bi16', [Query('bi16')], ResultTransform()[0][0]['@@result'].del_keys(['totalMessageCount']))


DEL_JOBS = {1:"Person",4:"Forum",6:"Post",7:"Comment"}
DEL_WORKLOADS = {i:Workload(f'del{i}', [Query(f'del{i}')], ResultTransform()[0][0]) for i,job in DEL_JOBS.items()}


'''Loads parameters for a given file'''
def load_parameters(file):
    with open(file) as f: 
        txt = f.read().replace('\n','')
        return ast.literal_eval(txt)

def cmd_load_schema(args):
    '''Loads the schema.'''
    subprocess.run(['gsql', str(SCRIPT_DIR_PATH / 'schema.gsql')])
    subprocess.run(['gsql', str(SCRIPT_DIR_PATH / 'load_static.gsql')])
    subprocess.run(['gsql', str(SCRIPT_DIR_PATH / 'insert.gsql')])
    subprocess.run(['gsql', str(SCRIPT_DIR_PATH / 'delete' / 'del_edge.gsql')])
    
"""
Load data
    - job: load_static, load_dynamic, delete_dynamic
    - machine : machine spec
    - data_dir: file path
    - tag : dynamic or static
    - names: STATIC_NAMES DYNAMIC_NAMES DEL_NAMES
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
    subprocess.run(['gsql', '-g', 'ldbc_snb', gsql])
    

def cmd_load_data(args):
    '''Loads data from the given data_dir path.'''
    load_data('load_static', args.machine, args.data_dir/'initial_snapshot', 'static', STATIC_NAMES, args.suffix)
    load_data('load_dynamic', args.machine, args.data_dir/'initial_snapshot', 'dynamic', DYNAMIC_NAMES, args.suffix)

def cmd_load_query(args):
    '''Loads queries from the given workloads.'''
    gsql = ''

    for workload in args.workload:
        workload_path = (SCRIPT_DIR_PATH / 'queries' / f'{workload.name}.gsql').resolve()
        gsql += f'@{workload_path}\n'

    for i in DEL_JOBS.keys():
        del_query = (SCRIPT_DIR_PATH / 'delete' / f'del{i}.gsql').resolve()
        gsql += f'@{del_query}\n'
    # stat.gsql to check the number of vertices and edges
    stat_query = (SCRIPT_DIR_PATH / 'stat.gsql').resolve()
    gsql += f'@{stat_query}\n'
    gen_query = (SCRIPT_DIR_PATH / 'parameter'/'gen_para.gsql').resolve()
    gsql += f'@{gen_query}\n'

    queries_to_install = [
        query.name
        for workload in args.workload
        for query in workload.queries
    ] + [f'del{i}' for i in DEL_JOBS.keys()] \
        + ['stat'] \
        + ['gen'] + [f'gen_bi{i}' for i in [10,15,16,19,20]]
    gsql += f'INSTALL QUERY {", ".join(queries_to_install)}\n'

    subprocess.run(['gsql', '-g', 'ldbc_snb'], input=gsql.encode())

def cmd_load_all(args):
    '''Loads the schema, data and queries.'''
    cmd_load_schema(args)
    cmd_load_data(args)
    cmd_load_query(args)
""" 
refresh data and run queries
"""
def cmd_refresh(args):
    os.makedirs(args.output, exist_ok=True)
    timelog = args.output/'time.log'
    with open(timelog, 'w') as f:
        cols = ['ins','del'] + [f'bi{i}' for i in range(1,21)]
        f.write('\t'.join(cols))
    
    begin = datetime.strptime(args.begin, '%Y-%m-%d')
    end = datetime.strptime(args.end, '%Y-%m-%d')    
    delta = timedelta(days=1)
    date = begin 
    while date < end:
        print('======== insertion for ' + date.strftime('%Y-%m-%d') + '========')
        t0 = timer()
        load_data('load_dynamic', args.machine, args.data_dir/'inserts', 'dynamic', DYNAMIC_NAMES, args.suffix, date)
        t1 = timer()
        print('======== deletion for ' + date.strftime('%Y-%m-%d') + '========')
        for i,job in DEL_JOBS.items():
            path = args.data_dir/'deletes'/'dynamic'/job/date.strftime('batch_id=%Y-%m-%d') 
            if path.exists():
                for fp in path.glob('*.csv'):
                    if fp.is_file(): 
                        result = DEL_WORKLOADS[i].run({'file':str(fp)})
                        print(f'Deleting {job}: {result.result}')
        load_data('delete_edge', args.machine, args.data_dir/'deletes', 'dynamic', DEL_NAMES, args.suffix, date)
        t2 = timer()
        date += delta    
        # if args.read_freq == 0 do not read
        if args.read_freq == 0: continue 
        if (date - begin).days % args.read_freq != 0: continue

        # generate parameter
        path = args.output/date.strftime('%Y-%m-%d')
        parameter = path / 'param.json'
        os.makedirs(path, exist_ok=True)
        if args.parameter == 'auto' and not parameter.exists():
            print('auto generate parameters ...')
            cmd_gen(args, parameter)
        
        # run query 
        times = cmd_run(args, parameter = parameter, output = path, printTime=False)
        
        # write running time
        with open(timelog, 'a') as f:
            time = [str(t) for t in[t1-t0, t2-t1] + times]
            f.write('\t'.join(times))
        
        
"""
generate parameters automatically
"""
def cmd_gen(args, output=None):
    if not output:
        output = args.output
    parameters = load_parameters(Path(default_parameter))
    dataType = load_parameters(Path('parameters/dataType.json'))
    gen_gsql = Workload('gen', [Query('gen')], ResultTransform()[0][0])
    result = gen_gsql.run(None).result
    country = result['@@country']
    tag = result['@@tag']
    tagclass = result['@@tagclass']
    def genValue(name, Dtype):
        if name == 'country': return country[randrange(3)]
        if name == 'country1': return country[0]
        if name == 'country2': return country[1]
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
    
    date = datetime.strptime(parameters['bi9']['startDate'], "%Y-%m-%dT%H:%M:%S") + timedelta(days=9) 
    parameters['bi9']['endDate'] = date.strftime("%Y-%m-%dT00:00:00")
    date = datetime.strptime(parameters['bi15']['startDate'], "%Y-%m-%dT%H:%M:%S") + timedelta(days=365) 
    
    bi10 = Workload('gen_bi10', [Query('gen_bi10')], ResultTransform()[0][0]).run(None).result
    bi15 = Workload('gen_bi15', [Query('gen_bi15')], ResultTransform()[0][0]).run(None).result
    bi16 = Workload('gen_bi16', [Query('gen_bi16')], ResultTransform()[0][0]).run(None).result
    bi19 = Workload('gen_bi19', [Query('gen_bi19')], ResultTransform()[0][0]).run(None).result
    bi20 = Workload('gen_bi20', [Query('gen_bi20')], ResultTransform()[0][0]).run(None).result
    for k in bi10.keys(): parameters['bi10'][k] = bi10[k]
    parameters['bi15']['endDate'] = date.strftime("%Y-%m-%dT00:00:00")
    for k in bi15.keys(): parameters['bi15'][k] = bi15[k]
    for k in bi16.keys(): parameters['bi16'][k] = bi16[k]
    parameters['bi16']['dateA'] = bi16['dateA'].replace(' ','T')
    parameters['bi16']['dateB'] = bi16['dateB'].replace(' ','T')
    parameters['bi17']['delta'] = randrange(8,16)
    parameters['bi17']['tag'] = result['smalltag']
    parameters['bi18']['person1Id'] = bi15['person1Id']
    parameters['bi19']['city1Id'] = bi19['@@cities'][0]
    parameters['bi19']['city2Id'] = bi19['@@cities'][1]
    parameters['bi20']['company'] = bi20['company']
    parameters['bi20']['person2Id'] = bi20['@@person2Ids'][0]
    
    stream = str(parameters).replace("'", '"')
    stream = re.sub(r'"bi', r'\n"bi', stream)
    with open(output,'w') as f:
        f.write(stream)
    print(f'Paremeters are generated to {str(output)}')

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
def cmd_run(args, parameter = None, output = None, printTime = True):
    if not parameter:
        parameter = args.paramter
    if not output:
        output = args.output
    os.makedirs(output, exist_ok=True)
    #os.makedirs('elapsed_time',exist_ok=True)
    parameters = load_parameters(parameter)
    parameters['bi1'] = {'date':parameters['bi1']['datetime']}
    print(f"run for {args.nruns} times ...")
    if printTime: print("Q\tNrow\tMedian time\t")

    alltime = []
    for workload in args.workload:
        time_list = []
        result = workload.run(parameters[workload.name])
        time_list.append(result.elapsed)    
        # write the query results to log/bi[1-20]
        result_file = output/ (workload.name+'.txt')  
        writeResult(result.result, result_file)
        #with open(Path('elapsed_time')/ (workload.name+'.txt'), 'w') as f:
        #    f.write(str(result.elapsed))
        
        for i in range(args.nruns-1):
            result = workload.run(parameters[workload.name])
            time_list.append(result.elapsed)    
            #with open(Path('elapsed_time')/ (workload.name+'.txt'), 'a') as f:
            #    f.write(','+str(result.elapsed))
        median_time = median(time_list)
        alltime.append(median_time)
        if printTime: print(f'{workload.name}\t{len(result.result)}\t{median_time:.2f}')
    print(f'Results are written to {str(output)}')
    return alltime 

def cmd_compare(args):
    for workload in args.workload:
        Pass = True
        file1 = args.source/(workload.name+'.txt')
        file2 = args.target/(workload.name+'.txt')
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

def cmd_all(args):
    cmd_load_all(args)
    cmd_run(args)

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
    print('Workload:', list2str([w.name for w in args.workload]))

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
        args.data_dir = Path(mds[1])
    else:
        args.machine, args.data_dir = 'm1:', Path(args.machine_dir)
        if args.cmd == 'refresh': # because some folders may not exist
            args.machine = 'ANY:'
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