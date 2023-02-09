#!/usr/bin/python3
from pathlib import Path
import time
import requests
import re
import subprocess
import datetime
import json
import sys
sys.path.append('../common')
from result_mapping import result_mapping

# query timeout value in milliseconds
HEADERS = {'GSQL-TIMEOUT': '36000000'}


def convert_value_to_string(value, result_type):
    if result_type == "ID[]" or result_type == "INT[]" or result_type == "INT32[]" or result_type == "INT64[]":
        return [int(x) for x in value]
    elif result_type == "ID" or result_type == "INT" or result_type == "INT32" or result_type == "INT64":
        return int(value)
    elif result_type == "FLOAT" or result_type == "FLOAT32" or result_type == "FLOAT64":
        return float(value)
    elif result_type == "STRING[]":
        return value
    elif result_type == "STRING":
        return value
    elif result_type in ["DATETIME", "DATE"]:
        return value.replace(" ", "T")
    elif result_type == "BOOL":
        return bool(value)
    else:
        raise ValueError(f"Result type {result_type} not found")


def cast_parameter_to_driver_input(value, type):
    if type == "ID[]" or type == "INT[]" or type == "INT32[]" or type == "INT64[]":
        return [int(x) for x in value.split(";")]
    elif type == "ID" or type == "INT" or type == "INT32" or type == "INT64":
        return int(value)
    elif type == "STRING[]":
        return value.split(";")
    elif type in ["STRING", "DATETIME", "DATE"]:
        return value
    else:
        raise ValueError(f"Parameter type {type} not found")
# ================ END: Variables and Functions from Cypher ========================

def run_query(endpoint, query_num, parameters):
    start = time.time()
    response = requests.get(f'{endpoint}/query/ldbc_snb/bi{query_num}', headers=HEADERS, params=parameters).json()
    end = time.time()
    duration = end - start
    if response['error']:
        if query_num == 11:
            return f"""[{{"count": 0}}]""", duration
        elif query_num == 15:
            return f"""[{{"weight": -1.0}}]""", duration
        else:
            print(response['message'])
            return '[]', 0
    results = response['results'][0]['result']
    # for BI-11 and BI-15, result is a single value
    if query_num == 11:
        return f"""[{{"count": {results}}}]""", duration
    elif query_num == 15:
        return f"""[{{"weight": {results}}}]""", duration
    
    #convert results from [dict()] to [[]] 
    results = [[v for k,v in res.items()] for res in results]
    #convert results to string
    mapping = result_mapping[query_num]
    result_tuples = [
            {
                result_descriptor["name"]: convert_value_to_string(result[i], result_descriptor["type"])
                for i, result_descriptor in enumerate(mapping)
            }
            for result in results
        ]

    return json.dumps(result_tuples), duration


def run_queries(query_variants, parameter_csvs, sf, results_file, timings_file, batch_date, batch_type, args):
    start = time.time()
    for query_variant in query_variants:
        query_num = int(re.sub("[^0-9]", "", query_variant))
        query_subvariant = re.sub("[^ab]", "", query_variant)

        print(f"========================= Q {query_num:02d}{query_subvariant.rjust(1)} =========================")
        query_num = int(re.sub("[^0-9]", "", query_variant))
        parameters_csv = parameter_csvs[query_variant]

        i = 0
        for query_parameters in parameters_csv:
            i = i + 1

            query_parameters_split = {k.split(":")[0]: v for k, v in query_parameters.items()}
            query_parameters_in_order = json.dumps(query_parameters_split)

            query_parameters = {k.split(":")[0]: cast_parameter_to_driver_input(v, k.split(":")[1]) for k, v in query_parameters.items()}
            if args.test:
                print(f'Q{query_variant}: {query_parameters}')
            # Q1 parameter name is conflict with TG data type keyword 'datetime' 
            if query_num == 1: query_parameters = {'date': query_parameters['datetime']}
            results, duration = run_query(args.endpoint, query_num, query_parameters)

            results_file.write(f"{query_num}|{query_variant}|{query_parameters_in_order}|{results}\n")
            results_file.flush()
            timings_file.write(f"TigerGraph|{sf}|{batch_date}|{batch_type}|{query_variant}|{query_parameters_in_order}|{duration:.6f}\n")
            timings_file.flush()

            # - test run: 1 query
            # - regular run: 30 queries
            if args.test:
                print(f"-> {duration:.4f} seconds")
                print(f"-> {results}")
            if args.test or i == args.nruns:
                break

    return time.time() - start

def run_precompute(args, timings_file, sf, batch_id, batch_type):
    t0 = time.time()
    print(f"==================== Precompute for BI 4, 6, 19, 20 ======================")
    # compute values and print to files
    for q in [4,6,20]:
        t1 = time.time()
        requests.get(f'{args.endpoint}/query/ldbc_snb/precompute_bi{q}', headers=HEADERS)
        duration = time.time()-t1
        print(f'precompute_bi{q}:\t\t{duration:.4f} s')
        timings_file.write(f"TigerGraph|{sf}|{batch_id}|{batch_type}|q{q}precomputation||{duration}\n")

    # precompute q19
    t1 = time.time()
    requests.get(f'{args.endpoint}/query/ldbc_snb/cleanup_bi19', headers=HEADERS)
    print(f'cleanup_bi19:\t\t{time.time()-t1:.4f} s')
    start = datetime.date(2010,1,1)
    nbatch = 12 # can be smaller if memory is sufficient
    for i in range(nbatch):
      t2 = time.time()
      end = start + datetime.timedelta(days=365*3//nbatch + 1)
      output = Path('/home/tigergraph/reply_count')
      out_file = output / f'part_{i:04d}.csv'
      params = {'startDate':start, 'endDate': end, 'file': str(out_file)}
      requests.get(f'{args.endpoint}/query/ldbc_snb/precompute_bi19', params = params, headers=HEADERS)
      print(f'precompute_bi19({start},{end}):{time.time()-t2:.4f} s')
      start = end
    q19precomputation_total_duration = time.time()-t1
    timings_file.write(f"TigerGraph|{sf}|{batch_id}|{batch_type}|q19precomputation||{q19precomputation_total_duration}\n")


    # load the files (this is faster in large SF)
    t1 = time.time()
    if not args.cluster:
        subprocess.run(f"docker exec --user tigergraph snb-bi-tg bash -c '/home/tigergraph/tigergraph/app/cmd/gsql -g ldbc_snb RUN LOADING JOB load_precompute'", shell=True)
    else:
        subprocess.run(f'gsql -g ldbc_snb RUN LOADING JOB load_precompute', shell=True)
    print(f'load_precompute:\t\t{time.time()-t1:.4f} s')
    return time.time() - t0
