const pregel = require("@arangodb/pregel")
var graph_module = require("@arangodb/general-graph")
var fs = require("fs");

if (graph_module._exists("twitter")){
  graph_module._drop("twitter")
}

var graph = graph_module._create("twitter");

graph._addVertexCollection("vertex");
var rel = graph_module._relation("edge", ["vertex"], ["vertex"]);
graph._extendEdgeDefinitions(rel);

var totalTime = 0;
var total = 0;


for (var i=0; i<3; i++) {
  var handle = pregel.start("pagerank", "twitter", {maxGSS: 10, resultField:"pagerank"});
  
  total++;

print("computing pagerank: Test # " + total);  
while (!["done", "canceled"].includes(pregel.status(handle).state)) {
    print("waiting to complete... Test # " + total);
    require("internal").wait(0.5); 
  }

 
 var status = pregel.status(handle);
 print(status);
 if (status.state == "done") {
   totalTime += status.totalRuntime;
   fs.writeFileSync("pagerankResults_twitter_" + i, "Test#" + i + ", " + status.totalRuntime+" s");
 }

pregel.cancel(handle);

}

print("SUMMARY PageRank for twitter graph:");
print("Total time, s: " + totalTime);
print("Total number of tests: " + total);
print("Avg time, s: ", totalTime/total);
fs.writeFileSync("pagerankResults_twitter", "Avg time, s: " + totalTime/total);
