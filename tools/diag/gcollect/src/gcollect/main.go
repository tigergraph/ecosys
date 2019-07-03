package main

import (
  "flag"
  "fmt"
  "os"
  "io"
  "path"
  "bufio"
  "bytes"
  "os/user"
  "io/ioutil"
  "logs"
  "log"
  "sort"
  "strconv"
  "strings"
  "utils"
  "time"
  "tsar"
  "path/filepath"
  "gopkg.in/yaml.v2"
  "golang.org/x/crypto/ssh"
)

type arrayFlags []string
var supported_components string = "gpe,gse,gsql,dict,tsar,kafka,zk,rest,nginx,admin,restpp_loader,fab,kafka-stream,kafka-connect,gui"

type NodeFolders struct {
  gpe_folder, restpp_folder, restpp_loader_folder, kafka_loader_folder, gse_folder string
}

// Only logs between startEpoch and endEpoch will be printed.
var startEpoch int64 = 0
var endEpoch int64 = 0
// Number of lines trailing context after each match
var after int = 0
// Number of lines leading context after each match
var before int = 0
// Start time before query starts (in seconds)
var before_seconds int = 0
// query timeout in seconds
var duration int = 0
// regular expressions
var patterns arrayFlags
// search for patterns for the specified nodes (e.g., m1,m2,m3...)
var nodes string
var nginx_port string = "14240"
var request_id string
var output_dir string
var components string
var master_dest_dir string
// Node id: m1/m2/m3...
var node_id string
// IP Addr of local machine
var master_ip string
// wether it is running on a slave machine
var slave bool = false
var display bool = false
var external bool = false
var under_gium bool = false
var tgRootDir string = "/home/tigergraph/tigergraph"
var tgLogDir string = "/home/tigergraph/tigergraph/logs"
// Temp folder for slave nodes to store result files,
// A subdirectory of 'tgLogDir' instead of "/tmp/"
var slave_tmp_folder string = "gcollect_tmp"
// A temp file under 'tgLogDir', for printing to screen
var tg_tmp_file string = "gcollect_tmp_file"
// All the arguments without program name
// Used to invoke itself on slave nodes
var argsWithoutProg string
// Save ssh connections to slave nodes
var name2client map[string]*ssh.Client
// Used to sync with slave nodes
var sync_chan chan int
// Mapping from node id to IP address 
var name2ip map[string]string
// A map contains all the components specified to be collected.
var need_components_map map[string]int
// A map contains folders of each component on each node
var nodes_folders_map map[string]NodeFolders
var failed_nodes_array []string
var gpeServers string
var gseServers string
var kafkaServers string
var restServers string
var zkServers string
const timeFormat = "2006-01-02,15:04:05"

// Helper functions to pass multiple flags (for option "pattern")
func (i *arrayFlags) String() string {
  return "array flags."
}

func (i *arrayFlags) Set(value string) error {
  *i = append(*i, value)
  return nil
}

func usage() {
  fmt.Println("gcollect [Options] COMMAND")
  fmt.Println("")
  fmt.Println("Options:")
  fmt.Println("-h, --help                        show this help message and exit")
  fmt.Println("-A num, --after-context num       print num lines of trailing context after each match.")
  fmt.Println("-B num, --before-context num      print num lines of leading context before each match.")
  fmt.Println("-c, --components gpe,gse,rest     only collect information related to the specified component(s). All by default. Supported components: " + supported_components)
  fmt.Println("-n, --nodes m1,m2                 only search patterns for specified nodes. (only works in together with command \"grep\")")
  fmt.Println("-s, --start DateTime              logs older than this DateTime will be ignored. Format: 2006-01-02,15:04:05")
  fmt.Println("-e, --end DateTime                logs newer than this DateTime will be ignored. Format: 2006-01-02,15:04:05")
  fmt.Println("-t, --tminus num                  only search for logs that are generated in the past num seconds.")
  fmt.Println("-r, --request_id id               only collect information related to the specified request id. Lines match \"pattern\" will also be printed.")
  fmt.Println("-b, --before num                  how long before the query should we start collecting. (in seconds, could ONLY be used with [--reqest_id] option).")
  fmt.Println("-d, --duration num                how long after the query should we stop collecting. (in seconds, could ONLY be used with [--reqest_id] option).")
  fmt.Println("-o, --output_dir dir              specify the output directory, \"./output\" by default. (ALERT: files in this folder will be DELETED.)")
  fmt.Println("-p, --pattern regex               collect lines from logs which match the regular expression. (Could have more than one regex, lines that match any of the regular expressions will be printed.)")
  fmt.Println("-i, --ignore-case                 ignore case distinctions in both the PATTERN and the input files.")
  fmt.Println("-D, --display                     print to screen.")
  fmt.Println("-g                                For GraphStudio to collect logs.")
  fmt.Println("")
  fmt.Println("Commands:")
  fmt.Println("grep                              search patterns from logs files that have been collected before.")
  fmt.Println("show                              show all the requests during the specified time window.")
  fmt.Println("collect                           collect all the debugging information which satisfy all the requirements specified by Options.")
  fmt.Println("")
  fmt.Println("Examples:")
  fmt.Println("  # show all requests during the last hour")
  fmt.Println("  ./bin/gcollect -t 3600 show")
  fmt.Println("  # collect debug info for a specific request")
  fmt.Println("  ./bin/gcollect -r RESTPP_2_1.1559075028795 -b 60 -d 120 -p \"error\" collect")
  fmt.Println("  # collect debug info for all components")
  fmt.Println("  ./bin/gcollect -i -p \"error\" -p \"FAILED\" -s \"2019-05-22,18:00:00\" -e \"2019-05-22,19:00:00\" collect")
  fmt.Println("  # Search from log files that have been collected before")
  fmt.Println("  ./bin/gcollect -i -p \"unknown\" -c admin,gpe -D -A 1 -B 2 grep")
}

// Get log dir for a specific component
// component: rest/gpe/gse
// node: m1, m2, m3, ...
func getLogDir(root, component, node string) string {
  switch component {
  case "rest":
    return fmt.Sprintf("%s/%s", root, nodes_folders_map[node].restpp_folder)
  case "restpp_loader":
    return fmt.Sprintf("%s/%s", root, nodes_folders_map[node].restpp_loader_folder)
  case "gpe":
    return fmt.Sprintf("%s/%s", root, nodes_folders_map[node].gpe_folder)
  case "gse":
    return fmt.Sprintf("%s/%s", root, nodes_folders_map[node].gse_folder)
  default:
    log.Fatal("Unsupported component: ", component)
    return ""
  }
}

// Read from chan to sync on master
func syncSlaveNodes () {
  length := len(name2ip) - len(failed_nodes_array) - 1
  for i := 0; i < length; i++ {
    <- sync_chan
  }
}

// Invoke the program itself on slave nodes with slave model enabled.
// The binary must be copied to slave nodes before.
func runCmdOnSlave(parent string) {
  // Some systems may not allow to invoke a binary from "/tmp",
  // so we use tgLogDir instead.
  prefix := parent + "/gcollect --slave -O " + master_dest_dir
  for name, client := range name2client {
    if name == node_id {
      continue
    }
    cmd := prefix + " -ip " + master_ip + argsWithoutProg
    go utils.RunCmds_async(client, cmd, sync_chan)
  }
}

func collectDebugInfo() {
  // Copy files to output dir
  if !external {
    utils.LocalRun("rm -rf " + output_dir + "/catalog")
    utils.LocalRun("mkdir " + output_dir + "/catalog")
    utils.LocalRunIgnoreError("~/.gium/gadmin __pullfromdict " + output_dir + "/catalog")
    utils.LocalRunIgnoreError("~/.gium/gadmin --dump-config > " + output_dir + "/gsql.cfg")
  }

  // Get system info
  sysInfo := output_dir + "/sysInfo.txt"
  utils.LocalRun("echo '>>>>>>>>>>>>>> cat /etc/os-release <<<<<<<<<<<<<<' > " + sysInfo)
  utils.LocalRun("cat /etc/os-release >> " + sysInfo)
  utils.LocalRun("echo '\n>>>>>>>>>>>>>> gadmin version <<<<<<<<<<<<<<' >> " + sysInfo)
  utils.LocalRunIgnoreError("~/.gium/gadmin version >> " + sysInfo + " 2>&1")
  utils.LocalRun("echo '\n>>>>>>>>>>>>>> TigerGraph root Dir: " + tgRootDir + "' >> " + sysInfo)
  utils.LocalRun("echo '\n>>>>>>>>>>>>>> \"df -lh\" on m1 <<<<<<<<<<<<<<' >> " + sysInfo)
  utils.LocalRun("df -lh >> " + sysInfo)
  utils.LocalRun("echo '\n>>>>>>>>>>>>>> \"free -g\" on m1 <<<<<<<<<<<<<<' >> " + sysInfo)
  utils.LocalRun("free -g >> " + sysInfo)
  utils.LocalRun("echo '\n>>>>>>>>>>>>>> \"dmesg | grep -i oom\" on m1 <<<<<<<<<<<<<<' >> " + sysInfo)
  _, err := utils.LocalRunIgnoreError("dmesg | grep -i oom >> " + sysInfo)

  f, err := os.OpenFile(sysInfo, os.O_APPEND|os.O_WRONLY, 0600)
  if err != nil {
    panic(err)
  }

  // Get system info on clusters.
  for name, client := range name2client {
    stdout, _ := utils.RunCmds(client, "df -lh")
    str := "\n>>>>>>>>>>>>>> \"df -lh\" on " + name + " <<<<<<<<<<<<<<\n" + stdout
    if _, err = f.WriteString(str); err != nil {
      panic(err)
    }
    stdout, _ = utils.RunCmds(client, "free -g")
    str = "\n>>>>>>>>>>>>>> \"free -g\" on " + name + " <<<<<<<<<<<<<<\n" + stdout
    if _, err = f.WriteString(str); err != nil {
      panic(err)
    }
    stdout, _ = utils.RunCmdsIgnoreError(client, "dmesg | grep -i oom")
    str = "\n>>>>>>>>>>>>>> \"dmesg | grep -i oom\" on " + name + " <<<<<<<<<<<<<<\n" + stdout
    if _, err = f.WriteString(str); err != nil {
      panic(err)
    }
  }
  f.Close()

  if !external {
    utils.LocalRun("echo '\n>>>>>>>>>>>>>> Service Status History <<<<<<<<<<<<<<' >> " + sysInfo)
    m1_ip, ok := name2ip["m1"]
    if ok {
      cmd := "curl -s \"http://" + m1_ip + ":" + nginx_port + "/ts3/api/datapoints?from=" +
        strconv.FormatInt(startEpoch, 10)
      cmd += "&to=" + strconv.FormatInt(endEpoch, 10) + "&what=servicestate\" | python -m json.tool >> " + sysInfo
      utils.LocalRunIgnoreError(cmd)
    } else {
      fmt.Println("Cannot find ip address of m1.")
    }
  }

  utils.LocalRun("echo '\n>>>>>>>>>>>>>> gadmin status -v <<<<<<<<<<<<<<' >> " + sysInfo)
  // It may fail when license expired
  if external {
    // for GraphStudio, we have to wait for its execution
    // otherwise users may get partial results.
    utils.LocalRunIgnoreError("~/.gium/gadmin status -v >> " + sysInfo)
  } else {
    // "gadmin status" may needs ~20s, so we run it in the background
    go utils.LocalRunIgnoreError("~/.gium/gadmin status -v >> " + sysInfo)
  }
}

func getComponentLogFilename(component string) []string {
  var ret []string
  switch component {
  case "rest":
    ret = append(ret, "restpp.log")
    ret = append(ret, "restpp.out")
  case "gpe":
    ret = append(ret, "gpe.log")
    ret = append(ret, "gpe.out")
  case "gse":
    ret = append(ret, "gse.log")
    ret = append(ret, "gse.out")
  case "gsql":
    ret = append(ret, "gsql.log")
    ret = append(ret, "gsql.out")
  case "fab":
    ret = append(ret, "fab_cmd.log")
  case "nginx":
    ret = append(ret, "nginx.log")
  case "kafka":
    ret = append(ret, "kafka.log")
  case "tsar":
    ret = append(ret, "tsar.log")
  case "zk":
    ret = append(ret, "zk.log")
  case "dict":
    ret = append(ret, "gdict_client.log")
    ret = append(ret, "gdict_server.log")
  case "admin":
    ret = append(ret, "admin_server.log")
    ret = append(ret, "admin_server.out")
  case "kafka-stream":
    ret = append(ret, "kafka-stream.log")
  case "kafka-connect":
    ret = append(ret, "kafka-connect.log")
  case "restpp_loader":
    ret = append(ret, "restpp_loader.log")
  default:
    log.Fatal("Unsupported component: ", component)
  }
  return ret
}

func grepLogFiles() {
  if len(patterns) == 0 {
    log.Fatalf("Please specify a \"pattern\" at least.")
  }
  if !utils.CheckDirExisted(output_dir) {
    log.Fatalf("Please run \"collect\" before using \"grep\".")
  }
  // Clear & create the tmp file
  utils.LocalRun("echo -n \"\" > " + tg_tmp_file)
  for name, _ := range name2ip {
    if nodes != "" {
      start := strings.Index(nodes, name)
      if start < 0 {
        continue
      }
    }
    dir := output_dir + "/" + name
    if !utils.CheckDirExisted(dir) {
      log.Fatalf("Please running \"collect\" before using \"grep\".")
    }
    need_components := components
    if components == "" {
      need_components = supported_components
    }
    for _, str := range strings.Split(need_components, ",") {
      fileList := getComponentLogFilename(str)
      for _, filename := range fileList {
        path := dir + "/" + filename
        if utils.CheckFileExisted(path) {
          logs.GrepLogFile(path, tg_tmp_file, patterns, before, after)
        }
      }
    }
  }
  content, err := readFile(tg_tmp_file)
  if err != nil {
    log.Fatal(err)
  }
  fmt.Printf("%s", content)
  fmt.Printf("***You may also check the result by running 'less %s'***\n", tg_tmp_file)
}

func main() {
  //////////////////////////////////////////
  //          Parse Options               //
  //////////////////////////////////////////

  helpPtr := flag.Bool("help", false, "show help info.")
  helpPtr = flag.Bool("h", *helpPtr, "show help info.")

  debugPtr := flag.Bool("debug", false, "show debug info.")

  externalPtr := flag.Bool("g", false, "For GraphStudio to collect logs.")

  // start & end DateTime
  var start string
  var end string
  flag.StringVar(&start, "start", "", "Start DateTime")
  flag.StringVar(&start, "s", start, "Start DateTime")
  flag.StringVar(&end, "end", "", "End DateTime")
  flag.StringVar(&end, "e", end, "End DateTime")

  // after context number
  flag.IntVar(&after, "A", 0, "after context")
  flag.IntVar(&after, "after-context", after, "after context")

  // before context number
  flag.IntVar(&before, "B", 0, "before context")
  flag.IntVar(&before, "before-context", before, "before context")

  // regex patterns
  flag.Var(&patterns, "pattern", "regular expression.")
  flag.Var(&patterns, "p", "regular expression.")

  // request id
  flag.StringVar(&request_id, "request_id", "", "request id.")
  flag.StringVar(&request_id, "r", request_id, "request id.")

  // nodes id
  flag.StringVar(&nodes, "nodes", "", "nodes id.")
  flag.StringVar(&nodes, "n", nodes, "nodes id.")

  // output dir
  flag.StringVar(&output_dir, "output_dir", "output", "output dir.")
  flag.StringVar(&output_dir, "o", output_dir, "output dir.")

  flag.StringVar(&components, "components", "", "components.")
  flag.StringVar(&components, "c", components, "components.")

  // before
  flag.IntVar(&before_seconds, "before",  0, "Start time before query starts.")
  flag.IntVar(&before_seconds, "b",  before_seconds, "Start time before query starts.")

  // duration
  flag.IntVar(&duration, "duration",  0, "Requests' duration.")
  flag.IntVar(&duration, "d",  duration, "Requests' duration.")

  var tminus int
  flag.IntVar(&tminus, "tminus",  0, "seconds in the past.")
  flag.IntVar(&tminus, "t",  tminus, "seconds in the past.")
  flag.BoolVar(&display, "display", false, "Print to screen.")
  flag.BoolVar(&display, "D", display, "Print to screen.")

  var ignoreCase bool
  flag.BoolVar(&ignoreCase, "ignore-case", false, "Ignore case.")
  flag.BoolVar(&ignoreCase, "i", ignoreCase, "Print to screen.")

  // Hidden options used internally on slave nodes
  flag.BoolVar(&slave, "slave", false, "Running on a slave.")
  flag.StringVar(&master_ip, "ip",  "", "Ip address of the machine on which the binary was invoked")
  flag.StringVar(&master_dest_dir, "O",  "", "Dest dir on master")

  flag.Parse()
  external = *externalPtr
  command := flag.Args()

  // Check components against supported components
  if components != "" {
    supported_components_map := make(map[string]int)
    for _, str := range strings.Split(supported_components, ",") {
      supported_components_map[str] = 1
    }
    var unknown_components []string
    need_components := strings.Split(components, ",")
    need_components_map = make(map[string]int)
    for _, str := range need_components {
      _, ok := supported_components_map[str]
      if !ok {
        unknown_components = append(unknown_components, str)
      }
      need_components_map[str] = 1
    }
    if len(unknown_components) > 0 {
      fmt.Println("unknown_components: ", unknown_components)
      log.Fatalf("Supported components are: %s", supported_components)
    }
  }

  // Parse start and end epoch
  if tminus > 0 {
    endEpoch = time.Now().Unix()
    startEpoch = endEpoch - int64(tminus)
  } else {
    if start != "" {
      t1, err := time.Parse(timeFormat, start)
      if err != nil {
        fmt.Println(err)
        log.Fatal("Support format: 2006-01-02,15:04:05")
      }
      startEpoch = t1.Unix()
    }
    if end != "" {
      t2, err := time.Parse(timeFormat, end)
      if err != nil {
        fmt.Println(err)
        log.Fatal("Support format: 2006-01-02,15:04:05")
      }
      endEpoch = t2.Unix()
    }
  }

  // Add "(?i)" for each pattern if case insensitive
  if ignoreCase {
    var tmpArray arrayFlags
    for _, pattern:= range patterns {
      tmpArray = append(tmpArray, "(?i)" + pattern)
    }
    patterns = tmpArray
  }

  if *debugPtr {
    fmt.Println("after: ", after)
    fmt.Println("before: ", before)
    fmt.Println("start: ", start)
    fmt.Println("end: ", end)
    fmt.Println("patterns: ", patterns)
    fmt.Println("output_dir: ", output_dir)
    fmt.Println("tminus: ", tminus)
    fmt.Println("command: ", command)
    fmt.Println("GraphStudio: ", external)
  }

  if *helpPtr {
    usage()
    return
  }

  // Parameters sanity check
  if len(command) == 0 {
    log.Fatal("Please choose one command: grep, show or collect")
  }

  if command[0] != "collect" && command[0] != "grep"  && command[0] != "show" {
    log.Fatalf("Command \"%s\" is not supported, please choose \"grep\", \"show\" or \"collect\"",
      command[0])
  }

  if request_id != "" && command[0] != "collect" {
    log.Fatal("\"request_id\" could ONLY be used in together with command \"collect\".")
  }

  if display && components == "" {
    log.Fatal("Please specify at least one component when printing to screen.")
  }

  // For values of option "pattern", we need to add quotation marks
  // And it is not allowed to have space in start & end DateTime for the sake of simplicity
  argsWithoutProg = ""
  previous_is_pattern := false
  for _, str := range os.Args[1:] {
    if previous_is_pattern {
      argsWithoutProg += " \"" + str + "\""
    } else {
      argsWithoutProg += " " + str
    }
    if strings.Contains(str, "pattern") {
      previous_is_pattern = true
    } else {
      previous_is_pattern = false
    }
  }

  //////////////////////////////////////////
  //        Get TigerGraph config         //
  //////////////////////////////////////////

  usr, err := user.Current()
  if err != nil {
      log.Fatal( err )
  }
  homeDir := usr.HomeDir
  gsqlCfgFile := homeDir + "/.gsql/gsql.cfg"
  if !utils.CheckFileExisted(gsqlCfgFile) {
    log.Fatalf("GSQL config file not found. Please run \"gsql_admin --configure\" first.")
  }
  yamlFile, err := ioutil.ReadFile(gsqlCfgFile)
  if err != nil {
    log.Fatalf("Failed to open [%s] with error: %s", gsqlCfgFile, err)
  }
  m := make(map[string]string)
  err = yaml.Unmarshal(yamlFile, &m)
  if err != nil {
    log.Fatalf("yaml.Unmarshal failed: %v", err)
  }
  sshPort, _ := m["ssh.port"]
  str, _ := m["restpp.timeout_seconds"]
  nginx_port, _ := m["nginx.services.port"]
  timeout, err := strconv.Atoi(str)

  // Get nodes' name and IP
  name2ip = make(map[string]string)
  clusterNodes, _ := m["cluster.nodes"]
  for _, value := range strings.Split(clusterNodes, ",") {
    value = strings.Replace(value, "\n", "", -1)
    pair := strings.Split(value, ":")
    if len(pair) == 2 {
      name2ip[pair[0]] = pair[1]
    }
  }

  // Get servers of each component
  gpeServers, _ = m["gpe.servers"]
  gseServers, _ = m["gse.servers"]
  kafkaServers, _ = m["kafka.servers"]
  restServers, _ = m["restpp.servers"]
  zkServers, _ = m["zk.servers"]

  // Get sshkey and TigerGraph root dir
  username, _ := m["cluster.user"]
  sshkey, _ := m["cluster.sshkey"]
  tgRootDir, _ = m["tigergraph.root.dir"]
  tgLogDir, _ = m["tigergraph.log.root"]

  // Parse folders for each component on each node
  nodes_folders_map = make(map[string]NodeFolders)
  sshCfgFile := homeDir + "/.gsql/ssh_config"
  if !utils.CheckFileExisted(sshCfgFile) {
    log.Fatalf("Failed to find file: %s", sshCfgFile)
  }
  for name, _ := range name2ip {
    stdout := utils.LocalRun("grep " + name + " " + sshCfgFile)
    stdout = strings.Replace(stdout, "\n", "", -1)
    gpe_folder := ""
    restpp_folder := ""
    restpp_loader_folder := ""
    kafka_loader_folder := ""
    gse_folder := ""
    for _, str := range strings.Split(stdout, " ") {
      if strings.HasPrefix(str, "GPE") {
        gpe_folder = str
      } else if strings.HasPrefix(str, "RESTPP_") {
        restpp_folder = str
      } else if strings.HasPrefix(str, "RESTPP-LOADER_") {
        restpp_loader_folder = str
      } else if strings.HasPrefix(str, "KAFKA-LOADER_") {
        kafka_loader_folder = str
      } else if strings.HasPrefix(str, "GSE") {
        gse_folder = str
      }
    }
    nodes_folders_map[name] = NodeFolders{gpe_folder, restpp_folder,
      restpp_loader_folder, kafka_loader_folder, gse_folder}
  }

  // print config if debugging is enabled
  if *debugPtr {
    fmt.Println("sshPort: ", sshPort)
    fmt.Println("name2ip: ", name2ip)
    fmt.Println("username: ", username)
    fmt.Println("sshkey: ", sshkey)
    fmt.Println("timeout: ", timeout)
    fmt.Println("gpeServers: ", gpeServers)
    fmt.Println("gseServers: ", gseServers)
    fmt.Println("kafkaServers: ", kafkaServers)
    fmt.Println("restServers: ", restServers)
    fmt.Println("zkServers: ", zkServers)
    fmt.Println("tgRootDir: ", tgRootDir)
    fmt.Println("tgLogDir: ", tgLogDir)
    fmt.Println("nginx_port: ", nginx_port)
    fmt.Println("nodes_folders_map: ", nodes_folders_map)
  }

  slave_tmp_folder = tgLogDir + "/" + slave_tmp_folder
  tg_tmp_file = tgLogDir + "/" + tg_tmp_file

  if command[0] == "grep" {
    grepLogFiles()
    return
  }

  // find node_id of current machine, e.g., m1/m2/m3...
  myIpMap := utils.GetMyIpMap()
  for name, ip := range name2ip {
    _, ok := myIpMap[ip]
    if ok {
      node_id = name
      if !slave {
        master_ip = ip
      }
      break
    }
  }

  if *debugPtr {
    fmt.Println("myIpMap: ", myIpMap)
    fmt.Println("node_id: ", node_id)
  }

  sync_chan = make(chan int)
  if slave {
    // Create tmp folder on slave nodes
    utils.CreateDirectory(slave_tmp_folder)
  } else {
    // Create output dir and subdirectories on master
    utils.CreateDirectory(output_dir)
    for name, _ := range name2ip {
      utils.CreateDirectory(output_dir + "/" + name)
    }
    master_dest_dir, err = filepath.Abs(output_dir)
    utils.CheckError("Failed to get absolute path: %s", err)

    exe, err := filepath.Abs(os.Args[0])
    parentDir := filepath.Dir(exe)
    _, str := path.Split(parentDir)
    if str == ".gium" {
      under_gium = true
    }
    if *debugPtr {
      fmt.Println("parent dir: ", parentDir)
      fmt.Println("under_gium: ", under_gium)
    }

    utils.CheckError("Failed to get absolute path: %s", err)
    name2client = make(map[string]*ssh.Client)

    for name, ip := range name2ip {
      if node_id == name {
        continue
      }

      if !under_gium {
        // Copy the binary to other nodes, so that it could be run remotely.
        err = utils.Scp(username, "", ip, sshPort, sshkey, exe, tgLogDir)
        if err != nil {
          fmt.Printf("Scp to [%s] failed with %s\n", name, err)
          failed_nodes_array = append(failed_nodes_array, name)
          continue
        }
      }

      // Setup ssh connection to slave nodes.
      client, err := utils.Connect(username, "", ip, sshPort, sshkey)
      utils.CheckError("utils.Connect failed with %s\n", err)
      name2client[name] = client
    }

    // Asynchronously launch the command on slaves, with "--slave" option
    if under_gium {
      runCmdOnSlave(parentDir)
    } else {
      runCmdOnSlave(tgLogDir)
    }
  }

  // show all requests during the specified time window
  if command[0] == "show" {
    request_id_filename := "tigergraph_request_id.csv"
    outFile := ""
    if slave {
      outFile = slave_tmp_folder + "/" + request_id_filename
    } else {
      outFile = output_dir + "/" + node_id + "/" + request_id_filename
    }
    logs.GetRequestIDs(getLogDir(tgLogDir, "rest", node_id), outFile, startEpoch, endEpoch, timeout)

    var id2desc map[string]string
    var id2status map[string]string
    id2desc = make(map[string]string)
    id2status = make(map[string]string)

    if slave {
      // Copy result files back to master on slave nodes.
      err = utils.Scp(username, "", master_ip, sshPort, sshkey,
        outFile, master_dest_dir + "/" + node_id)
      utils.CheckError("utils.Scp failed with %s\n", err)
    } else {
      syncSlaveNodes()

      // Since all the files have been write back to master, aggregate them into a map
      for name, _ := range name2ip {
        filename := output_dir + "/" + name + "/" + request_id_filename
        if !utils.CheckFileExisted(filename) {
          continue
        }
        f , err := os.OpenFile(filename, os.O_RDONLY, 0)
        utils.CheckError("Failed to OpenFile: %s", err)
        br := bufio.NewReader(f)
        for {
          line, err := br.ReadString('\n')
          if err == io.EOF {
            break
          }
          line = strings.Replace(line, "\n", "", -1)
          tokens := strings.Split(line, "\t")
          id2desc[tokens[0]] = tokens[1]
          id2status[tokens[0]] = tokens[2]
        }
      }
    }

    if len(id2desc) == 0 {
      fmt.Printf("No new request.\n")
    } else {
      // Sort based on timestamp
      var ts2key map[string]string
      ts2key = make(map[string]string)
      var ts2desc map[string]string
      ts2desc = make(map[string]string)
      var ts2status map[string]string
      ts2status = make(map[string]string)
      var keys []string
      for key, desc := range id2desc {
        // Job id example: RESTPP_2_1.1558114825768
        // Use the latter timestamp as key, so we can sort based on it
        tokens := strings.Split(key, ".")
        k := tokens[1]
        keys = append(keys, k)
        ts2key[k] = key
        ts2desc[k] = desc
        ts2status[k] = id2status[key]
      }
      sort.Strings(keys)
      fmt.Printf("request_id                 Status    job_desc\n")
      for _, k := range keys {
        fmt.Printf("%s   %07s  %s\n", ts2key[k], ts2status[k], ts2desc[k])
      }
    }

    if len(failed_nodes_array) > 0 {
      fmt.Println("Failed nodes: ", failed_nodes_array)
    }
    return
  }

  // Collect logs regarding a certain request
  if request_id != "" {
    // e.g., RESTPP_1_1.1557527283332
    tokens := strings.Split(request_id, ".")
    if len(tokens) < 2 || len(tokens[1]) < 10 {
      fmt.Printf("Illegal request_id: \"%s\"\n", request_id)
      log.Fatal("A valid request_id shoule be like this: RESTPP_1_1.1557527283332")
    }
    startEpoch, err = strconv.ParseInt(tokens[1][0:10], 10, 64)
    startEpoch = startEpoch - int64(before_seconds)
    if err != nil || startEpoch < 0 || startEpoch > time.Now().Unix() {
      fmt.Printf("Illegal request_id: \"%s\"\n", request_id)
      log.Fatal("A valid request_id shoule be like this: RESTPP_1_1.1557527283332")
    }
    if duration > 0 {
      endEpoch = startEpoch + int64(duration)
    } else {
      endEpoch = time.Now().Unix()
    }

    // append the "request_id" to the "patterns" array,
    // so that lines that match any of them will be printed.
    patterns = append(patterns, request_id)
  }

  if *debugPtr {
    fmt.Println("startEpoch: ", startEpoch)
    fmt.Println("endEpoch: ", endEpoch)
  }

  outFolder := ""
  if slave {
    outFolder = slave_tmp_folder + "/"
  } else {
    outFolder = output_dir + "/" + node_id + "/"
    // Some debug info only need to be collected from master
    if components == "" {
      collectDebugInfo()
    }
  }

  // Get all dmesg info when OOM occurs
  _, err = utils.LocalRunIgnoreError("dmesg | grep -i oom")
  if err == nil {
    utils.LocalRun("dmesg > " + outFolder + "/dmesg")
  }
  if !external && utils.CheckFileExisted(tgRootDir + "/data/ts3.db") {
    utils.LocalRun("cp -r " + tgRootDir + "/data/ts3.db " + outFolder)
  }

  // Only collect yaml files and loader logs when components is not specified.
  if !external && components == "" {
    if utils.CheckFileExisted(tgRootDir + "/gstore/0/part/config.yaml") {
      utils.LocalRun("cp -r " + tgRootDir + "/gstore/0/part/config.yaml " + outFolder)
    }
    if utils.CheckFileExisted(tgRootDir + "/gstore/0/1/ids/graph_config.yaml") {
      utils.LocalRun("cp -r " + tgRootDir + "/gstore/0/1/ids/graph_config.yaml " + outFolder)
    }
    if utils.CheckDirExisted(tgLogDir + "/restpp/restpp_loader_logs") {
      utils.LocalRun("cp -r " + tgLogDir + "/restpp/restpp_loader_logs " + outFolder)
    }
  }

  // If there is no other regex except request_id, only gpe/gse/restpp logs will be collected.
  if request_id != "" && len(patterns) == 1 {
    logs.FilterLogs(getLogDir(tgLogDir, "rest", node_id),
      outFolder + "/restpp.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterLogs(getLogDir(tgLogDir, "gpe", node_id),
      outFolder + "/gpe.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterLogs(getLogDir(tgLogDir, "gse", node_id),
      outFolder + "/gse.log", patterns, startEpoch, endEpoch, before, after)
  } else {
    // Otherwise, collect all the logs.
    filterLogs(outFolder)
  }

  if !slave {
    // Waiting for slave nodes to copy logs backup to master
    syncSlaveNodes()
    // Close all ssh connections to slave nodes
    for name, client := range name2client {
      if name == node_id {
        continue
      }
      client.Close()
    }
    if display {
      printToScreen()
    }
  } else {
    // Copy log files back to master on slave nodes.
    err = utils.Scp(username, "", master_ip, sshPort,
      sshkey, slave_tmp_folder + "/*" , master_dest_dir + "/" + node_id)
    utils.CheckError("utils.Scp failed with %s\n", err)
    utils.RemoveContents(slave_tmp_folder)
  }

  if len(failed_nodes_array) > 0 {
    fmt.Println("Failed nodes: ", failed_nodes_array)
  }

  return
}

func readFile(path string) ([]byte, error) {
  file, err := os.Open(path)
  if err != nil {
    return nil, err
  }

  defer file.Close()
  return read(file)
}

func read(fd_r io.Reader) ([]byte, error) {
  br := bufio.NewReader(fd_r)
  var buf bytes.Buffer

  for {
    ba, isPrefix, err := br.ReadLine()

    if err != nil {
      if err == io.EOF {
        break
      }
      return nil, err
    }

    buf.Write(ba)
    if !isPrefix {
      buf.WriteByte('\n')
    }

  }
  return buf.Bytes(), nil
}

func printToScreen() {
  // Clear & create the tmp file
  utils.LocalRun("echo -n \"\" > " + tg_tmp_file)
  for name, _ := range name2ip {
    dir := output_dir + "/" + name
    files, err := ioutil.ReadDir(dir)
    if err != nil {
      log.Fatal(err)
    }
    for _, file := range files {
      if file.IsDir() {
        continue
      }
      logInfo := dir + "/" + file.Name()
      fi, err := os.Stat(logInfo);
      if err != nil {
        log.Fatal(err)
      }
      if fi.Size() == 0 {
        continue
      }
      utils.LocalRun("echo \"================" + logInfo + " \" >> " + tg_tmp_file)
      utils.LocalRun("cat " + logInfo + " >> " + tg_tmp_file)
      utils.LocalRun("echo \"\" >> " + tg_tmp_file)
    }
  }
  content, err := readFile(tg_tmp_file)
  if err != nil {
    log.Fatal(err)
  }
  fmt.Printf("%s", content)
  fmt.Printf("***You may also check the result by running 'less %s'***\n", tg_tmp_file)
}

func needToFilter(component string) bool {
  if components == "" {
    return true
  }

  _, ok := need_components_map[component]
  return ok
}

func filterLogs(outFolder string) {
  if needToFilter("rest") && strings.Contains(restServers, node_id) {
    logs.FilterLogs(getLogDir(tgLogDir, "rest", node_id),
      outFolder + "/restpp.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterOutLogs(tgLogDir + "/restpp",
      outFolder + "/restpp.out", "restpp", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("gpe") && strings.Contains(gpeServers, node_id) {
    logs.FilterLogs(getLogDir(tgLogDir, "gpe", node_id),
      outFolder + "/gpe.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterOutLogs(tgLogDir + "/gpe",
      outFolder + "/gpe.out", "gpe", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("gse") && strings.Contains(gseServers, node_id) {
    logs.FilterLogs(getLogDir(tgLogDir, "gse", node_id),
      outFolder + "/gse.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterOutLogs(tgLogDir + "/gse",
      outFolder + "/gse.out", "gse", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("tsar") {
    tsar.FilterLogs("/var/log", outFolder + "/tsar.log", startEpoch, endEpoch)
  }

  if needToFilter("nginx") {
    logs.NginxFilterLogs(tgLogDir + "/nginx",
      outFolder + "/nginx.log", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("admin") {
    logs.FilterLogs(tgLogDir + "/admin_server",
      outFolder + "/admin_server.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterOutLogs(tgLogDir + "/admin_server",
      outFolder + "/admin_server.out", "gadmin_server.out",
      patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("dict") {
    logs.FilterLogs(tgLogDir + "/dict",
      outFolder + "/gdict_server.log", patterns, startEpoch, endEpoch, before, after)
    logs.FilterOutLogs(tgLogDir + "/dict",
      outFolder + "/gdict_client.log", "gdict_client__INFO",
      patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("zk") && strings.Contains(zkServers, node_id) {
    logs.FilterOutLogs(tgRootDir + "/zk",
      outFolder + "/zk.log", "zookeeper.out", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("kafka") && strings.Contains(kafkaServers, node_id){
    logs.KafkaFilterLogs(tgRootDir + "/kafka",
      outFolder + "/kafka.log", "kafka.out", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("kafka-connect") && strings.Contains(kafkaServers, node_id){
    logs.KafkaFilterLogs(tgRootDir + "/kafka",
      outFolder + "/kafka-connect.log", "kafka-connect.out", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("kafka-stream") && strings.Contains(kafkaServers, node_id){
    logs.KafkaFilterLogs(tgRootDir + "/kafka",
      outFolder + "/kafka-stream.log", "kafka-stream.out", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("gui") {
    logs.GuiFilterLogs(tgLogDir + "/gui",
      outFolder + "/gui.log", "gui_INFO.log", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("restpp_loader") {
    logs.FilterLogs(getLogDir(tgLogDir, "restpp_loader", node_id),
      outFolder + "/restpp_loader.log", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("fab") {
    logs.FilterOutLogs(tgRootDir + "/.gsql/fab_dir/cmd_logs",
      outFolder + "/fab_cmd.log", "", patterns, startEpoch, endEpoch, before, after)
  }

  if needToFilter("gsql") {
    gsql_log := tgRootDir + "/dev/gdk/gsql/logs/GSQL_LOG"
    if utils.CheckFileExisted(gsql_log) {
      utils.LocalRun("cp -r " + gsql_log + " " + outFolder)
    }
    gsql_log = tgLogDir + "/gsql_server_log/gsql.out"
    if utils.CheckFileExisted(gsql_log) {
      utils.LocalRun("cp -r " + gsql_log + " " + outFolder)
    }
    gsql_log = tgLogDir + "/gsql_server_log/gsql_ADMIN.log"
    if utils.CheckFileExisted(gsql_log) {
      utils.LocalRun("cp -r " + gsql_log + " " + outFolder)
    }
    logs.GsqlFilterLogs(tgLogDir + "/gsql_server_log/",
      outFolder + "/gsql.log", patterns, startEpoch, endEpoch, before, after)
  }
}

