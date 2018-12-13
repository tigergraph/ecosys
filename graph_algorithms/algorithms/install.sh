#!/bin/bash
echo "*** GSQL Graph Algorithm Installer ***"
fExt="_file"
aExt="_attr"

countStringInFile() {
	echo $(grep -c $1 $2)
}
countVertexType() {
	echo $(grep -c "\- VERTEX" schema.$grph)
}
ifEdgeType() {
	echo $(grep -c "RECTED EDGE $1" schema.$grph)
}	
countVertexAttr() {
	echo $(grep "\- VERTEX" schema.$grph|grep -c $1)
}
countEdgeAttr() {
	echo $(grep "RECTED EDGE" schema.$grph|grep -c $1)
}

# 1. Select a graph (done only once)
echo "Available graphs:" 
gsql ls | grep "\- Graph "
read -p 'Graph name? ' grph
grph=${grph//[[:space:]]/}

if [ "${grph}" == "" ]; then
    echo "Graph name cannot be empty."
    exit 1
fi
gsql -g $grph ls > schema.$grph
if [[ $(countVertexType) == 0 ]]; then
	echo "Bad graph name(?) No vertices found."
	exit 1
fi

# 2. Select an algorithm.  After finishing, loop again to select another algorithm.
finished=false
while [ !$finished ]; do
	echo; echo "Please enter the index of the algorithm you want to create or EXIT:"
	select algo in "EXIT" "Closeness Centrality" "Connected Components" "Label Propagation" "Community detection: Louvain" "PageRank" "Shortest Path, Single-Source, No Weight" "Shortest Path, Single-Source, Positive Weight" "Shortest Path, Single-Source, Any Weight" "Triangle Counting(minimal memory)" "Triangle Counting(fast, more memory)" "Cosine Similarity (single vertex)" "Cosine Similary (all vertices)" "Jaccard Similarity (single vertex)" "Jaccard Similary (all vertices)"; do
    	case $algo in
			"Closeness Centrality" )
				algoName="closeness_cent"
				echo "  closeness_cent() works on undirected edges"
				break;;
			"Connected Components" )
				algoName="conn_comp"
				echo "  conn_comp() works on undirected edges"
				break;;
			"Label Propagation" )
				algoName="label_prop"
				echo "  label_prop() works on undirected edges"
				break;;
			"Community detection: Louvain" )
				algoName="louvain"
				echo "  louvain() works on undirected edges"
				break;;
			"PageRank" )
				algoName="pageRank"
				echo "  pageRank() works on directed edges"
				break;;
			"Shortest Path, Single-Source, No Weight" )
                                algoName="shortest_ss_no_wt"
                                echo "  shortest_ss_no_wt() works on directed or undirected edges without weight"
                                break;;
			"Shortest Path, Single-Source, Positive Weight" )
                                algoName="shortest_ss_pos_wt"
                                echo "  shortest_ss_pos_wt() works on weighted directed or undirected edges without negative weight"
                                break;;
			"Shortest Path, Single-Source, Any Weight" )
				algoName="shortest_ss_any_wt"
				echo "  shortest_ss_any_wt() works on weighted directed or undirected edges"
				break;;
			"Triangle Counting(minimal memory)" )
				algoName="tri_count"
				echo "  tri_count() works on undirected edges"
				break;;
			'Triangle Counting(fast, more memory)' )
				algoName="tri_count_fast"
				echo "  tri_count_fast() works on undirected graphs"
				break;;
			'Cosine Similarity (single vertex)' )
				algoName="similarity_cos_single"
                                echo "  similarity_cos_single() calculates the similarity between one given vertex and all other vertices"
                                break;;
			'Cosine Similary (all vertices)' )
				algoName="similarity_cos"
                                echo "  similarity_cos() calculates the similarity between all vertices"
                                break;;
	                'Jaccard Similarity (single vertex)' )
                                algoName="similarity_jaccard_single"
                                echo "  similarity_jaccard_single() calculates the similarity between one given vertex and all other vertices"
                                break;;
                        'Jaccard Similary (all vertices)' )
                                algoName="similarity_jaccard"
                                echo "  similarity_jaccard() calculates the similarity between all vertices"
                                break;;
			"EXIT" )
				finished=true
				break;;
			* )
				echo "Not a valid choice. Try again."
				;;
		esac
	done
	if [[ $finished == true ]]; then
		echo "Exiting"
		break
	fi

	# Copy the algorithm template file to the destination file.
	templPath="./templates"
	genPath="./generated"
	cp ${templPath}/${algoName}.gtmp ${genPath}/${algoName}.gsql;

	# Replace *graph* placeholder
	sed -i "s/\*graph\*/$grph/g" ${genPath}/${algoName}.gsql
	
	echo; echo "Available vertex and edge types:"
	gsql -g $grph ls|grep '\- VERTEX\|\DIRECTED EDGE'
	echo
	echo "Please enter the vertex type(s) and edge type(s) for running ${algo}."
	echo "   Use commas to separate multiple types [ex: type1, type2]"
	echo "   Leaving this blank will select all available types"
	echo " Similarity algorithms only take single vertex type"
	echo

	# 3. Ask for vertex types. Replace *vertex-types* placeholder. For similarity algos, only take one vertex type.
	read -p 'Vertex types: ' vts
	vts=${vts//[[:space:]]/}
	if [[ $algoName == similarity* ]]; then #[ "${algoName}" == "similarity_cos_single" ] || [ "${algoName}" == "similarity_cos" ] || [ "${algoName}" == "similarity_jaccard" ] || [ "${algoName}" == "similarity_jaccard_single" ]; then
		sed -i "s/\*vertex-types\*/$vts/g" ${genPath}/${algoName}.gsql
	elif [ "${vts}" == "" ]; then
		vts="ANY"
	else
		vts=${vts/,/.*, }   # replace the delimiter
		vts="${vts}.*"
	fi
	sed -i "s/\*vertex-types\*/$vts/g" ${genPath}/${algoName}.gsql
	

	# 4. Ask for edge types. Replace *edge-types* placeholder.
	read -p 'Edge types: ' egs
	egs=${egs//[[:space:]]/}

	#Outdegree() and neighbors() processing
	edgeFuncProc(){
		OIFS=$IFS
		IFS=','
		egsinp=$egs
		outshold=($egsinp)
		if [ "${outshold}" != "" ]
			then
				outs="${4}.${1}(\"${outshold}\")"
		else
			outs="${4}.${1}()"
		fi
		for x in $egsinp
		do
			if [ "${x}" != "${outshold}" ]
				then
					outs="${outs} ${3} ${4}.${1}(\"$x\")"
			fi
		done
		sed -i "s/\*${2}\*/$outs/g" ${genPath}/${algoName}.gsql
		IFS=$OIFS
	}

	# Why not run this for all algorithms? Then we don't have to keep CASEs up to date.
	#case $algoName in
	#	"pageRank" )
	#	edgeFuncProc outdegree s_outdegrees + s;;
	#	"tri_count" )
		edgeFuncProc neighbors s_neighbors UNION s;
		edgeFuncProc neighbors t_neighbors UNION t;
	#	"tri_count_fast" )
		edgeFuncProc outdegree s_outdegrees + s;
		edgeFuncProc outdegree t_outdegrees + t;
	#esac

	if [[ $egs = *","* ]]; then
		egs=${egs/,/|}
		egs="(${egs})"
	fi
	sed -i "s/\*edge-types\*/$egs/g" ${genPath}/${algoName}.gsql

	# 4.2 Ask for reverse edge type for similarity algos. 
        if [[ ${algoName} == similarity* ]]; then
		read -p 'Reverse Edge type: ' reveg
        	reveg=${reveg//[[:space:]]/}
		sed -i "s/\*rev-edge-types\*/$reveg/g" ${genPath}/${algoName}.gsql
	fi


     	# 5. Ask for edge weight name. Replace *edge-weight* placeholder.
	if [ "${algoName}" == "shortest_ss_pos_wt" ] || [ "${algoName}" == "shortest_ss_any_wt" ]; then
		while true; do
                	read -p "Edge attribute that stores FLOAT weight:"  weight
			if [[ $(countEdgeAttr $weight) > 0 ]]; then
				sed -i "s/\*edge-weight\*/$weight/g" ${genPath}/${algoName}.gsql
				break;
			else
				echo " *** Edge attribute name not found. Try again."
			fi
		done
        fi

        if [ "${algoName}" == "similarity_cos_single" ] || [ "${algoName}" == "similarity_cos" ]; then
        	while true; do
	        	read -p "Edge attribute that stores FLOAT weight, leave blank if no such attribute:"  weight
                        weight=${weight//[[:space:]]/}
                        if [ "${weight}" == "" ]; then   #when there is no weight attribute, use uniform weight
				sed -i "s/e\.\*edge-weight\*/1/g" ${genPath}/${algoName}.gsql
				break; 
			elif [[ $(countEdgeAttr $weight) > 0 ]]; then   #when there is the weight attribute
                                sed -i "s/\*edge-weight\*/$weight/g" ${genPath}/${algoName}.gsql
				break;
			else
                                echo " *** Edge attribute name not found. Try again."
                        fi
		done
        fi

: <<'END'
	# 6. Drop queries and subqueries in order
	gsql -g $grph "DROP QUERY ${algoName}"
	gsql -g $grph "DROP QUERY ${algoName}$fExt"
	gsql -g $grph "DROP QUERY ${algoName}$aExt"
	# Search for subqueries and drop them
	# DOESN'T YET WORK FOR MULTIPLE SUBQUERIES BELONGING TO ONE MAIN QUERY
	subqueryClue="\*SUB\* CREATE QUERY"
	subqueryLine=$(grep "$subqueryClue" ${genPath}/${algoName}.gsql)
	if [[ $(grep -c "$subqueryClue" ${genPath}/${algoName}.gsql) > 0 ]]; then
		subqueryWords=( $subqueryLine )
		gsql -g $grph "DROP QUERY ${subqueryWords[3]}"
	fi
END
	
###################################################
# 7. Create up to 3 versions of the algorithm:
# ${algoName}      produces JSON output
# ${algoName}$fExt writes output to a file
# ${algoName}$aExt saves output to graph attribute (if they exist)

	echo; echo "Please choose a way to show result:"
        select version in "Show JSON result" "Write to File" "Save to Attribute/Insert Edge"; do
        	case $version in
        		"Show JSON result" )
				sed -i 's/\*EXT\*//g' ${genPath}/${algoName}.gsql;
				sed -i '/^\*ATTR\*/ d' ${genPath}/${algoName}.gsql;  # Delete lines with *ATTR*
                                sed -i 's/\*ACCM\*//g' ${genPath}/${algoName}.gsql;  # Cut the #ACCM# string
                                sed -i '/^\*FILE\*/ d' ${genPath}/${algoName}.gsql; # Delete lines with *FILE*
				gsql -g $grph "DROP QUERY ${algoName}"
				subqueryClue="\*SUB\* CREATE QUERY"
				subqueryLine=$(grep "$subqueryClue" ${genPath}/${algoName}.gsql)
				if [[ $(grep -c "$subqueryClue" ${genPath}/${algoName}.gsql) > 0 ]]; then
					subqueryWords=( $subqueryLine )
					gsql -g $grph "DROP QUERY ${subqueryWords[3]}"
				fi
                		# Finalize the JSON (ACCMulator) version of the query
        			sed -i 's/\*SUB\*//g' ${genPath}/${algoName}.gsql;   # Cut the *SUB* string
        			echo; echo "gsql -g $grph ${genPath}/${algoName}.gsql"
        			gsql -g $grph ${genPath}/${algoName}.gsql
                                break;;
			
			"Write to File" )
				# Finalize the FILE output version of the query
				mv ${genPath}/${algoName}.gsql ${genPath}/${algoName}$fExt.gsql;
				# Check if this algorithm has *FILE*
				if [[ $(grep -c "\*FILE\*" ${genPath}/${algoName}$fExt.gsql) == 0 ]]; then
                			rm ${genPath}/${algoName}$fExt.gsql
        			else
                			sed -i "s/\*EXT\*/$fExt/g" ${genPath}/${algoName}$fExt.gsql;  # *EXT* -> $fExt
					sed -i '/^\*ATTR\*/ d' ${genPath}/${algoName}$fExt.gsql; # Del *ATTR* lines
					sed -i '/^\*ACCM\*/ d' ${genPath}/${algoName}$fExt.gsql; # Del *ACCM* lines
					sed -i 's/\*FILE\*//g' ${genPath}/${algoName}$fExt.gsql; # Cut *FILE* string
					gsql -g $grph "DROP QUERY ${algoName}$fExt"
					subqueryClue="\*SUB\* CREATE QUERY"
					subqueryLine=$(grep "$subqueryClue" ${genPath}/${algoName}$fExt.gsql)
					if [[ $(grep -c "$subqueryClue" ${genPath}/${algoName}$fExt.gsql) > 0 ]]; then
						subqueryWords=( $subqueryLine )
						gsql -g $grph "DROP QUERY ${subqueryWords[3]}"
					fi

					sed -i 's/\*SUB\*//g' ${genPath}/${algoName}$fExt.gsql;   # Cut the *SUB* string
					echo; echo gsql -g $grph ${genPath}/${algoName}$fExt.gsql
					gsql -g $grph ${genPath}/${algoName}$fExt.gsql
				fi
				break;;
			"Save to Attribute/Insert Edge" )	
				mv ${genPath}/${algoName}.gsql ${genPath}/${algoName}$aExt.gsql;
				# Check if this algorithm has *ATTR*
				if [[ $(grep -c "\*ATTR\*" ${genPath}/${algoName}$aExt.gsql) == 0 ]]; then
					rm ${genPath}/${algoName}$aExt.gsql
				else
				  # Finalize the ATTR version of the query
				  echo; echo "If your graph schema has appropriate vertex or edge attributes,"
				  echo " you can update the graph with your results."
				  read -p 'Do you want to update the graph [yn]? ' updateG

				  case $updateG in [Yy]*)
					attrQuery=${genPath}/${algoName}$aExt.gsql
					# *vIntType*
					if [[ $(countStringInFile "\*vIntAttr\*" $attrQuery) > 0 ]]; then
					  while true; do
						read -p "Vertex attribute to store INT result (e.g. component ID): " vIntAttr
						if [[ $(countVertexAttr $vIntAttr) > 0 ]]; then
							sed -i "s/\*vIntAttr\*/$vIntAttr/g" ${genPath}/${algoName}$aExt.gsql;
							break;
						else
							echo " *** Vertex attribute name not found. Try again."
						fi
					  done
					fi

					# * vFltType*
					if [[ $(countStringInFile "\*vFltAttr\*" $attrQuery) > 0 ]]; then
					  while true; do
						read -p "Vertex attribute to store FLOAT result (e.g. pageRank): " vFltAttr
						if [[ $(countVertexAttr $vFltAttr) > 0 ]]; then
							sed -i "s/\*vFltAttr\*/$vFltAttr/g" $attrQuery;
							break;
						else
							echo " *** Vertex attribute name not found. Try again."
						fi
					  done
					fi

					# * vStrType*
					if [[ $(countStringInFile "\*vStrAttr\*" $attrQuery) > 0 ]]; then
					  while true; do
						read -p "Vertex attribute to store STRING result (e.g. path desc): " vStrAttr
						if [[ $(countVertexAttr $vStrAttr) > 0 ]]; then
							sed -i "s/\*vStrAttr\*/$vStrAttr/g" $attrQuery;
							break;
						else
							echo " *** Vertex attribute name not found. Try again."
						fi
					  done
					fi
					
					# edge to insert for similarity algorithms
					if [[ $(countStringInFile "\*insert-edge-name\*" $attrQuery) > 0 ]]; then
					  while true; do
                                                read -p "Name of the edge to insert and store FLOAT result (e.g. insert \"similarity\" edge with one FLOAT attribute called \"score\"): " edgeName
                                                if [[ $(ifEdgeType $edgeName) > 0 ]]; then
                                                        sed -i "s/\*insert-edge-name\*/$edgeName/g" $attrQuery;
                                                        break;
                                                else
                                                        echo " *** Edge not found. Try again."
                                                fi
                                          done
					fi
					sed -i "s/\*EXT\*/$aExt/g" $attrQuery; # *EXT* > $aExt
					sed -i 's/\*ATTR\*//g' $attrQuery;  # Cut *ATTR* string
					sed -i '/^\*ACCM\*/ d' $attrQuery;  # Del *ACCM* lines
					sed -i '/^\*FILE\*/ d' $attrQuery;  # Del *FILE*lines
					gsql -g $grph "DROP QUERY ${algoName}$aExt"
					subqueryClue="\*SUB\* CREATE QUERY"
					subqueryLine=$(grep "$subqueryClue" ${genPath}/${algoName}$aExt.gsql)
					if [[ $(grep -c "$subqueryClue" ${genPath}/${algoName}$aExt.gsql) > 0 ]]; then
						subqueryWords=( $subqueryLine )
						gsql -g $grph "DROP QUERY ${subqueryWords[3]}"
					fi				
					sed -i 's/\*SUB\*//g' ${genPath}/${algoName}$aExt.gsql;   # Cut the *SUB* string
					echo gsql -g $grph $attrQuery;
					gsql -g $grph $attrQuery;
				  ;;
				  esac
				fi
				break;;
                        * )
                                echo "Not a valid choice. Try again."
                                ;;
                esac
        done

	echo "Created the following algorithms:"
	gsql -g $grph ls | grep $algoName
	echo
done
rm schema.$grph

# 8. Install the queries
read -p "Algorithm files have been created. Do want to install them now [yn]? " doInstall
case $doInstall in
	[Yy] )
		gsql -g $grph INSTALL QUERY ALL
		;;
	* )
		exit;;
esac
