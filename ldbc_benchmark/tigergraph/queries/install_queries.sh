#!/bin/bash

# helper query
gsql "$PWD/helper/get_messages_from_person.gsql"

# interactive short queries
for i in {1..7}
do
  gsql "$PWD/interactive_short/is_$i.gsql"
done

# interactive complex queries
# for i in {1..14}
# do
#   gsql "$PWD/interactive_complex/ic_$i.gsql"
# done

# business intelligence queries
# for i in {1..25}
# do
#   gsql "$PWD/business_intelligence/bi_$i.gsql"
# done