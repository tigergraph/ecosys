using System;
using System.IO;
using System.Diagnostics;
using System.Threading;
using System.Configuration;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Gremlin.Net;
using Gremlin.Net.Driver.Exceptions;
using Gremlin.Net.Driver;
using Newtonsoft.Json;
using Gremlin.Net.Structure.IO.GraphSON;


namespace khop
{
    /// <summary>
    /// Based on Sample program that shows how to get started with the Graph (Gremlin) APIs for Azure Cosmos DB using the open-source connector Gremlin.Net
    /// </summary>
    class Program
    {
        // Azure Cosmos DB Configuration variables
        // Replace the values in these variables to your own.
        private static readonly string hostname = ConfigurationManager.AppSettings["HostName"];
        private static readonly int port = int.Parse(ConfigurationManager.AppSettings["PortNumber"]);
        private static readonly string authKey = ConfigurationManager.AppSettings["AuthorizationKey"];
        private static readonly string database = ConfigurationManager.AppSettings["DatabaseName"];
        private static readonly string collection = ConfigurationManager.AppSettings["CollectionName"];
        private static readonly int depth = int.Parse(ConfigurationManager.AppSettings["Depth"]);
        private static readonly int timeout = int.Parse(ConfigurationManager.AppSettings["TimeOutMilliseconds"]);
        private static readonly string seedFile = @"" + ConfigurationManager.AppSettings["SeedFile"] + "";
        private static readonly System.IO.StreamWriter fileResult = new System.IO.StreamWriter(@"c: \Users\cosmos\Documents\khop\result\khopResults_"+collection+"_"+depth+".txt", true);
        private static readonly GremlinServer gremlinServer = new GremlinServer(hostname, port, enableSsl: true,
                                                   username: "/dbs/" + database + "/colls/" + collection,
                                                   password: authKey);
        private static readonly GremlinClient gremlinClient = new GremlinClient(gremlinServer, new GraphSON2Reader(), new GraphSON2Writer(), GremlinClient.GraphSON2MimeType);

        static void Main(string[] args)
        {
            
            ExecuteTaskAsync().Wait();

        }

        public static async Task ExecuteTaskAsync()
        {
           

            // get random seeds from seed file
            string[] roots = File.ReadAllText(seedFile).TrimEnd().Split(null);

            int totalQuery = 0;
            int errorQuery = 0;
            int otherError = 0;
            long totalSize = 0;
            long totalTime = 0;

            fileResult.WriteLine("k-hop query with depth = {0}", depth);
            fileResult.WriteLine("start vertex,\tneighbor size,\tquery time (in ms)");
            fileResult.Flush();
            foreach (var root in roots)
            {
                if (depth > 2 && totalQuery == 10) break;
                
                string query = "g.V(['" + root + "', '" + root + "']).repeat(out ()).times(" + depth + ").dedup().count()";
                totalQuery++;

                Console.WriteLine(String.Format("Running khop for seed: {0}", root));
                var token = new CancellationTokenSource(timeout);
                long[] result = new long[2];
                try
                {
                    result = await Task.Run(() => gremlinTask(query, token), token.Token);
                    Console.WriteLine("FINISHED");
                }
                catch (OperationCanceledException)
                {
                    token.Cancel();
                    errorQuery++;
                    result = new long[] { -1, -1 };
                    Console.WriteLine("TIMEOUT");
         
                }
                finally {
                    token.Dispose();
                }

                

                if (result[1] > 0)
                {
                    totalSize += result[0];
                    totalTime += result[1];
                }

                // count queries failed due to other errors
                if (result[1] == 0)
                    otherError++;

                Console.WriteLine(String.Format("Result:{0} and ElapsedTime, ms:{1}", result[0], result[1]));
                Console.WriteLine();

                fileResult.WriteLine(root + ",\t" + result[0] + ",\t" + result[1]);
                fileResult.Flush();

                
            }

            double avgSize = (errorQuery == totalQuery)|| (otherError == totalQuery) ? -1.0 : totalSize / (totalQuery - errorQuery - otherError);
            double avgTime = (errorQuery == totalQuery) || (otherError == totalQuery) ? -1.0 : totalTime / (totalQuery - errorQuery - otherError);

            Console.WriteLine("===================SUMMARY=================================");
            Console.WriteLine("Total " + depth + "-hop Neighborhood size: " + totalSize);
            Console.WriteLine("Total elapsed time, ms: " + totalTime);
            Console.WriteLine("Total number of queries: " + totalQuery);
            Console.WriteLine("Number of timeout queries: " + errorQuery);
            Console.WriteLine("Number of failed queries: " + otherError);
            Console.WriteLine("Average " + depth + "-hop Neighborhood size: " + avgSize);
            Console.WriteLine("Average query time, ms: " + avgTime);

            fileResult.WriteLine("===================SUMMARY=================================");
            fileResult.WriteLine("Total " + depth + "-hop Neighborhood size: " + totalSize);
            fileResult.WriteLine("Total elapsed time, ms: " + totalTime);
            fileResult.WriteLine("Total number of queries: " + totalQuery);
            fileResult.WriteLine("Number of timeout queries: " + errorQuery);
            fileResult.WriteLine("Number of failed queries: " + otherError);
            fileResult.WriteLine("Average " + depth + "-hop Neighborhood size: " + avgSize);
            fileResult.WriteLine("Average query time, ms: " + avgTime);
            fileResult.Flush();

            // Exit program
            Console.WriteLine("Done. Press any key to exit...");
            Console.ReadLine();
        }
        //private static Task<long> gremlinTask(string query)
        private static long[] gremlinTask(string query, CancellationTokenSource token)
        {
            //Task<long[]> task = null;

            Stopwatch stopwatch = new Stopwatch();

            stopwatch.Start();
            var gtask = gremlinClient.SubmitWithSingleResultAsync<long>(query);
            
            try
            {
               
                gtask.Wait(token.Token);
                stopwatch.Stop();
                return new long[] { gtask.Result, stopwatch.ElapsedMilliseconds };

            }
            catch (OperationCanceledException)
            {
               
                token.Token.ThrowIfCancellationRequested();
            }
            catch (AggregateException aggregateException)
            {
                if (aggregateException.InnerException.GetType() == typeof(ResponseException))
                {
                    var docExcep = aggregateException.InnerException as ResponseException;
                    Console.WriteLine("Type: {0}", aggregateException.InnerException.GetType());
                    Console.WriteLine("Request Rate Too Large Exception");


                }
                else
                {
                    foreach (Exception ex in aggregateException.InnerExceptions)
                    {
                        Console.WriteLine(ex.ToString());

                    }

                }
               

            }
           

            return new long[2];

        }

    }
}
