#!/usr/bin/env python3

import argparse
import csv
import json
import os
import sys
from pathlib import Path

import requests


# TODO: Merge load-query.py file.
# TODO: Parse the result.
# TODO: Keep running other queries when one query fails.
# TODO: Get query information from TigerGraph directly with /gsqlserver/gsql/queryinfo endpoint.
# TODO: Run query in detached mode.
# TODO: Time the query.


# Each query name has keys 'parameters' and 'result'.
# 'parameters' is a dictionary
#     from the parameter name in the parameter text file
#     to the parameter name in the query.
# 'result' is the name that contains the printed result.
CONVERSION = {
    'bi1': {
        'parameters': {'datetime': 'date'},
        'result': '@@result',
    },
    'bi2': {
        'parameters': {'date': 'date', 'tagClass': 'tagClassName'},
        'result': '@@result',
    },
    'bi3': {
        'parameters': {'tagClass': 'tagClassName', 'country': 'countryName'},
        'result': '@@result',
    },
    'bi4': {
        'parameters': {'country': 'countryName'},
        'result': '@@result',
    },
    'bi5': {
        'parameters': {'tag': 'tagName'},
        'result': '@@result',
    },
    'bi6': {
        'parameters': {'tag': 'tagName'},
        'result': '@@result',
    },
    'bi7': {
        'parameters': {'tag': 'tagName'},
        'result': '@@result',
    },
    'bi8': {
        'parameters': {'tag': 'tagName', 'date': 'date'},
        'result': '@@result',
    },
    'bi9': {
        'parameters': {'startDate': 'startDate', 'endDate': 'endDate'},
        'result': '@@result',
    },
    'bi10': {
        'parameters': {
            'personId': 'personId',
            'country': 'countryName',
            'tagClass': 'tagClassName',
            'minPathDistance': 'minPathDistance',
            'maxPathDistance': 'maxPathDistance'
        },
        'result': 'result',
    },
    'bi11': {
        'parameters': {'country': 'countryName', 'startDate': 'startDate'},
        'result': '@@count',
    },
    'bi12': {
        'parameters': {
            'date': 'date',
            'lengthThreshold': 'lengthThreshold',
            'languages': 'languages',
        },
        'result': '@@result',
    },
    'bi13': {
        'parameters': {'country': 'countryName', 'endDate': 'endDate'},
        'result': '@@result',
    },
    'bi14': {
        'parameters': {'country1': 'country1Name', 'country2': 'country2Name'},
        'result': '@@result',
    },
    'bi15': {
        'parameters': {
            'person1Id': 'person1Id',
            'person2Id': 'person2Id',
            'startDate': 'startDate',
            'endDate': 'endDate',
        },
        'result': '@@result',
    },
    'bi16': {
        'parameters': {
            'tagA': 'tagNameA',
            'dateA': 'dateA',
            'tagB': 'tagNameB',
            'dateB': 'dateB',
            'maxKnowsLimit': 'maxKnowsLimit',
        },
        'result': '@@result',
    },
    'bi17': {
        'parameters': {'tag': 'tagName', 'delta': 'delta'},
        'result': '@@result',
    },
    'bi18': {
        'parameters': {'person1Id': 'person1Id', 'tag': 'tagName'},
        'result': '@@result',
    },
    #'bi19': {
    #    'parameters': {'city1Id': 'city1Id', 'city2Id': 'city2Id'},
    #    'result': '@@result',
    #},
    'bi20': {
        'parameters': {'company': 'companyName', 'person2Id': 'person2Id'},
        'result': '@@result',
    },
}

ENDPOINT = 'http://127.0.0.1:9000/query/ldbc_snb/'


def get_parameters_file_path(parameters_path, query_name):
    '''Returns the parameters file name of the given query name.'''
    return parameters_path / f'{query_name[:2]}-{query_name[2:]}.txt'


def process_parameter_value(param_type, value):
    '''Converts the value according to the parameter type.'''

    if param_type.endswith('[]'):
        return value.split(';')

    # Ignore other types than list.
    return value


def parse_parameters(parameters_file, conversion):
    '''Parses given file as list of parameters for a query.'''

    parameters_csv = csv.reader(parameters_file, delimiter='|', strict=True)
    query_parameters = []

    names = []
    types = []

    for row in parameters_csv:

        if len(names) == 0:
            names, types = zip(*(v.split(':') for v in row))
            continue

        query_parameter = {
            conversion[name]: process_parameter_value(param_type, value)
            for name, param_type, value in zip(names, types, row)
        }

        query_parameters.append(query_parameter)

    return query_parameters


def run_query(name, params):
    '''Runs query with given name and parameters.'''
    r = requests.get(ENDPOINT + name, params=params)
    r.raise_for_status()
    return r.json()


def process_result(result, conversion):
    '''Processes the result.'''
    if result['error']:
        raise ValueError(result['message'])

    return result['results'][0][conversion]


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run queries with given parameters.')
    parser.add_argument('parameters', help='directory to parameters files')
    parser.add_argument('queries', nargs='*', default=CONVERSION.keys(), help='queries to run (default: all)')
    args = parser.parse_args()

    parameters_path= Path(args.parameters).resolve()

    invalid_queries = set(args.queries) - set(CONVERSION.keys())
    if len(invalid_queries) > 0:
        print('There are invalid queries: {", ".join(invalid_queries)}')
        sys.exit(2)

    for query_name in args.queries:
        print(f'\nRunning query: {query_name}')

        parameters_file_path = get_parameters_file_path(parameters_path, query_name)

        with parameters_file_path.open() as f:
            parameters = parse_parameters(f, CONVERSION[query_name]['parameters'])

        for params in parameters:
            result = run_query(query_name, params)
            result = process_result(result, CONVERSION[query_name]['result'])
            print(json.dumps(result, indent=4))
