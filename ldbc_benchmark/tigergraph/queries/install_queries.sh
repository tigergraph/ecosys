#!/bin/bash

# drop queries calling helper queries first to install helper queries
gsql -g ldbc_snb "DROP QUERY ic_3"

# and then add helper queries
for file in "$PWD/helper"/*.gsql
do
  gsql "$file"
done

# interactive short queries
for file in "$PWD/interactive_short"/*.gsql
do
  gsql "$file"
done

for file in "$PWD/interactive_complex"/*.gsql
do
  gsql "$file"
done

for file in "$PWD/business_intelligence"/*.gsql
do
  gsql "$file"
done
