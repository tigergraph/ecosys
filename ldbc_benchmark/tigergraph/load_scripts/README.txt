pre_process_data.sh
-------------------
description:

 	ldbc data generator will generate edges connecting supper classes(it will have mixed edges of subclasses pairs). 
        E.g. organization_isLocatedIn_place file will contains (university, city), (company,country). Below we extract them to separate files.

        compile and run SplitPlace.cpp and SplitEdges.cpp
        split edge organisation_isLocatedIn_place_*_0.csv into university_isLocatedIn_city.csv and company_isLocatedIn_country.csv
        split edge place_isPartOf_place_*_0.csv into city_isPartOf_country.csv and country_isPartOf_continent.csv
        then delete organisation_isLocatedIn_place_*_0.csv and place_isPartOf_place_*_0.csv
        the new data files located in the same folder as raw data files

move_data.sh
-------------------
description: 

        all raw data files are located in the same folder e.g /home/ubuntu/data/social_network
        in order to make loading job more conveniently, need to group same type of edge files and vertex files together.
        for same type edge files or vertex files, create new folder to store them.


setup_schema.gsql
-------------------
description:  

        setup schema

load_data.sh
-------------------
description:

        load pre-processed raw data into TigerGraph

one_step_load.sh
-------------------
description:
        if you are confident about above scripts, run one_step_load.sh 
        this script execute pre_process_data.sh, move_data.sh, setup_schema.gsql, load_data.sh one by one
