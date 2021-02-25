#!/usr/bin/env python3

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from timeit import default_timer as timer

import requests


class ResultWithElapsed:
    def __init__(self, result, elapsed):
        self.result = result
        self.elapsed = elapsed


class Query:

    ENDPOINT = 'http://127.0.0.1:9000/query/ldbc_snb/'

    def __init__(self, name, transform_parameter=None):
        self.name = name
        self.transform_parameter = transform_parameter

    def run(self, parameter):
        if self.transform_parameter is not None:
            parameter = self.transform_parameter(parameter)

        start = timer()
        response = requests.get(self.ENDPOINT + self.name, params=parameter).json()
        end = timer()

        if response['error']:
            raise Error(response['message'])

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


def transform_parameter_bi1(parameter):
    return {'date': parameter['datetime']}


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
    'Comment_hasTag_Tag',
    'Forum',
    'Forum_hasMember_Person',
    'Forum_hasTag_Tag',
    'Organisation',
    'Person',
    'Person_hasInterest_Tag',
    'Person_knows_Person',
    'Person_likes_Comment',
    'Person_likes_Post',
    'Person_studyAt_University',
    'Person_workAt_Company',
    'Place',
    'Post',
    'Post_hasTag_Tag',
    'TagClass',
    'Tag',
]

SCRIPT_DIR_PATH = Path(__file__).resolve().parent

WORKLOADS = [
    Workload('bi1', [Query('bi1', transform_parameter_bi1)], ResultTransform()[0][0]['@@result']),
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


def load_parameters(parameters_dir, workload):
    '''Loads parameters for the given workload in the given directory.'''
    parameters = []

    parameters_file_path = parameters_dir / f'{workload.name[:2]}-{workload.name[2:]}.txt'
    with open(parameters_file_path, 'r') as parameters_file:
        names = []
        types = []

        for line in parameters_file:

            line = line.rstrip('\n')

            if len(names) == 0:
                # First line; parse the name and type.
                names, types = zip(*(w.split(':') for w in line.split('|')))
                continue

            parameter = {}

            for name, param_type, value in zip(names, types, line.split('|')):
                if param_type.endswith('[]'):
                    value = value.split(';')

                # Ignore other types.

                parameter[name] = value

            parameters.append(parameter)

    return parameters


def load_schema(args):
    '''Loads the schema.'''
    subprocess.run(['gsql', 'schema.gsql'])


def load_query(args):
    '''Loads queries from the given workloads.'''
    gsql = ''

    for workload in args.workload:
        workload_path = (SCRIPT_DIR_PATH / 'workloads' / f'{workload.name}.gsql').resolve()
        gsql += f'@{workload_path}\n'

    queries_to_install = [
        query.name
        for workload in args.workload
        for query in workload.queries
    ]
    gsql += f'INSTALL QUERY {", ".join(queries_to_install)}\n'

    subprocess.run(['gsql', '-g', 'ldbc_snb'], input=gsql.encode())


def load_data(args):
    '''Loads data from the given data_dir path.'''
    file_paths = [(args.data_dir / name).with_suffix('.csv') for name in DATA_NAMES]
    gsql = 'RUN LOADING JOB load_ldbc_snb_composite_merged_fk USING '
    gsql += ', '.join(f'file_{name}="{file_path}"' for name, file_path in zip(DATA_NAMES, file_paths))
    subprocess.run(['gsql', '-g', 'ldbc_snb', gsql])


def load_all(args):
    '''Loads the schema, queries, and data.'''
    load_schema(args)
    load_query(args)
    load_data(args)


def run(args):
    '''Runs the given workload(s).'''
    for workload in args.workload:
        parameters = load_parameters(args.parameters_dir, workload)
        for parameter in parameters:
            result = workload.run(parameter)
            print(result.result)
            print(result.elapsed)


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
    load_schema_parser.set_defaults(func=load_schema)

    load_query_parser = load_subparsers.add_parser('query', help='Load queries for workload(s).')
    load_query_parser.add_argument('workload', nargs='*', help='The workload to load queries from.')
    load_query_parser.set_defaults(func=load_query)

    load_data_parser = load_subparsers.add_parser('data', help='Load data.')
    load_data_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    load_data_parser.set_defaults(func=load_data)

    load_all_parser = load_subparsers.add_parser('all', help='Load the schema, queries, and data.')
    load_all_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    load_all_parser.set_defaults(func=load_all)

    # The parser for running.
    run_parser = main_subparsers.add_parser('run', help='Run the workload(s).')
    run_parser.add_argument('parameters_dir', type=Path, help='The directory to load parameters from.')
    run_parser.add_argument('workload', nargs='*', help='The workload to run.')
    run_parser.set_defaults(func=run)

    return main_parser


def check_args(args):
    if args.cmd == 'load' and (args.cmd_load == 'query' or args.cmd_load == 'all') or args.cmd == 'run':
        # If the command is all, the workload may not be defined.
        if len(getattr(args, 'workload', [])) == 0:
            # The default is all workloads.
            args.workload = WORKLOADS
        else:
            actual = set(args.workload)
            expected = set(w.name for w in WORKLOADS)
            remaining = actual - expected
            if len(remaining) > 0:
                raise ValueError(f'Invalid workload {", ".join(sorted(remaining))}.')
            args.workload = [w for w in WORKLOADS if w.name in actual]

    if args.cmd == 'load' and (args.cmd_load == 'data' or args.cmd_load == 'all'):
        missing = []
        for name in DATA_NAMES:
            file_path = (args.data_dir / name).with_suffix('.csv')
            if not file_path.is_file():
                missing.append(file_path.name)
        if len(missing) > 0:
            raise ValueError(f'The directory "{args.data_dir}" is missing files {", ".join(missing)}.')


def main():
    args = get_parser().parse_args()
    check_args(args)
    args.func(args)


if __name__ == '__main__':
    main()
