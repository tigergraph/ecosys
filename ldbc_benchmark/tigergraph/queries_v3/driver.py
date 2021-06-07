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

def get_parser():
    # The top-level parser.
    main_parser = argparse.ArgumentParser(description='Various utilities working with GSQL for LDBC SNB.')
    main_parser.set_defaults(func=lambda _: main_parser.print_usage())
    main_subparsers = main_parser.add_subparsers(dest='cmd')

    # The parser for loading.
    load_parser = main_subparsers.add_parser('load', help='Load the schema, queries, or data.')
    load_parser.set_defaults(func=lambda _: load_parser.print_usage())
    load_subparsers = load_parser.add_subparsers(dest='cmd_load')

    load_schema_parser = load_subparsers.add_parser('schema', help='Load the LDBC SNB schema.')
    load_schema_parser.set_defaults(func=cmd_load_schema)

    load_query_parser = load_subparsers.add_parser('query', help='Load queries for workload(s).')
    load_query_parser.add_argument('workload', nargs='?', default='', help='The workload to load queries from.')
    load_query_parser.set_defaults(func=cmd_load_query)

    load_data_parser = load_subparsers.add_parser('data', help='Load data.')
    load_data_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    load_data_parser.set_defaults(func=cmd_load_data)

    load_all_parser = load_subparsers.add_parser('all', help='Load the schema, queries, and data.')
    load_all_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    load_all_parser.set_defaults(func=cmd_load_all)

    # The parser for running.
    run_parser = main_subparsers.add_parser('run', help='Run the workload(s).')
    run_parser.add_argument('parameters_dir', type=Path, help='The directory to load parameters from.')
    run_parser.add_argument('workload', nargs='?', default='', help='The workload to run.')
    run_parser.set_defaults(func=cmd_run)

    # Running all from loading to running.
    all_parser = main_subparsers.add_parser('all', help='Do all of the above.')
    all_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    all_parser.add_argument('parameters_dir', type=Path, help='The directory to load parameters from.')
    all_parser.set_defaults(func=cmd_all)

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

'''
def transform_parameter_bi1(parameter):
    return {'date': parameter['datetime']}
'''

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


DATA_NAMES = [
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
    'Organisation',
    'Organisation_isLocatedIn_Place',
    'Person',
    'Person_hasInterest_Tag',
    'Person_isLocatedIn_City',
    'Person_knows_Person',
    'Person_likes_Comment',
    'Person_likes_Post',
    'Person_studyAt_University',
    'Person_workAt_Company',
    'Place',
    'Place_isPartOf_Place',
    'Post',
    'Post_hasCreator_Person',
    'Post_hasTag_Tag',
    'Post_isLocatedIn_Country',
    'Tag',
    'TagClass',
    'TagClass_isSubclassOf_TagClass',
    'Tag_hasType_TagClass',
]

SCRIPT_DIR_PATH = Path(__file__).resolve().parent

WORKLOADS = [
    Workload('bi1', [Query('bi1')], ResultTransform()[0][0]['@@result']),
    Workload('bi2', [Query('bi2')], ResultTransform()[0][0]['@@result']),
    Workload('bi3', [Query('bi3')], ResultTransform()[0][0]['@@result'].change_keys({
        'forumId': 'forum.id',
        'forumTitle': 'forum.title',
        'forumCreationDate': 'forum.creationDate',
        'personId': 'person.id',
    })),
    Workload('bi4', [Query('bi4')], ResultTransform()[0][0]['@@result'].change_keys({
        'personId': 'person.id',
        'personFirstName': 'person.firstName',
        'personLastName': 'person.lastName',
        'personCreationDate': 'person.creationDate',
    })),
    Workload('bi5', [Query('bi5')], ResultTransform()[0][0]['@@result'].change_keys({
        'personId': 'person.id',
    })),
    Workload('bi6', [Query('bi6')], ResultTransform()[0][0]['@@result'].change_keys({
        'personId': 'person.id',
    })),
    Workload('bi7', [Query('bi7')], ResultTransform()[0][0]['@@result'].change_keys({
        'relatedTagName': 'relatedTag.name',
        'replyCount': 'count',
    })),
    Workload('bi8', [Query('bi8')], ResultTransform()[0][0]['@@result'].del_keys(['totalScore']).change_keys({
        'personId': 'person.id',
    })),
    Workload('bi9', [Query('bi9')], ResultTransform()[0][0]['@@result'].change_keys({
        'personId': 'person.id',
        'personFirstName': 'person.firstName',
        'personLastName': 'person.lastName',
    })),
    Workload('bi10', [Query('bi10')], ResultTransform()[0][0]['result'].change_keys({
        'expertCandidatePersonId': 'expertCandidatePerson.id',
        'tagName': 'tag.name',
    })),
    Workload('bi11', [Query('bi11')], ResultTransform()[0][0].change_key({'@@count': 'count'})),
    Workload('bi12', [Query('bi12')], ResultTransform()[0][0]['@@result']),
    Workload('bi13', [Query('bi13')], ResultTransform()[0][0]['@@result'].change_keys({
        'zombieId': 'zombie.id',
    })),
    Workload('bi14', [Query('bi14')], ResultTransform()[0][0]['@@result'].change_keys({
        'person1Id': 'person1.id',
        'person2Id': 'person2.id',
        'city1Name': 'city1.name',
    })),
    Workload(
        'bi15',
        [
            Query('bi15_add_weighted_edges'),
            Query('bi15'),
            Query('bi15_delete_weighted_edges'),
        ],
        ResultTransform()[1][0]['@@result'].change_keys({'personId': 'person.id'}),
    ),
    Workload('bi16', [Query('bi16')], ResultTransform()[0][0]['@@result'].del_keys(['totalMessageCount']).change_keys({
        'personId': 'person.id',
    })),
    Workload('bi17', [Query('bi17')], ResultTransform()[0][0]['@@result'].change_keys({
        'person1Id': 'person1.id',
    })),
    Workload('bi18', [Query('bi18')], ResultTransform()[0][0]['@@result'].change_keys({
        'person2Id': 'person2.id',
    })),
    Workload(
        'bi19',
        [
            Query('bi19_add_weighted_edges'),
            Query('bi19'),
            Query('bi19_delete_weighted_edges'),
        ],
        ResultTransform()[1][0]['@@result'].change_keys({
            'person1Id': 'person1.id',
            'person2Id': 'person2.id',
        }),
    ),
    Workload('bi20', [Query('bi20')], ResultTransform()[0][0]['@@result'].change_keys({
        'person1Id': 'person1.id',
    })),
]

'''Loads parameters for a given file'''
def load_allparameters(file):
    with open(file) as f: 
        txt = f.readlines() 
        txt = ''.join(txt).replace('\n','')
        return ast.literal_eval(txt)

def cmd_load_schema(args):
    '''Loads the schema.'''
    subprocess.run(['gsql', str(SCRIPT_DIR_PATH / 'schema.gsql')])


def cmd_load_query(args):
    '''Loads queries from the given workloads.'''
    gsql = ''

    for workload in args.workload:
        workload_path = (SCRIPT_DIR_PATH / 'reads' / f'{workload.name}.gsql').resolve()
        gsql += f'@{workload_path}\n'

    queries_to_install = [
        query.name
        for workload in args.workload
        for query in workload.queries
    ]
    gsql += f'INSTALL QUERY {", ".join(queries_to_install)}\n'

    subprocess.run(['gsql', '-g', 'ldbc_snb'], input=gsql.encode())

def cmd_load_csv(args):
    '''Loads data from the given data_dir path.'''
    file_paths = [(args.data_dir / name).with_suffix('.csv') for name in DATA_NAMES]
    gsql = 'RUN LOADING JOB load_ldbc_snb USING '
    gsql += ', '.join(f'file_{name}="{file_path}"' for name, file_path in zip(DATA_NAMES, file_paths))
    subprocess.run(['gsql', '-g', 'ldbc_snb', gsql])

def cmd_load_data(args):
    '''Loads data from the given data_dir path.'''
    if args.data_dir is None: 
        subprocess.run('RUN LOADING JOB load_ldbc_snb')
        return 
    file_paths = [(args.data_dir / name) for name in DATA_NAMES]
    gsql = 'RUN LOADING JOB load_ldbc_snb USING '
    gsql += ', '.join(f'file_{name}="ALL:{file_path}"' for name, file_path in zip(DATA_NAMES, file_paths))
    subprocess.run(['gsql', '-g', 'ldbc_snb', gsql])

def cmd_load_all(args):
    '''Loads the schema, data and queries.'''
    cmd_load_schema(args)
    cmd_load_data(args)
    cmd_load_query(args)


def median(time_list):
    return sorted(time_list)[len(time_list)//2]

def cmd_run(args):
    os.makedirs('results',exist_ok=True)
    os.makedirs('elapsed_time',exist_ok=True)
    additional_run = 2

    median_time = []
    parameters = load_allparameters(args.parameters_dir)
    for workload in args.workload:
        time_list = []
        print(f"running query {workload.name} for {additional_run+1} times...")
        result = workload.run(parameters[workload.name])
        time_list.append(result.elapsed)    
        # write the query results to log/bi[1-20]  
        with open(Path('results')/ (workload.name+'.txt'), 'w') as f:
            f.write(str(result.result))
        with open(Path('elapsed_time')/ (workload.name+'.txt'), 'w') as f:
            f.write(str(result.elapsed))
        
        for i in range(additional_run):
            result = workload.run(parameters[workload.name])
            time_list.append(result.elapsed)    
            with open(Path('elapsed_time')/ (workload.name+'.txt'), 'a') as f:
                f.write(','+str(result.elapsed))
        median_time.append(median(time_list))
    print('median time are: '+list2str(median_time))


def cmd_all(args):
    cmd_load_all(args)
    cmd_run(args)

def check_args(args):
    # Parse workload from string to a list 
    if (
        args.cmd == 'load' and (args.cmd_load == 'query' or args.cmd_load == 'all')
        or args.cmd == 'run'
        or args.cmd == 'all'
    ):
        # default is all the workloads
        if getattr(args, 'workload', '') == '':
            args.workload = WORKLOADS
        # exclude cases
        elif args.workload.startswith('not:'):
            print(args.workload)
            exclude = args.workload[4:].split(',')
            expected = set(w.name for w in WORKLOADS)
            actual = expected -set(exclude)
            args.workload = [w for w in WORKLOADS if w.name in actual]
        # regular expression
        elif args.workload.startswith('reg:'):
            r = re.compile(args.workload[4:])
            actual = list(filter(r.match, [w.name for w in WORKLOADS]))
            print(list(actual))
            args.workload = [w for w in WORKLOADS if w.name in actual]
        else:
            args.workload = args.workload.split(',')
            expected = set(w.name for w in WORKLOADS)
            actual = set(args.workload)            
            remaining = actual - expected
            if len(remaining) > 0:
                raise ValueError(f'Invalid workload {", ".join(sorted(remaining))}.')
            args.workload = [w for w in WORKLOADS if w.name in actual]
        print('Queries to be run:', list2str([w.name for w in args.workload]))
    if (args.cmd == 'load' and (args.cmd_load=='csv')):
        missing = []
        for name in DATA_NAMES:
            file_path = (args.data_dir / name).with_suffix('.csv')
            if not file_path.is_file():
                missing.append(file_path.name)
        if len(missing) > 0:
            raise ValueError(f'The directory "{args.data_dir}" is missing files {", ".join(missing)}.')

    if (args.cmd == 'load' and (args.cmd_load in ['dir','data','all']))  or args.cmd == 'all':
        missing = []
        for name in DATA_NAMES:
            file_path = (args.data_dir / name)
            if not file_path.is_dir():
                missing.append(file_path.name)
        if len(missing) > 0:
            raise ValueError(f'The directory "{args.data_dir}" is missing files {", ".join(missing)}.')

def main():
    args = get_parser().parse_args()
    check_args(args)
    args.func(args)


if __name__ == '__main__':
    main()
