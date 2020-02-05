Install Kafka Connect steps
1) change settings of tigergraphSinkConnector.properties:
  a) Replace "bootstrap.servers=localhost:30008" with the correct list of Kafka brokers in the form of "IP1:port1, IP2:port2".
  b) Replace "plugin.path=/root/cluster_run/plugins" with current path of cluster_run folder.
  c) Replace "rest.port=8083" if 8083 is already occupied. start_connect_workers.sh will use this port as base port and increment by 1 for each additional worker. For example, worker0 will have port 8083, then worker1 will have 8084 etc.

2) Start connector workers and Tasks
  a) Start kafka connector workers, all workers will be in the same group and work distributely. One could start multiple workers on different machines.
    ./start_connector_workers.sh $/absolute/path/to/kafka/connect-distributed.sh tigergraphSinkConnector.properties $num_of_workers 
  where
    $/absolute/path/to/kafka/connect-distributed.sh: the path to kafka connect-distributed.sh script
    $num_of_workers: the number of workers needed to be started
  The code will use nohup to start the connect workers and put the pid in the pid.log file. kill_all.sh will use this file to stop all workers

  b) Create kafka connector tasks
    ./start_task.sh $task.cfg 
  where
    $task.cfg: the config file for start_task.sh, please take task.cfg as examle,
    i.e.
      topicName="MYTOPIC"   # The name of kafka topic that need to be loaded into tigergraph
      hostList="HOSTLIST"   # hostList of TigerGraph platform in the format of "ip1:port1,ip2:port2,ip3:port3"
      loadingJobName="YYYYYYY" #The loading job name for tigergraph loader
      maxTask="100"         # optional the number of max tasks, optional with default value 100
      flushBatchSize="200"  # the number of batch size for sending batch kafka messages, optional with default value 200
      restPort="8083"       #the rest port of kafka connect workers


3) Check the status of each worker, i.e. 
  tail -f connect_work$num.log
  where $num is starting form 0 to $num_of_workers - 1, each worker will have a corresponding log file to record all screen messages. 
  Current print message take the following form, i.e.
      "[2017-10-28 06:47:00,318] INFO commited: 500 messages with size = 215072 bytes, which took 66 ms, including 6 ms parsing time."
  where it tells that 500 messages were sent to tigergraph in one commit which takes total 66 ms and 6 ms was used to parse the kafak message.

To Stop Kafka connectors: ./kill_all.sh
To clear logs and pid.log: ./clear_all_logs.sh
To Restart the Task in case of error: ./restart_all.sh
To check status of tasks: ./check_status.sh
