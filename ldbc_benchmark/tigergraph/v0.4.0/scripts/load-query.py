#!/usr/bin/env python3

import argparse
import subprocess
import sys
from pathlib import Path

def get_all_queries(queries_dir_path):
    return set(p.stem for p in queries_dir_path.glob('*.gsql'))

def get_valid_invalid_queries(all_queries, queries):
    return sorted(all_queries & queries), sorted(queries - all_queries)

if __name__ == '__main__':
    script_dir_path = Path(__file__).resolve().parent
    queries_dir_path = (script_dir_path / '..' / 'queries').resolve()

    all_queries = get_all_queries(queries_dir_path)

    parser = argparse.ArgumentParser(description='Creates and installs queries.')
    parser.add_argument('queries', nargs='*', default=all_queries, help='queries to install (default: all)')
    parser.add_argument('-i', '--install', action='store_true', help='install queries')
    args = parser.parse_args()

    queries = set(args.queries)
    valid_queries, invalid_queries = get_valid_invalid_queries(all_queries, queries)

    if len(invalid_queries) > 0:
        print(f'Skipping invalid queries: {", ".join(invalid_queries)}')

    if len(valid_queries) == 0:
        print('There is no queries to install.')
        sys.exit(1)

    print(f'Valid queries: {", ".join(valid_queries)}')

    print('Dropping queries...')
    gsql = 'DROP QUERY ' + ','.join(valid_queries) + '\n'
    subprocess.run(['gsql', '-g', 'ldbc_snb', gsql])

    print('\nCreating queries...')
    for query in valid_queries:
        query_path = queries_dir_path.joinpath(query + '.gsql')
        subprocess.run(['gsql', '-g', 'ldbc_snb', str(query_path)])

    if args.install:
        print('\nInstalling queries...')
        gsql = 'INSTALL QUERY ' + ','.join(valid_queries) + '\n'
        subprocess.run(['gsql', '-g', 'ldbc_snb', gsql])
