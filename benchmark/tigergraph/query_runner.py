############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Mingxi Wu mingxi.wu@tigergraph.com
############################################################

import requests

from neo4j.v1 import GraphDatabase, basic_auth

import config

class QueryRunner():
  def __init__(self):
    pass

  def KN(self, root):
    pass

  def SSSP(self, root):
    pass

  def PG(self):
    pass

  def WCC(self):
    pass

  def LCC(self):
    pass



class Neo4jQueryRunner(QueryRunner):
  def __init__(self, url = config.NEO4J_BOLT):
    QueryRunner.__init__(self)
    self.driver = GraphDatabase.driver(url, auth=basic_auth("neo4j", "benchmark"))
    self.session = self.driver.session()

  def KN(self, root, depth):
 
     try:
       # a path of length depth, if a node is 1-hop away, and also 2-hop away, we count it in 2-hop set.
       result = self.session.run("match (n1:MyNode)-[:MyEdge*" + str(depth) + "]->(n2:MyNode) where n1.id={root} return count(distinct n2)", {"root":root})
       record = result.peek()
     except:  #timeout, we return -1, reset session
        self.session.close()
        self.session = self.driver.session()
        return -1
     else: 
       return record["count(distinct n2)"]

  def PG(self, iteration):
      result = self.session.run("MATCH (node:MyNode) WITH COLLECT(node) AS nodes CALL apoc.algo.pageRankWithConfig(nodes,{iterations:{iteration}}) YIELD node, score RETURN node, score LIMIT 1", {"iteration":iteration})
      record = result.peek()
      return record

  def WCC(self):
      result = self.session.run("CALL apoc.algo.wcc() yield stats return count(*)")
      record = result.peek()
      return record

  #build index
  def Index(self, att, typename):
      result = self.session.run("CREATE INDEX ON :{typename}({att});", {"typename":typename, "att":att})

      record = result.peek()


class TigerGraphQueryRunner(QueryRunner):
  def __init__(self, url = config.TIGERGRAPH_HTTP):
    QueryRunner.__init__(self)
    self.session = requests.Session()
    self.url = url

  def KN(self, root, depth):
      result = self.session.get(self.url + "/query/khop", params={'start_node': root, "depth":depth}).json()
      return result["results"][0]["Start.size()"]

  def PG(self, iteration):
      result = self.session.get(self.url + "/query/pagerank", params={'iteration': iteration, "dampingFactor":0.8}).json()
      print (result)

  def WCC(self):
      result = self.session.get(self.url + "/query/wcc").json()
      print (result)

if __name__ == "__main__":
    runner = TigerGraphQueryRunner() 
    runner.PG(100)
