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
	echo; echo "Please enter the number of the algorithm to install:"
	select algo in "EXIT" "Closeness Centrality" "Connected Components" "Label Propagation" "Community detection: Louvain" "PageRank" "Shortest Path, Single-Source, Any Weight" "Triangle Counting(minimal memory)" "Triangle Counting(fast, more memory)"; do
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
	cp ${templPath}/${algoName}.gtmp ${templPath}/${algoName}.gsql;

	# Replace *graph* placeholder
	sed -i "s/\*graph\*/$grph/g" ${templPath}/${algoName}.gsql
	
	echo; echo "Available vertex and edge types:"
	gsql -g $grph ls|grep '\- VERTEX\|\DIRECTED EDGE'
	echo
	echo "Please enter the vertex type(s) and edge type(s) for running ${algo}."
	echo "   Use commas to separate multiple types [ex: type1, type2]"
	echo "   Leaving this blank will select all available types"
	echo

	# 3. Ask for vertex types. Replace *vertex-types* placeholder.
	read -p 'Vertex types: ' vts
	vts=${vts//[[:space:]]/}
	if [ "${vts}" == "" ]; then
		vts="ANY"
	else
		vts=${vts/,/.*, }
		vts="${vts}.*"
	fi
	sed -i "s/\*vertex-types\*/$vts/g" ${templPath}/${algoName}.gsql

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
		sed -i "s/\*${2}\*/$outs/g" ${templPath}/${algoName}.gsql
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
	sed -i "s/\*edge-types\*/$egs/g" ${templPath}/${algoName}.gsql

	# 5. Drop queries and subqueries in order
	gsql -g $grph "DROP QUERY ${algoName}"
	gsql -g $grph "DROP QUERY ${algoName}$fExt"
	gsql -g $grph "DROP QUERY ${algoName}$aExt"
	# Search for subqueries and drop them
	# DOESN'T YET WORK FOR MULTIPLE SUBQUERIES BELONGING TO ONE MAIN QUERY
	subqueryClue="\*SUB\* CREATE QUERY"
	subqueryLine=$(grep "$subqueryClue" ${templPath}/${algoName}.gsql)
	if [[ $(grep -c "$subqueryClue" ${templPath}/${algoName}.gsql) > 0 ]]; then
		subqueryWords=( $subqueryLine )
		gsql -g $grph "DROP QUERY ${subqueryWords[3]}"
	fi
	
	
###################################################
# 6. Create up to 3 versions of the algorithm:
# ${algoName}      produces JSON output
# ${algoName}$fExt writes output to a file
# ${algoName}$aExt saves output to graph attribute (if they exist)

	cp ${templPath}/${algoName}.gsql ${templPath}/${algoName}$fExt.gsql;
	cp ${templPath}/${algoName}.gsql ${templPath}/${algoName}$aExt.gsql;
	
	# Finalize the JSON (ACCMulator) version of the query
	sed -i 's/\*EXT\*//g' ${templPath}/${algoName}.gsql;   # Cut the *EXT* string
	sed -i 's/\*SUB\*//g' ${templPath}/${algoName}.gsql;   # Cut the *SUB* string
	sed -i '/^\*ATTR\*/ d' ${templPath}/${algoName}.gsql;  # Delete lines with *ATTR*
	sed -i 's/\*ACCM\*//g' ${templPath}/${algoName}.gsql;  # Cut the #ACCM# string
	sed -i '/^\*FILE\*/ d' ${templPath}/${algoName}.gsql; # Delete lines with *FILE*
	echo; echo "gsql -g $grph ${templPath}/${algoName}.gsql"
	gsql -g $grph ${templPath}/${algoName}.gsql
	mv ${templPath}/${algoName}.gsql ${genPath}/${algoName}.gsql
	
	# Finalize the FILE output version of the query
	# Check if this algorithm has *FILE*
	if [[ $(grep -c "\*FILE\*" ${templPath}/${algoName}$fExt.gsql) == 0 ]]; then
		rm ${templPath}/${algoName}$fExt.gsql
	else
		sed -i "s/\*EXT\*/$fExt/g" ${templPath}/${algoName}$fExt.gsql;  # *EXT* -> $fExt
		sed -i '/^\*SUB\*/ d' ${templPath}/${algoName}$fExt.gsql;  # Del *SUB* lines
		sed -i '/^\*ATTR\*/ d' ${templPath}/${algoName}$fExt.gsql; # Del *ATTR* lines
		sed -i '/^\*ACCM\*/ d' ${templPath}/${algoName}$fExt.gsql; # Del *ACCM* lines
		sed -i 's/\*FILE\*//g' ${templPath}/${algoName}$fExt.gsql; # Cut *FILE* string
		echo; echo gsql -g $grph ${templPath}/${algoName}$fExt.gsql
		gsql -g $grph ${templPath}/${algoName}$fExt.gsql
		mv ${templPath}/${algoName}$fExt.gsql ${genPath}/${algoName}$fExt.gsql
	fi
		
	# Check if this algorithm has *ATTR*
	if [[ $(grep -c "\*ATTR\*" ${templPath}/${algoName}$aExt.gsql) == 0 ]]; then
		rm ${templPath}/${algoName}$aExt.gsql
	else
	  # Finalize the ATTR version of the query
	  echo; echo "If your graph schema has appropriate vertex or edge attributes,"
	  echo " you can update the graph with your results."
	  read -p 'Do you want to update the graph [yn]? ' updateG
	
	  case $updateG in [Yy]*)
	  	attrQuery=${templPath}/${algoName}$aExt.gsql
		# *vIntType*
		if [[ $(countStringInFile "\*vIntAttr\*" $attrQuery) > 0 ]]; then
		  while true; do
			read -p "Vertex attribute to store INT result (e.g. component ID): " vIntAttr
			if [[ $(countVertexAttr $vIntAttr) > 0 ]]; then
				sed -i "s/\*vIntAttr\*/$vIntAttr/g" ${templPath}/${algoName}$aExt.gsql;
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

		sed -i "s/\*EXT\*/$aExt/g" $attrQuery; # *EXT* > $aExt 
		sed -i '/^\*SUB\*/ d' $attrQuery;   # Del *SUB* lines
		sed -i 's/\*ATTR\*//g' $attrQuery;  # Cut *ATTR* string
		sed -i '/^\*ACCM\*/ d' $attrQuery;  # Del *ACCM* lines
		sed -i '/^\*FILE\*/ d' $attrQuery;  # Del *FILE*lines
		echo gsql -g $grph $attrQuery;
		gsql -g $grph $attrQuery;
		mv ${templPath}/${algoName}$aExt.gsql ${genPath}/${algoName}$aExt.gsql
	  ;;
	  esac
	fi
	
	echo "Created the following algorithms:"
	gsql -g $grph ls | grep $algoName
	echo
done
rm schema.$grph

# 7. Install the queries
read -p "Algorithm files have been created. Do want to install them now [yn]? " doInstall
case $doInstall in
	[Yy] )
		gsql -g $grph INSTALL QUERY ALL
		;;
	* )
		exit;;
esac