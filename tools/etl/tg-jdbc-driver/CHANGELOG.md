## Release 1.3.8
* Supported TG Version: 2.4.1+
* Bugfixes:
    1. Increase the default connectTimeout to 30s and socketTimeout to 60s

## Release 1.3.7
* Supported TG Version: 2.4.1+
* New Features:
    1. Loading Stats Aggregation (TG Version >= 3.9.0): the data in a Spark loading job can be divided into multiple batches, now supports aggregating the loading stats of those batches by associating a unique job ID. Detailed instructions can be found at https://github.com/tigergraph/cqrs/tree/master/tg-jdbc-driver#to-load-data-from-files. 
    2. Error Limit for Loading Job (TG Version >= 3.9.0): now supports specifying the `max_num_error` and `max_percent_error` for a loading job (job ID must be given). When the count of invalid objects reach `max_num_error` or the percentage of invalid objects reach `max_percent_error`, the loading job will be aborted. Detailed instructions can be found at https://github.com/tigergraph/cqrs/tree/master/tg-jdbc-driver#to-load-data-from-files. 
* Bugfixes:
    1. Improve connection stability.

## Release 1.3.6
* Supported TG Version: 2.4.1+
* Bugfixes:
    1. Incorrect conversion for null field.

## Release 1.3.5
* Supported TG Version: 2.4.1+
* New Features:
    1. Support disabling `sslHostnameVerification`.

## Release 1.3.4
* Supported TG Version: 2.4.1+
* Bugfixes:
    1. Fix the "notEnoughToken" error.

## Release 1.3.3
* Supported TG Version: 2.4.1+
* Bugfixes:
    1. Print restpp responses in ERROR logs when there's any error.

## Release 1.3.2
* Supported TG Version: 2.4.1+
* Bugfixes:
    1. Fix backward compatibility issue.
    2. Add exponential backoff for heavy restpp traffic.

## Release 1.3.1
* Supported TG Version: 2.4.1+
* Bugfixes:
    1. Fix wrong loading job statistics.

## Release 1.3.0
* Supported TG Version: 2.4.1+
* New Features:
    1. Support path-finding algorithms.
    2. Support all Spark datatypes.
* Bugfixes:
    1. Improve stability.