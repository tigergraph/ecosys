# Pre-generated data sets

## Streaming decompression

To download and decompress the data sets on-the-fly, make sure you have `curl` and [`zstd`](https://facebook.github.io/zstd/) installed, then run:

```bash
export DATASET_URL=...
curl --silent --fail ${DATASET_URL} | tar -xv --use-compress-program=unzstd
```

For multi-file data sets, first download them. Then, to recombine and decompress, run:

```bash
cat <data-set-filename>.tar.zst* | tar -xv --use-compress-program=unzstd
```

This command works on both standalone files (`.tar.zst`) and chunked ones (`.tar.zst.XXX`).


## Data sets links

### Compressed CSVs in the composite-merged-fk format

* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1-composite-merged-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3-composite-merged-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10-composite-merged-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf30-composite-merged-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf100-composite-merged-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf300-composite-merged-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1000-composite-merged-fk.tar.zst.000
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1000-composite-merged-fk.tar.zst.001

### Compressed CSVs in the composite-projected-fk format

* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1-composite-projected-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3-composite-projected-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10-composite-projected-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf30-composite-projected-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf100-composite-projected-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf300-composite-projected-fk.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1000-composite-projected-fk.tar.zst.000
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1000-composite-projected-fk.tar.zst.001
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1000-composite-projected-fk.tar.zst.002
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.000
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.001
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.002
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.003
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.004
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.005
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3000-composite-projected-fk.tar.zst.006
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.000
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.001
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.002
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.003
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.004
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.005
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.006
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.007
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.008
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.009
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.010
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.011
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.012
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.013
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.014
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.015
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.016
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.017
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.018
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.019
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.020
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10000-composite-projected-fk.tar.zst.021

### Compressed CSVs in the composite-projected CSV format with quotes and without headers

* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1-composite-projected-fk-with-quotes-without-headers.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3-composite-projected-fk-with-quotes-without-headers.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10-composite-projected-fk-with-quotes-without-headers.tar.zst

### Raw (up to SF30)

* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf1-raw.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf3-raw.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf10-raw.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/bi-sf30-raw.tar.zst

### Factor tables

* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf1.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf3.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf10.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf30.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf100.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf300.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf1000.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf3000.tar.zst
* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/factors/factors-sf10000.tar.zst

### Parameters

* https://pub-383410a98aef4cb686f0c7601eddd25f.r2.dev/bi-pre-audit/parameters-2022-10-01.zip
