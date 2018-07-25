before loading data you should know:
	1. your-neptune-endpoint: go to the Neptune instance page and it is in Details section
	2. source: s3://bucket-name/object-key-name
	3. iamRoleArn: go to the Neptune instance page and it is in Details section
modify parameters before running script
================================================================================================
1. To generate vertex file and edge file with required format, use generateVertexEdgeFile.java 
	You need to replace parameter for "File file", "String filePath", "String vertexPath",
 	and "String label" from generateVertexEdgeFile.java
2. You need to use upload.sh copy data files to Amazon S3
3. Use load.sh to load data from S3 to Neptuen, The Neptune loader returns a
 loadId that allows you to check the status or cancel the loading process;
4. Use check_load.sh to get the status of the load with the
 loadId from previous step
5. If you neep to cancel loading job, use cancel_loading.sh


