before running query, replace the following parameters in kn.java
	"your-neptune-endpoint" from static String URL
	"your-result-file-path" from String resultFilePath
	static String depth: 1 or 2
	"twitter-test-seed-path" from File file

compile kn.java by using the following command line:
	javac -cp json.jar kn.java
run kn.java by using the following command line: 
	nohup java -cp "json.jar:." kn &
