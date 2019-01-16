############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################


pre_process_data.sh
-------------------
LDBC SNB data generator will generate edges connecting supper classes (it will have mixed edges of subclasses pairs), e.g. organization_isLocatedIn_place file will contains (university,city), (company,country). Below we extract them to separate files: 
- Compile and run SplitPlace.cpp and SplitEdges.cpp
- Split edge organisation_isLocatedIn_place_*_0.csv into university_isLocatedIn_city.csv and company_isLocatedIn_country.csv
- Split edge place_isPartOf_place_*_0.csv into city_isPartOf_country.csv and country_isPartOf_continent.csv
- Remove organisation_isLocatedIn_place_*_0.csv and place_isPartOf_place_*_0.csv
- New data files located in the same folder as raw data files


move_data.sh
-------------------
All raw data files are located in a folder under the user tigergraph e.g /home/tigergraph/ldbc_snb_data. In order to make loading job more conveniently, need to group same type of edge files and vertex files together. For same type edge files or vertex files, create new folder to store them.


setup_schema.gsql
-------------------
Setup schema and create loading job.


load_data.sh
-------------------
Load pre-processed raw data into TigerGraph


one_step_load.sh
-------------------
This script executes pre_process_data.sh, move_data.sh, setup_schema.gsql, and load_data.sh in one shot.
