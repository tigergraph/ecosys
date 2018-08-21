const pregel = require("@arangodb/pregel")
var graph_module = require("@arangodb/general-graph")
var fs = require("fs");

if (graph_module._exists("graph500")){
  graph_module._drop("graph500")
}

var graph = graph_module._create("graph500");

graph._addVertexCollection("vertex");
var rel = graph_module._relation("edge", ["vertex"], ["vertex"]);
graph._extendEdgeDefinitions(rel);

var totalTime = 0;
var total = 0;

for (var i=0; i<3; i++) {
  var handle = pregel.start("connectedcomponents", "graph500", {resultField:"wcc"});

  total++;

print("computing wcc: Test # " + total);  
while (!["done", "canceled"].includes(pregel.status(handle).state)) {
    print("waiting to complete... Test # " + total);
    require("internal").wait(0.5); 
  }

 
 var status = pregel.status(handle);
 print(status);
 if (status.state == "done") {
   totalTime += status.totalRuntime;
   fs.writeFileSync("wccResults_graph500_" + i, "Test#" + i + ", " + status.totalRuntime+" s"); 
 }


}


print("SUMMARY WCC for graph500:");
print("Total time, s: " + totalTime);
print("Total number of tests: " + total);
print("Avg time, s: ", totalTime/total);
fs.writeFileSync("wccResults_graph500", "Avg time, s: " + totalTime/total);
