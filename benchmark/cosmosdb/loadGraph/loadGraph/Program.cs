//------------------------------------------------------------
// Copyright (c) Microsoft Corporation.  All rights reserved.
//------------------------------------------------------------

namespace loadGraph
{
    using System;
    using System.Configuration;
    using System.Diagnostics;
    using System.Threading;
    using System.Threading.Tasks;

    using Microsoft.Azure.Documents;
    using Microsoft.Azure.Documents.Client;
    using Microsoft.Azure.CosmosDB.BulkExecutor;
    using Microsoft.Azure.CosmosDB.BulkExecutor.BulkImport;
    using Microsoft.Azure.CosmosDB.BulkExecutor.Graph;
    using Microsoft.Azure.CosmosDB.BulkExecutor.Graph.Element;
    using System.Collections.Generic;
    using System.Collections;

    class Program
    {
        private static readonly string EndpointUrl = ConfigurationManager.AppSettings["EndPointUrl"];
        private static readonly string AuthorizationKey = ConfigurationManager.AppSettings["AuthorizationKey"];
        private static readonly string DatabaseName = ConfigurationManager.AppSettings["DatabaseName"];
        private static readonly string CollectionName = ConfigurationManager.AppSettings["CollectionName"];
        private static readonly int CollectionThroughput = int.Parse(ConfigurationManager.AppSettings["CollectionThroughput"]);
        private static readonly System.IO.StreamReader file = new System.IO.StreamReader(@"" + ConfigurationManager.AppSettings["InputFile"] + "");
        private static readonly System.IO.StreamWriter fileOut = new System.IO.StreamWriter(@"c: \Users\cosmos\Documents\loadGraph\loadSummary.txt", true);
        private static readonly ConnectionPolicy ConnectionPolicy = new ConnectionPolicy
        {
            ConnectionMode = ConnectionMode.Direct,
            ConnectionProtocol = Protocol.Tcp
        };

        private DocumentClient client;
        private dynamic verticesOrEdges;

        private Program(DocumentClient client)
        {
            this.client = client;
        }

        public static void Main(string[] args)
        {
            Trace.WriteLine("Summary:");
            Trace.WriteLine("--------------------------------------------------------------------- ");
            Trace.WriteLine(String.Format("Endpoint: {0}", EndpointUrl));
            Trace.WriteLine(String.Format("Collection : {0}.{1}", DatabaseName, CollectionName));
            Trace.WriteLine("--------------------------------------------------------------------- ");
            Trace.WriteLine("");

            try
            {
                using (var client = new DocumentClient(
                    new Uri(EndpointUrl),
                    AuthorizationKey,
                    ConnectionPolicy))
                {
                    var program = new Program(client);
                    program.RunBulkImportAsync().Wait();
                }
            }
            catch (AggregateException e)
            {
                Trace.TraceError("Caught AggregateException in Main, Inner Exception:\n" + e);
                Console.ReadKey();
            }

        }

        /// <summary>
        /// Driver function for bulk import.
        /// </summary>
        /// <returns></returns>
        private async Task RunBulkImportAsync()
        {
            // Cleanup on start if set in config.

            DocumentCollection dataCollection = null;
            try
            {
                if (bool.Parse(ConfigurationManager.AppSettings["ShouldCleanupOnStart"]))
                {
                    Database database = Utils.GetDatabaseIfExists(client, DatabaseName);
                    if (database != null)
                    {
                        await client.DeleteDatabaseAsync(database.SelfLink);
                    }

                    Trace.TraceInformation("Creating database {0}", DatabaseName);
                    database = await client.CreateDatabaseAsync(new Database { Id = DatabaseName });

                    Trace.TraceInformation(String.Format("Creating collection {0} with {1} RU/s", CollectionName, CollectionThroughput));
                    dataCollection = await Utils.CreatePartitionedCollectionAsync(client, DatabaseName, CollectionName, CollectionThroughput);
                }
                else
                {
                    dataCollection = Utils.GetCollectionIfExists(client, DatabaseName, CollectionName);
                    if (dataCollection == null)
                    {
                        throw new Exception("The data collection does not exist");
                    }
                }
            }
            catch (Exception de)
            {
                Trace.TraceError("Unable to initialize, exception message: {0}", de.Message);
                throw;
            }

            // Prepare for bulk import.

            // Creating documents with simple partition key here.
            string partitionKeyProperty = dataCollection.PartitionKey.Paths[0].Replace("/", "");
            int batchSize = int.Parse(ConfigurationManager.AppSettings["BatchSize"]);
            long fileSize = long.Parse(ConfigurationManager.AppSettings["FileSize"]);
       

            // Set retry options high for initialization (default values).
            client.ConnectionPolicy.RetryOptions.MaxRetryWaitTimeInSeconds = 30;
            client.ConnectionPolicy.RetryOptions.MaxRetryAttemptsOnThrottledRequests = 9;

            IBulkExecutor graphbulkExecutor = new GraphBulkExecutor(client, dataCollection);
            await graphbulkExecutor.InitializeAsync();

            // Set retries to 0 to pass control to bulk executor.
            client.ConnectionPolicy.RetryOptions.MaxRetryWaitTimeInSeconds = 0;
            client.ConnectionPolicy.RetryOptions.MaxRetryAttemptsOnThrottledRequests = 0;

            var tokenSource = new CancellationTokenSource();
            var token = tokenSource.Token;

            BulkImportResponse verticesOrEdgesResponse = null;
            long counter = 0;
            int batch = 0;
            string line;
            ArrayList items = new ArrayList();
            double totalTime = 0;
            double totalRU = 0;
            long totalDocsImported = 0; 

            // Read file line by line
            while ((line = file.ReadLine()) != null)
            {
                counter++;
                string[] split = line.Split('\t');
                items.Add(split[0]);
                if (split.Length == 2) {
                    items.Add(split[1]);
                }

                // send batch after reading every 500 000 lines
                    if (((counter % batchSize == 0) ||(counter == fileSize)) && counter != 0)
                {
                    if (bool.Parse(ConfigurationManager.AppSettings["VertexInsert"]))
                    {
                        verticesOrEdges = Utils.GenerateVertices(items);

                    }
                    else
                    {
                        verticesOrEdges = Utils.GenerateEdges(items);
                    }

                    batch++;

                    try
                    {
                        verticesOrEdgesResponse = await graphbulkExecutor.BulkImportAsync(
                                verticesOrEdges,
                                enableUpsert: true,
                                disableAutomaticIdGeneration: true,
                                maxConcurrencyPerPartitionKeyRange: null,
                                maxInMemorySortingBatchSize: null,
                                cancellationToken: token);

                    }
                    catch (DocumentClientException de)
                    {
                        Trace.TraceError("Document client exception: {0}", de);
                    }
                    catch (Exception e)
                    {
                        Trace.TraceError("Exception: {0}", e);
                    }

                    totalTime += verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds;
                    totalDocsImported += verticesOrEdgesResponse.NumberOfDocumentsImported;
                    totalRU += verticesOrEdgesResponse.TotalRequestUnitsConsumed;

                    Console.WriteLine("\nSummary for batch {0}", batch);
                    Console.WriteLine("--------------------------------------------------------------------- ");
                    Console.WriteLine(
                        "Inserted {0} graph elements @ {1} writes/s, {2} RU/s in {3} sec)",
                        verticesOrEdgesResponse.NumberOfDocumentsImported,
                        Math.Round(
                            (verticesOrEdgesResponse.NumberOfDocumentsImported) /
                            (verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds)),
                        Math.Round(
                            (verticesOrEdgesResponse.TotalRequestUnitsConsumed) /
                            (verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds)),
                        verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds);
                    Console.WriteLine(
                        "Average RU consumption per insert: {0}",
                        (verticesOrEdgesResponse.TotalRequestUnitsConsumed) /
                        (verticesOrEdgesResponse.NumberOfDocumentsImported));
                    Console.WriteLine("---------------------------------------------------------------------\n ");
                    fileOut.WriteLine("\nSummary for batch {0}", batch);
                    fileOut.WriteLine("--------------------------------------------------------------------- ");
                    fileOut.WriteLine(
                        "Inserted {0} graph elements @ {1} writes/s, {2} RU/s in {3} sec)",
                        verticesOrEdgesResponse.NumberOfDocumentsImported,
                        Math.Round(
                            (verticesOrEdgesResponse.NumberOfDocumentsImported) /
                            (verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds)),
                        Math.Round(
                            (verticesOrEdgesResponse.TotalRequestUnitsConsumed) /
                            (verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds)),
                        verticesOrEdgesResponse.TotalTimeTaken.TotalSeconds);
                    fileOut.WriteLine(
                        "Average RU consumption per insert: {0}",
                        (verticesOrEdgesResponse.TotalRequestUnitsConsumed) /
                        (verticesOrEdgesResponse.NumberOfDocumentsImported));
                    fileOut.WriteLine("---------------------------------------------------------------------\n ");
                    fileOut.Flush();

                    if (verticesOrEdgesResponse.BadInputDocuments.Count > 0)
                    {
                        using (System.IO.StreamWriter file = new System.IO.StreamWriter(@".\FailedToInsert.txt", true))
                        {
                            foreach (object doc in verticesOrEdgesResponse.BadInputDocuments)
                            {
                                file.WriteLine(doc);
                            }
                        }

                    }

                    items.Clear();
                }
                //counter++;
            }
            file.Close();
            Console.WriteLine("TOTAL SUMMARY:");
            Console.WriteLine("Number of lines read from file: {0}", counter);
            Console.WriteLine("Number of batches submitted: {0}", batch);
            Console.WriteLine("Total number of documents imported: {0}", totalDocsImported);
            Console.WriteLine("Total time taken, sec: {0}", totalTime);
            Console.WriteLine("Average number of document writes/s: {0}", Math.Round(totalDocsImported/totalTime));
            Console.WriteLine("Average RU/s: {0}", Math.Round(totalRU/totalTime));
            Console.WriteLine("Average RU consumption per insert: {0}", Math.Round(totalRU/totalDocsImported));

            fileOut.WriteLine("TOTAL SUMMARY:");
            fileOut.WriteLine("Number of lines read from file: {0}", counter);
            fileOut.WriteLine("Number of batches submitted: {0}", batch);
            fileOut.WriteLine("Total number of documents imported: {0}", totalDocsImported);
            fileOut.WriteLine("Total time taken, sec: {0}", totalTime);
            fileOut.WriteLine("Average number of document writes/s: {0}", Math.Round(totalDocsImported / totalTime));
            fileOut.WriteLine("Average RU/s: {0}", Math.Round(totalRU / totalTime));
            fileOut.WriteLine("Average RU consumption per insert: {0}", Math.Round(totalRU / totalDocsImported));
            fileOut.Flush();
            fileOut.Close();

            // Cleanup on finish if set in config.
            if (bool.Parse(ConfigurationManager.AppSettings["ShouldCleanupOnFinish"]))
            {
                Trace.TraceInformation("Deleting Database {0}", DatabaseName);
                await client.DeleteDatabaseAsync(UriFactory.CreateDatabaseUri(DatabaseName));
            }

            Trace.WriteLine("\nPress any key to exit.");
            Console.ReadKey();
        }
    }
}
