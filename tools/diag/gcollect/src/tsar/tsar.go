package tsar

import (
  "utils"
  "os"
  "io"
  "fmt"
  "bufio"
  "io/ioutil"
  "log"
  "sort"
  "strings"
  "strconv"
)

func str2Epoch(str string) int64 {
  epoch, err := strconv.ParseInt(str, 10, 64)
  if err != nil {
    fmt.Printf("Failed to convert \"%s\" to int64", str)
    return -1
  }
  return epoch
}

// Get all the logs between startEpoch and endEpoch from logDir
// Typically logDir is "/var/log/"
// tsar log has fixed format, so don't need pattern match
func FilterLogs(logDir, outFile string, startEpoch, endEpoch int64) {

  if !utils.CheckDirExisted(logDir) {
    return
  }

  // Open output file for write
  fw, err := os.OpenFile(outFile, os.O_WRONLY|os.O_TRUNC|os.O_CREATE, os.ModePerm)
  if err != nil {
    panic(err)
  }
  defer fw.Close()

  // Sort files by the last modified time
  files, err := ioutil.ReadDir(logDir)
  if err != nil {
    log.Fatal(err)
  }
  sort.Slice(files, func(i, j int) bool{
    return files[i].ModTime().Unix() < files[j].ModTime().Unix()
  })

  // Process files one by one
  for _, file := range files {
    if file.IsDir() {
      continue
    }
    logInfo := file.Name()

    // Ignore files not starting with "tsar.data"
    if !strings.HasPrefix(logInfo, "tsar.data") {
      continue
    }
    logInfo = logDir + "/" + logInfo

    // Check the time of the first log in the file.
    str := utils.LocalRun("head -1 " + logInfo)
    tokens := strings.Split(str, "|")
    epoch := str2Epoch(tokens[0])
    if epoch > 0 && endEpoch < epoch {
      continue
    }

    // Check the time of the last log in the file.
    str = utils.LocalRun("tail -n 1 " + logInfo)
    tokens = strings.Split(str, "|")
    epoch = str2Epoch(tokens[0])
    if epoch > 0 && startEpoch > epoch {
      continue
    }

    // Open log file for read
    f , err := os.OpenFile(logInfo, os.O_RDONLY, 0)
    if err != nil{
      panic(err)
    }
    br := bufio.NewReader(f)
    if _, err = fw.WriteString(">>>>>>>>>" + logInfo + "\n"); err != nil {
      panic(err)
    }

    // Handle each line of the log file
    for {
      line, err := br.ReadString('\n')
      if err == io.EOF {
        break
      }
      if startEpoch > 0 || endEpoch > 0 {
        tokens = strings.Split(line, "|")
        epoch = str2Epoch(tokens[0])
        // Some logs may be out of order due to disk flushing
        // Scan for extra 2 minutes to wait for logs syncing to disk
        if (endEpoch > 0 && epoch > endEpoch + 120) {
          break
        }
        if (startEpoch > 0 && epoch < startEpoch) || (endEpoch > 0 && epoch > endEpoch) {
          continue
        }
      }
      if _, err = fw.WriteString(line); err != nil {
        panic(err)
      }
    }
    f.Close()

  }
}

