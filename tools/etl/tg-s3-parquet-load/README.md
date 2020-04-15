# Spark Loading

This program uses Spark SQL to load parquet from Amazon S3 and store as CSV locally or in S3.

### Structure
1. aws_jar: AWS java libraries, Spark imports those jars to access AWS S3
2. graphsql: java package folder
3. run.sh: bash script to run the program
4. s3.property: config file, require AWS credentials, bucket name, source and target folder.

### Setup
You need to install OpenJdk 8, Scala(>= 2.11.7), Spark(>= 2.0.0) into all machines in cluster.
1. Install Java 8
    ```sh
    sudo apt-get install default-jdk -y
    ```
2. Install Scala
    ```sh
    sudo apt-get install scala -y or sudo yum install scala -y
    ```
3. Install Spark 2.2.1
    ```sh
    wget http://apache.claz.org/spark/spark-2.2.1/spark-2.2.1-bin-hadoop2.7.tgz
    tar xvzf spark-2.2.1-bin-hadoop2.7.tgz
    rm -rf spark-2.2.1-bin-hadoop2.7.tgz
    sudo mv spark-2.2.1-bin-hadoop2.7 /usr/local/spark
    sudo iptables -A INPUT -p tcp --dport 6066 -j ACCEPT
    sudo iptables -A INPUT -p tcp --dport 7077 -j ACCEPT
    sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
    sudo iptables -A INPUT -p tcp --dport 8081 -j ACCEPT
    sudo iptables-save
    echo 'export SPARK_HOME=/usr/local/spark' >> .bashrc
    echo 'export PATH=$PATH:$SPARK_HOME/bin' >> .bashrc
    source .bashrc
    spark-shell
    ```
4.  Setup cluster
    * For master machine:
      ```sh
      cd /usr/local/spark
      ./sbin/start-master.sh
      get the master node url like "spark://ip-xxx-xxx-xxx-xxx:7077"
      ./sbin/start-slave.sh $master_url
      ```
    * For slave machine:
      ```sh
      cd /usr/local/spark
      ./sbin/start-slave.sh $master_url
      ```

### Config
1. Config s3 user credentials in s3.properties file.
   * change awsAccessKey
   * change awsSecretKey

2. Config s3 bucket name, scheme folder name in s3.properties file.
   * change bucket_name. For example, "loading-test"
   * change schema_folders. The folders are split by ",". For example, "entity/legacy_device,entity/driver"
   * change restpp loading endpoint.

### Run
bash compile_and_run.sh $master_url $target_host $target_path $times $log_path
   1. $master_url is the url for Spark cluster manager
   2. $target_host
      * local (local csv mode, save as csv in local)
      * ip address of TG database (post mode, post to TG)
      * schema (schema mode, print schema and row count)
   3. $target_path is the path to store in the local machine if it is local csv mode. It must be absolute path.
   4. $times, just use 1
   5. $log_path the path that outputs the log, default is /tmp/sparkLoading/
