gdev=$(grep gdev.path ~/.gsql/gsql.cfg | cut -f 2 -d " ")
currentSec=$(date -u  +"%s")

cp ${gdev}/gdk/gsql/src/TokenBank/TokenBank.cpp  ${gdev}/gdk/gsql/src/TokenBank/TokenBank.cpp.${currentSec}
echo "Your old TokenBank.cpp has been saved to "${gdev}/gdk/gsql/src/TokenBank/TokenBank.cpp.${currentSec}
cp ./TokenBank.cpp ${gdev}/gdk/gsql/src/TokenBank/TokenBank.cpp

gsql schema.gsql

gsql -g RDF loader.gsql
gsql -g RDF run job loadRDF

gsql -g RDF getMostRelatedPerson.gsql  
gsql -g RDF getRelatedTopics.gsql  
gsql -g RDF getTheTopPublisher.gsql

gsql install query all
