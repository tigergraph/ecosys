package utils

import (
  "os/exec"
  "log"
  "os"
  "bytes"
  "io/ioutil"
  "path/filepath"
  // "runtime/debug" 
  "fmt"
  "time"
  "net"
  "regexp"
  "strconv"
  "strings"
  "golang.org/x/crypto/ssh"
)

type DateTimeType int
const (
    Full DateTimeType = iota // e.g., 2019-05-07 22:47:09
    Compact // e.g., 20190507-221634.23248
    NoYear // e.g., I/W/E0507 22:16:49.801210
    Short // 22:47:09.824235
    Epoch // e.g., %3|1557778930.317 
    Unknown
)

func isDigit(str string) bool {
  _, err := strconv.Atoi(str)
  return err == nil
}

// Determin DateTimeType of a certain timestamp
// Instead of using regex match (which is too time-consuming),
// just check some chars in the string
func CheckDateTimeType(str string) DateTimeType {
  if len(str) < 15 {
    return Unknown
  }
  if str[4] == '-' && str[10] == ' ' && isDigit(str[0:4]) && isDigit(str[5:7]) {
    return Full
  }
  if str[8] == '-' && str[15] == '.' && isDigit(str[0:8]) && isDigit(str[9:15]) {
    return Compact
  }
  if str[5] == ' ' && (str[0] == 'I' || str[0] == 'W' || str[0] == 'E') {
    if isDigit(str[1:5]) && isDigit(str[6:8]) && str[8] == ':' {
      return NoYear
    } else {
      return Unknown
    }
  }
  if str[2] == ':' && str[5] == ':' && isDigit(str[0:2]) && isDigit(str[3:5]) {
    return Short
  }
  if str[2] == '|' && str[0] == '%' && str[13] == '.' && isDigit(str[3:13]) {
    return Epoch
  }
  return Unknown
}

// Convert the following DateTime format to epoch:
// %3|1557778930.317
func Datetime2EpochPlain(ts string) int64 {
  str := ts[3:13]
  epoch, err := strconv.ParseInt(str, 10, 64)
  if err != nil {
    fmt.Printf("Failed to convert \"%s\" to int64", str)
    return -1
  }
  return epoch
}

// Convert the following DateTime format to epoch:
// 22:47:09.824235
func Datetime2EpochShort(lastEpoch int64, ts string) int64 {
  t := time.Unix(lastEpoch, 0)
  year, month, day := t.Date()
  formattedTime := fmt.Sprintf("%4d-%02d-%02dT%sZ", year, month, day, ts[0:8])
  t, err := time.Parse(time.RFC3339, formattedTime)
  if err != nil {
    errInfo := fmt.Sprintf("[IGNORED]Failed to convert \"%s\" to epoch with error: %s\n", ts, err)
    fmt.Printf(errInfo)
    // debug.PrintStack()
    return -1
  }
  return t.Unix()
}

// Convert the following DateTime format to epoch:
// 2019-05-07 22:47:09
func Datetime2EpochFull(ts string) int64 {
  const timeFormat = "2006-01-02 15:04:05"
  formattedTime := ts[0:19]
  t, err := time.Parse(timeFormat, formattedTime)
  if err != nil {
    errInfo := fmt.Sprintf("Failed to convert \"%s\" to epoch with error: %s", ts, err)
    fmt.Println(errInfo)
    return -1
  }
  return t.Unix()
}

// Convert the following DateTime format to epoch:
// 20190507-221634.23248
func Datetime2EpochCompact(ts string) int64 {
  const timeFormat = "20060102-150405"
  formattedTime := ts[0:15]
  t, err := time.Parse(timeFormat, formattedTime)
  if err != nil {
    errInfo := fmt.Sprintf("Failed to convert \"%s\" to epoch with error: %s", ts, err)
    fmt.Println(errInfo)
    return -1
  }
  return t.Unix()
}

// Convert the following DateTime format to epoch:
// I0507 22:47:09.501549
func Datetime2EpochNoYear(year, str string) int64 {
  const timeFormat = "20060102 15:04:05"
  formattedTime := year + str[1:14]
  t, err := time.Parse(timeFormat, formattedTime)
  if err != nil {
    errInfo := fmt.Sprintf("Failed to convert \"%s\" to epoch with error: %s", str, err)
    fmt.Println(errInfo)
    return -1
  }
  return t.Unix()
}

func LocalRun (cmd string) string {
  out, err := exec.Command("bash", "-c", cmd).Output()
	if err != nil {
    fmt.Printf("Failed to execute command: %s\nError: %s", cmd, err)
	}
	return string(out)
}

func LocalRunIgnoreError(cmd string) (string, error) {
  out, err := exec.Command("bash", "-c", cmd).Output()
	return string(out), err
}

func CheckError(str string, err error) {
  if err != nil {
    log.Fatalf(str, err)
  }
}

func RemoveContents(dir string) error {
  d, err := os.Open(dir)
  if err != nil {
    return err
  }
  defer d.Close()
  names, err := d.Readdirnames(-1)
  if err != nil {
    return err
  }
  for _, name := range names {
    err = os.RemoveAll(filepath.Join(dir, name))
    if err != nil {
      return err
    }
  }
  return nil
}

func CheckDirExisted(dir string) bool {
  src, err := os.Stat(dir)

  if os.IsNotExist(err) {
    return false
  }

  if src.Mode().IsDir() {
    return true
  }

  return false
}

func CheckFileExisted(filename string) bool {
  src, err := os.Stat(filename)

  if os.IsNotExist(err) {
    return false
  }

  if src.Mode().IsRegular() {
    return true
  }

  return false
}

func CreateDirectory(dir string) bool {
  src, err := os.Stat(dir)

  if os.IsNotExist(err) {
    err := os.MkdirAll(dir, 0755)
    if err != nil {
      panic(err)
    }
    return true
  }

  // Remove directory if it exists
  if src.Mode().IsDir() {
    RemoveContents(dir)
    return true
  }

  if src.Mode().IsRegular() {
    log.Fatalf("%s already exist as a file!", dir)
  }

   return false
}

func Connect(user, password, host, port, key string) (*ssh.Client, error) {
  var (
    auth         []ssh.AuthMethod
    addr         string
    clientConfig *ssh.ClientConfig
    client       *ssh.Client
    config       ssh.Config
    err          error
  )
  // get auth method
  auth = make([]ssh.AuthMethod, 0)
  if key == "" {
    auth = append(auth, ssh.Password(password))
  } else {
    pemBytes, err := ioutil.ReadFile(key)
    if err != nil {
        return nil, err
    }

    var signer ssh.Signer
    if password == "" {
        signer, err = ssh.ParsePrivateKey(pemBytes)
    } else {
        signer, err = ssh.ParsePrivateKeyWithPassphrase(pemBytes, []byte(password))
    }
    if err != nil {
        return nil, err
    }
    auth = append(auth, ssh.PublicKeys(signer))
  }

  config = ssh.Config{
      Ciphers: []string{"aes128-ctr", "aes192-ctr", "aes256-ctr", "aes128-gcm@openssh.com",
      "arcfour256", "arcfour128", "aes128-cbc", "3des-cbc", "aes192-cbc", "aes256-cbc"},
  }

  clientConfig = &ssh.ClientConfig{
    User:    user,
    Auth:    auth,
    Timeout: 30 * time.Second,
    Config:  config,
    HostKeyCallback: func(hostname string, remote net.Addr, key ssh.PublicKey) error {
        return nil
    },
  }

  // connet to ssh
  addr = fmt.Sprintf("%s:%s", host, port)

  if client, err = ssh.Dial("tcp", addr, clientConfig); err != nil {
    return nil, err
  }

  return client, nil
}

// run several commands(separated with ";") one by one on remote server 
func RunCmds(client *ssh.Client, cmds string) (string, string) {
  cmdlist := strings.Split(cmds, ";")
  var stdoutBuff bytes.Buffer
  var stderrBuff bytes.Buffer
  for _, command := range cmdlist {
    session, err := client.NewSession()
    if err != nil {
      log.Fatal(err)
    }
    defer session.Close()
    session.Stdout = &stdoutBuff
    session.Stderr = &stderrBuff
    session.Run(command)
  }
  return stdoutBuff.String(), stderrBuff.String()
}

// run several commands asynchronously
func RunCmds_async(client *ssh.Client, cmds string, c chan int) {
  RunCmds(client, cmds)
  c <- 0
}

// Run commands that might fail
func RunCmdsIgnoreError(client *ssh.Client, cmds string) (string, string) {
  cmdlist := strings.Split(cmds, ";")
  var stdoutBuff bytes.Buffer
  var stderrBuff bytes.Buffer
  for _, command := range cmdlist {
    session, _ := client.NewSession()
    defer session.Close()
    session.Stdout = &stdoutBuff
    session.Stderr = &stderrBuff
    session.Run(command)
  }
  return stdoutBuff.String(), stderrBuff.String()
}

func Scp(user, password, host, port, key, src, dest string) error {
  cmd := exec.Command("bash")
  cmdWriter, err := cmd.StdinPipe()
  if err != nil {
    return err
  }
  cmd.Start()

  cmdString := ""
  if password != "" {
    cmdString = fmt.Sprintf("sshpass -p %s scp -o ConnectTimeout=30 -o \"StrictHostKeyChecking no\" -P %s -r %s %s@%s:%s", password, port, src, user, host, dest)
  } else {
    cmdString = fmt.Sprintf("scp -o ConnectTimeout=30 -o \"StrictHostKeyChecking no\" -i %s -P %s -r %s %s@%s:%s", key, port, src, user, host, dest)
  }
  cmdWriter.Write([]byte(cmdString + "\n"))
  cmdWriter.Write([]byte("exit"    + "\n"))

  err = cmd.Wait()
  return err
}

// get last modified time
func GetFileModTime(filename string) time.Time {
  file, err := os.Stat(filename)

  if err != nil {
    log.Fatal(err)
  }

  return file.ModTime()
}

// Return true when match anyone of the regexArray
func MatchAnyRegex(line string, regexArray []*regexp.Regexp) bool {
  if len(regexArray) == 0 {
    return true
  }

  for _, re:= range regexArray {
    if re.FindStringIndex(line) != nil {
      return true
    }
  }

  return false
}

func Interface2Str(inter interface{}) string {
  switch inter.(type) {
  case string:
    return inter.(string)
  case int:
    return string(inter.(int))
  default:
    return ""
  }
}

// Get local ip addresses
// return a map for better search performance
func GetMyIpMap() map[string]int {
  var myIpMap map[string]int
  myIpMap = make(map[string]int)
  ifaces, err := net.Interfaces()
  if err != nil {
    log.Fatal(err)
  }
  for _, i := range ifaces {
    addrs, err := i.Addrs()
    if err != nil {
      log.Fatal(err)
    }
    for _, addr := range addrs {
      var ip net.IP
      switch v := addr.(type) {
        case *net.IPNet:
          ip = v.IP
        case *net.IPAddr:
          ip = v.IP
      }
      myIpMap[ip.String()] = 1
    }
  }
  return myIpMap
}

