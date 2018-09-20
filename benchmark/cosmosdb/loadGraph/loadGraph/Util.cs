//------------------------------------------------------------
// Copyright (c) Microsoft Corporation.  All rights reserved.
//------------------------------------------------------------

namespace loadGraph
{
    using Microsoft.Azure.CosmosDB.BulkExecutor.Graph.Element;
    using Microsoft.Azure.Documents;
    using Microsoft.Azure.Documents.Client;

    using System;
    using System.Collections;
    using System.Collections.Generic;
    using System.Collections.ObjectModel;
    using System.Configuration;
    using System.Linq;
    using System.Threading.Tasks;

    internal sealed class Utils
    {
        /// <summary>
        /// Get the collection if it exists, null if it doesn't.
        /// </summary>
        /// <returns>The requested collection.</returns>
        public static DocumentCollection GetCollectionIfExists(DocumentClient client, string databaseName, string collectionName)
        {
            if (GetDatabaseIfExists(client, databaseName) == null)
            {
                return null;
            }

            return client.CreateDocumentCollectionQuery(UriFactory.CreateDatabaseUri(databaseName))
                .Where(c => c.Id == collectionName).AsEnumerable().FirstOrDefault();
        }

        /// <summary>
        /// Get the database if it exists, null if it doesn't.
        /// </summary>
        /// <returns>The requested database.</returns>
        public static Database GetDatabaseIfExists(DocumentClient client, string databaseName)
        {
            return client.CreateDatabaseQuery().Where(d => d.Id == databaseName).AsEnumerable().FirstOrDefault();
        }

        /// <summary>
        /// Create a partitioned collection.
        /// </summary>
        /// <returns>The created collection.</returns>
        public static async Task<DocumentCollection> CreatePartitionedCollectionAsync(DocumentClient client, string databaseName,
            string collectionName, int collectionThroughput)
        {
            PartitionKeyDefinition partitionKey = new PartitionKeyDefinition
            {
                Paths = new Collection<string> { $"/{ConfigurationManager.AppSettings["CollectionPartitionKey"]}" }
            };
            DocumentCollection collection = new DocumentCollection { Id = collectionName, PartitionKey = partitionKey };

            try
            {
                collection = await client.CreateDocumentCollectionAsync(
                    UriFactory.CreateDatabaseUri(databaseName),
                    collection,
                    new RequestOptions { OfferThroughput = collectionThroughput });
            }
            catch (Exception e)
            {
                throw e;
            }

            return collection;
        }

        public static IEnumerable<GremlinEdge> GenerateEdges(ArrayList edges)
        {
            for (int i = 0; i < edges.Count; i+= 2)
            {
                GremlinEdge e = new GremlinEdge(
                    "e" + edges[i]+edges[i+1],
                    "edge",
                    edges[i].ToString(),
                    edges[i+1].ToString(),
                    "vertex",
                    "vertex",
                    edges[i],
                    edges[i+1]);

                yield return e;
            }
        }

        public static IEnumerable<GremlinVertex> GenerateVertices(ArrayList vertices)
        {
            // CosmosDB currently doesn't support documents with id length > 1000
            //GremlinVertex vBad = new GremlinVertex(getLongId(), "vertex");
            //vBad.AddProperty(ConfigurationManager.AppSettings["CollectionPartitionKey"], 0);
           // yield return vBad;

            foreach (String vertex in vertices)
            {
                GremlinVertex v = new GremlinVertex(vertex, "vertex");
                v.AddProperty(ConfigurationManager.AppSettings["CollectionPartitionKey"], vertex);
                yield return v;
            }
        }

        private static string getLongId()
        {
            return new string('1', 2000);
        }
    }
}
