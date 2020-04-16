#!/bin/bash

# Script guide the user through the following 3 steps:

echo "Which graph schema would you like to load?"
echo "  generic    Vertex:          Node"
echo "             Directed Edge:   Link_To (rev: Link_From)"
echo "             Undirected Edge: Link"
echo
echo "  social     Vertex:          Person"
echo "             Directed Edge:   Friend (rev: Also_Friend)"
echo "             Undirected Edge: Coworker"
echo
echo "  movie      Vertex:          Person"
echo "             Directed Edge:   Likes (rev: Reverse_Likes)"
echo "             Undirected Edge: Similarity"
read -p "> " gname
read -p 'WARNING: Installing this graph will drop all current graphs and jobs. Continue? [y/n]: ' yn

case $yn in
	[Yy]* )
		case $gname in
			"movie")
				gsql $gname/schema_${gname}.gsql
                                gsql -g $gname $gname/load_${gname}.gsql
				cd $gname
                                echo "RUN LOADING JOB load_$gname USING movie_file=\"data/data.csv\""
                                gsql -g $gname "RUN LOADING JOB load_$gname USING movie_file=\"data/data.csv\""
				;;
                        "generic")
				gsql $gname/schema_${gname}.gsql
				gsql -g $gname $gname/load_${gname}.gsql
				echo; echo "Which data set would you like to load?"
				echo "  shortest_pos5: 5 vertices, all edge weights are positive"
				echo "  shortest_neg5: 5 vertices, some edge weights are negative"
				read -p "> " dname
				case $dname in
					"shortest_pos5"|"shortest_neg5" )
						cp generic/data/${dname}.csv $gname/data/data.csv
						cd $gname
						gsql -g $gname "RUN LOADING JOB load_$gname USING dir_efile=\"data/data.csv\""
						;;
					* )
					echo "Unknown data file"
					;;
				esac				
				;;	
                        "social")
                                gsql $gname/schema_${gname}.gsql
                                gsql -g $gname $gname/load_${gname}.gsql
                                echo; echo "Which data set would you like to load?"
                                echo "  social10: 10 vertices, used for many documentation examples"
                                echo "  social26: 26 vertices"
                                read -p "> " dname
                                case $dname in
                                        "social10"|"social26" )
                                                cp social/data/${dname}.csv $gname/data/data.csv
                                                cd $gname
                                                echo "RUN LOADING JOB load_$gname USING mixed_efile=\"data/data.csv\""
                                                gsql -g $gname "RUN LOADING JOB load_$gname USING mixed_efile=\"data/data.csv\""
                                                ;;		
			                * )
				        echo "Unknown graph schema"
				        ;;
                                esac
                                ;;
                        * )
                                echo "Unknown graph schema"
                                ;;
                esac
                ;;
	[Nn]* )
		;;
	* )
		read -p "Please answer yes or no: " yn
		;;
esac
