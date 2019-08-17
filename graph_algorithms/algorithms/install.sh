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


echo "GSQL Algorithm Library is moved to: https://github.com/tigergraph/gsql-graph-algorithms" 
