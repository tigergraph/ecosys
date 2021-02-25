#!/usr/bin/env python3

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

import requests


# TODO: Merge load-query.py file.
# TODO: Parse the result.
# TODO: Keep running other queries when one query fails.
# TODO: Get query information from TigerGraph directly with /gsqlserver/gsql/queryinfo endpoint.
# TODO: Run query in detached mode.
# TODO: Time the query.

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

ENDPOINT = 'http://127.0.0.1:9000/query/ldbc_snb/'

SCRIPT_DIR_PATH = Path(__file__).resolve().parent

WORKLOADS = {
    'bi1': {
        'queries': {
            'bi1': {},
        },
    },
    'bi2': {
        'queries': {
            'bi2': {},
        },
    },
    'bi3': {
        'queries': {
            'bi3': {},
        },
    },
    'bi4': {
        'queries': {
            'bi4': {},
        },
    },
    'bi5': {
        'queries': {
            'bi5': {},
        },
    },
    'bi6': {
        'queries': {
            'bi6': {},
        },
    },
    'bi7': {
        'queries': {
            'bi7': {},
        },
    },
    'bi8': {
        'queries': {
            'bi8': {},
        },
    },
    'bi9': {
        'queries': {
            'bi9': {},
        },
    },
    'bi10': {
        'queries': {
            'bi10': {},
        },
    },
    'bi11': {
        'queries': {
            'bi11': {},
        },
    },
    'bi12': {
        'queries': {
            'bi12': {},
        },
    },
    'bi13': {
        'queries': {
            'bi13': {},
        },
    },
    'bi14': {
        'queries': {
            'bi14': {},
        },
    },
    'bi15': {
        'queries': {
            'bi15': {},
            'bi15_add_weighted_edges': {},
            'bi15_delete_weighted_edges': {},
        },
    },
    'bi16': {
        'queries': {
            'bi16': {},
        },
    },
    'bi17': {
        'queries': {
            'bi17': {},
        },
    },
    'bi18': {
        'queries': {
            'bi18': {},
        },
    },
    'bi19': {
        'queries': {
            'bi19': {},
            'bi19_add_weighted_edges': {},
            'bi19_delete_weighted_edges': {},
        },
    },
    'bi20': {
        'queries': {
            'bi20': {},
        },
    },
}


def get_parameters_file_path(parameters_path, query_name):
    '''Returns the parameters file name of the given query name.'''
    return parameters_path / f'{query_name[:2]}-{query_name[2:]}.txt'


def parse_parameters_file(parameters_file_name):
    '''Parses the given file name as a list of parameters.'''

    with open(parameters_file_name, 'r') as f:
        parameters = []
        names = []
        types = []

        for line in f:
            if len(names) == 0:
                # First line; parse the name and type.
                names, types = zip(*(w.split(':') for w in line.split('|')))
                continue

            for name, param_type, value in zip(names, types, line.split('|')):
                if param_type.endswith('[]'):
                    value = value.split(';')

                # Ignore other types.

                parameter[name] = value

            parameters.append(parameter)

        return parameters


def print_usage_from(parser):
    '''Returns a function that prints the usage from given parser when called.'''
    def fn(args):
        parser.print_usage()
    return fn


def load_schema(args):
    '''Loads the schema.'''
    subprocess.run(['gsql', 'schema.gsql'])
    # Check for errors?


def load_query(args):
    '''Loads queries from the given workloads.'''
    gsql = ''

    for workload in args.workload:
        workload_path = (SCRIPT_DIR_PATH / 'workloads' / f'{workload}.gsql').resolve()
        gsql += f'@{workload_path}\n'

    queries_to_install = [
        query
        for workload in args.workload
        for query in WORKLOADS[workload]['queries'].keys()
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


def get_parser():
    # The top-level parser.
    main_parser = argparse.ArgumentParser(description='Various utilities working with GSQL for LDBC SNB.')
    main_parser.set_defaults(func=print_usage_from(main_parser))
    main_subparsers = main_parser.add_subparsers(dest='cmd')

    # The parser for loading.
    load_parser = main_subparsers.add_parser('load', help='Load the schema, queries, or data.')
    load_parser.set_defaults(func=print_usage_from(load_parser))
    load_subparsers = load_parser.add_subparsers(dest='cmd_load')

    load_schema_parser = load_subparsers.add_parser('schema', help='Load the LDBC SNB schema.')
    load_schema_parser.set_defaults(func=load_schema)

    load_query_parser = load_subparsers.add_parser('query', help='Load queries for workload(s).')
    load_query_parser.add_argument('workload', nargs='*', help='Workloads to load queries from.')
    load_query_parser.set_defaults(func=load_query)

    load_data_parser = load_subparsers.add_parser('data', help='Load data.')
    load_data_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    load_data_parser.set_defaults(func=load_data)

    load_all_parser = load_subparsers.add_parser('all', help='Load the schema, queries, and data.')
    load_all_parser.add_argument('data_dir', type=Path, help='The directory to load data from.')
    load_all_parser.set_defaults(func=load_all)

    return main_parser


def check_args(args):
    if args.cmd_load == 'query' or args.cmd_load == 'all':
        if len(args.workload) == 0:
            args.workload = set(WORKLOADS.keys())
        else:
            actual = set(args.workload)
            expected = set(WORKLOADS.keys())
            remaining = actual - expected
            if len(remaining) > 0:
                raise ValueError(f'Invalid workload {", ".join(remaining)}.')
            args.workload = actual

    if args.cmd_load == 'data' or args.cmd_load == 'all':
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
