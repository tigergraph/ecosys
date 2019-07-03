package logs

import (
  "utils"
  "os"
  "io"
  "bufio"
  "time"
  "sort"
  "regexp"
  "container/list"
  "io/ioutil"
  "log"
  "strings"
  "strconv"
)

// Get all requests' ID and url between startEpoch and endEpoch from logDir
// Typically logDir is "/home/tigergraph/tigergraph/logs/RESTPP_1_1"
func GetRequestIDs(logDir, outFile string, startEpoch, endEpoch int64, timeout int) {
  var lastEpoch int64 = 0
  var id2desc map[string]string
  var id2status map[string]string
  id2desc = make(map[string]string)
  id2status = make(map[string]string)

  if !utils.CheckDirExisted(logDir) {
    return
  }

  // Sort files by the last modified time
  files, err := ioutil.ReadDir(logDir)
  if err != nil {
    log.Fatal(err)
  }
  sort.Slice(files, func(i, j int) bool{
    return files[i].ModTime().Unix() < files[j].ModTime().Unix()
  })

  // Open output file for writing
  f, err := os.OpenFile(outFile, os.O_WRONLY|os.O_TRUNC|os.O_CREATE, os.ModePerm)
  if err != nil {
    panic(err)
  }

  for _, file := range files {
    if file.IsDir() {
      continue
    }
    logInfo := file.Name()
    // Ignore files not starting with "INFO"
    if !strings.HasPrefix(logInfo, "INFO") {
      continue
    }

    // File name example: INFO.20190507-221634.23248
    // The last part of file name is DateTime when the file was created,
    // and also the time of the first log in the file.
    tokens := strings.Split(logInfo, ".")
    epoch := utils.Datetime2EpochCompact(tokens[1])
    if epoch > 0 && endEpoch + int64(timeout) < epoch {
      continue
    }
    t := time.Unix(epoch, 0)
    year := strconv.Itoa(t.Year())
    begin_epoch := epoch
    begin_year := year

    // Check the time of the last log in the file.
    logInfo = logDir + "/" + logInfo
    epoch = file.ModTime().Unix()
    if epoch > 0 && startEpoch > epoch {
      continue
    }

    // Get request id and its url
    str, _ := utils.LocalRunIgnoreError("grep -ar '/query' " + logInfo + " | grep -i Engine_req")
    for _, line := range strings.Split(str, "\n") {
      if len(line) < 60 {
        continue
      }
      if startEpoch > 0 || endEpoch > 0 {
        // e.g., I0510 22:28:03.332123
        epoch = utils.Datetime2EpochNoYear(year, line[0:20])
        if epoch > 0 && epoch + 3600 * 100 < lastEpoch {
          // Move to next year
          tmp, _ := strconv.Atoi(year)
          year = strconv.Itoa(tmp + 1)
          epoch = utils.Datetime2EpochNoYear(year, line[0:20])
        }
        lastEpoch = epoch
        if (startEpoch > 0 && epoch < startEpoch) || (endEpoch > 0 && epoch > endEpoch) {
          continue
        }
      }
      tokens := strings.Split(line, "|")
      if len(tokens) > 4 {
        str := tokens[2]
        start := strings.Index(str, "RESTPP")
        if start < 0 || start + 24 > len(str) {
          continue
        }
        request_id := str[start:start+24]
        desc := strings.Replace(tokens[4], "url = ", "", 1)
        id2desc[request_id] = desc
      }
    }
    // Get requests that have finished.
    lastEpoch = begin_epoch
    year = begin_year
    str, err = utils.LocalRunIgnoreError("grep -ar 'ReturnResult' " + logInfo)
    for _, line := range strings.Split(str, "\n") {
      if len(line) < 60 {
        continue
      }
      if startEpoch > 0 || endEpoch > 0 {
        // e.g., I0510 22:28:03.332123
        epoch = utils.Datetime2EpochNoYear(year, line[0:20])
        if epoch > 0 && epoch + 3600 * 100 < lastEpoch {
          // Move to next year
          tmp, _ := strconv.Atoi(year)
          year = strconv.Itoa(tmp + 1)
          epoch = utils.Datetime2EpochNoYear(year, line[0:20])
        }
        lastEpoch = epoch
        if (startEpoch > 0 && epoch < startEpoch) || (endEpoch > 0 && epoch > endEpoch + int64(timeout)) {
          continue
        }
      }
      start := strings.Index(line, "RESTPP")
      if start < 0 || start + 24 > len(line) {
        continue
      }
      request_id := line[start:start+24]
      id2status[request_id] = "FINISHED"
    }
  }
  // Write to result file
  for key, desc := range id2desc {
    status, ok := id2status[key]
    if !ok {
      status = "Running"
    }
    str := key + "\t" + desc + "\t" + status + "\n"
    if _, err = f.WriteString(str); err != nil {
      panic(err)
    }
  }
}

// Append "str" to the list "l", while keeping its length less or equals to "cap"
func appendList(l *list.List, str string, cap int) *list.List {
  if cap == 0 {
    return l
  }
  if l.Len() < cap {
    l.PushBack(str)
    return l
  }
  // Remove the first one and append "str" to the end
  e := l.Front()
  l.Remove(e)
  l.PushBack(str)
  if l.Len() > cap {
    log.Fatal("list len out of limit: ", l)
  }
  return l
}

// Get all the logs between startEpoch and endEpoch from logDir
// Typically logDir is "/home/tigergraph/tigergraph/logs/RESTPP_1_1"
// Each line starts with a timestamp like "I/W/E0507 22:16:49.801210"
func FilterLogs(logDir, outFile string, patterns []string, startEpoch,
  endEpoch int64, before, after int) {
  var lastEpoch int64 = 0
  // Wether the last line's timestamp is between startEpoch and endEpoch
  var lastLineIncluded bool = false
  var hasSth bool = false
  var regexArray []*regexp.Regexp
  afterMatch := 0
  buff := list.New()

  if !utils.CheckDirExisted(logDir) {
    return
  }

  for _, pattern:= range patterns {
    re := regexp.MustCompile(pattern)
    regexArray = append(regexArray, re)
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


  for _, file := range files {
    if file.IsDir() {
      continue
    }
    logInfo := file.Name()
    // Ignore files not starting with "INFO"
    if !strings.HasPrefix(logInfo, "INFO") {
      continue
    }

    // File name example: INFO.20190507-221634.23248
    // The last part of file name is DateTime when the file was created,
    // and also the time of the first log in the file.
    tokens := strings.Split(logInfo, ".")
    epoch := utils.Datetime2EpochCompact(tokens[1])
    if epoch > 0 && endEpoch < epoch {
      continue
    }

    // Check the time of the last log in the file.
    t := time.Unix(epoch, 0)
    year := strconv.Itoa(t.Year())
    logInfo = logDir + "/" + logInfo
    epoch = file.ModTime().Unix()
    if epoch > 0 && startEpoch > epoch {
      continue
    }

    // Open log file for read
    f , err := os.OpenFile(logInfo, os.O_RDONLY, 0)
    if err != nil{
      panic(err)
    }
    br := bufio.NewReader(f)
    lastLineIncluded = false
    lastLineMatched := false
    needToWriteHeader := true

    // Read each line of the log
    for {
      line, err := br.ReadString('\n')
      if err == io.EOF {
        break
      }

      if startEpoch > 0 || endEpoch > 0 {
        // For lines that have no timestamp detected,
        // only include it when the last line is also printed
        if utils.CheckDateTimeType(line) == utils.Unknown {
          if !lastLineIncluded {
            continue
          }
        } else {
          // e.g., I0510 22:28:03.332123
          epoch = utils.Datetime2EpochNoYear(year, line[0:20])
          if epoch > 0 && epoch + 36000 < lastEpoch {
            // Move to next year
            tmp, _ := strconv.Atoi(year)
            year = strconv.Itoa(tmp + 1)
            epoch = utils.Datetime2EpochNoYear(year, line[0:20])
          }
          lastEpoch = epoch
          // Some logs may be out of order due to disk flushing
          // Scan for extra 2 minutes to wait for logs syncing to disk
          if (endEpoch > 0 && epoch > endEpoch + 120) {
            break
          }
          if (startEpoch > 0 && epoch < startEpoch) || (endEpoch > 0 && epoch > endEpoch) {
            lastLineIncluded = false
            continue
          }
        }
      }

      // Match patterns and write to file
      lastLineIncluded = true
      if utils.MatchAnyRegex(line, regexArray) {
        hasSth = true
        if needToWriteHeader {
          if _, err = fw.WriteString(">>>>>>>>>" + logInfo + "\n"); err != nil {
            panic(err)
          }
          needToWriteHeader = false
        }
        if !lastLineMatched {
          // Print num lines of leading context before each match
          lastLineMatched = true
          afterMatch = after
	        for e := buff.Front(); e != nil; e = e.Next() {
            str := utils.Interface2Str(e.Value)
            if _, err = fw.WriteString(str); err != nil {
              panic(err)
            }
          }
          buff.Init()
        }
        if _, err = fw.WriteString(line); err != nil {
          panic(err)
        }
      } else {
        lastLineMatched = false
        if afterMatch > 0 {
          // Print num lines of trailing context after each match
          if _, err = fw.WriteString(line); err != nil {
            panic(err)
          }
          afterMatch--
        } else {
          buff = appendList(buff, line, before)
        }
      }
    }
    f.Close()
  }

  // remove output file if it is empty.
  if (!hasSth) {
    os.Remove(outFile)
  }
}

// Get all the logs between startEpoch and endEpoch from logDir
// Typically logDir is "/home/tigergraph/tigergraph/logs/restpp"
// The format of timestamp may change from line to line
// Need to detect DateTime format for each line
func FilterOutLogs(logDir, outFile, prefix string, patterns []string,
  startEpoch, endEpoch int64, before, after int) {
  var lastEpoch int64 = 0
  // Wether the last line's timestamp is between startEpoch and endEpoch
  var lastLineIncluded bool = false
  var hasSth bool = false
  var regexArray []*regexp.Regexp
  afterMatch := 0
  buff := list.New()

  if !utils.CheckDirExisted(logDir) {
    return
  }

  for _, pattern:= range patterns {
    re := regexp.MustCompile(pattern)
    regexArray = append(regexArray, re)
  }

  // Open output file for write, with option "O_TRUNC" to overwrite the file
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

  for _, file := range files {
    if file.IsDir() {
      continue
    }
    logInfo := file.Name()

    // Ignore files not starting with specified prefix
    if prefix != "" && !strings.HasPrefix(logInfo, prefix) {
      continue
    }

    // Check the time of the last log in the file.
    logInfo = logDir + "/" + logInfo
    t := utils.GetFileModTime(logInfo)
    epoch := t.Unix()
    if epoch > 0 && startEpoch > epoch {
      continue
    }

    // Check the time of the first log in the file.
    str := utils.LocalRun("head -50 " + logInfo)
    for _, line := range strings.Split(str, "\n") {
      if utils.CheckDateTimeType(line) == utils.Full {
        epoch = utils.Datetime2EpochFull(line)
        break
      }
    }
    if epoch > 0 && endEpoch < epoch {
      continue
    }

    // Save time for timestamp which don't have year or day
    lastEpoch = epoch
    t = time.Unix(epoch, 0)
    year := strconv.Itoa(t.Year())

    // Open log file for read
    f , err := os.OpenFile(logInfo, os.O_RDONLY, 0)
    if err != nil{
      panic(err)
    }
    br := bufio.NewReader(f)
    lastLineIncluded = false
    lastLineMatched := false
    needToWriteHeader := true

    for {
      line, err := br.ReadString('\n')
      if err == io.EOF {
        break
      }

      if startEpoch > 0 || endEpoch > 0 {
        // For lines that have no timestamp detected,
        // only include it when the last line is also printed
        dateType := utils.CheckDateTimeType(line)
        if dateType == utils.Unknown {
          if !lastLineIncluded {
            continue
          }
        } else {
          switch dateType {
            case utils.Full:
              epoch = utils.Datetime2EpochFull(line[0:20])
            case utils.Compact:
              epoch = utils.Datetime2EpochCompact(line[0:20])
            case utils.NoYear:
              epoch = utils.Datetime2EpochNoYear(year, line[0:20])
            case utils.Short:
              epoch = utils.Datetime2EpochShort(lastEpoch, line[0:20])
            case utils.Epoch:
              epoch = utils.Datetime2EpochPlain(line[0:20])
            default:
              log.Fatal("Should not be here. \"Unknown\" has been handled before.")
          }

          // When failed to convert time, only include it when last line is also included
          if epoch < 0 {
            if !lastLineIncluded {
              continue
            }
          } else {
            if dateType == utils.NoYear && epoch + 3600 * 300 < lastEpoch {
              // Move to next year
              tmp, _ := strconv.Atoi(year)
              year = strconv.Itoa(tmp + 1)
              epoch = utils.Datetime2EpochNoYear(year, line[0:20])
            }
            if dateType == utils.Short && epoch + 3600 * 10 < lastEpoch {
              // Move to next day
              epoch = epoch + 60 * 60 * 24
            }
            lastEpoch = epoch
            // Some logs may be out of order due to disk flushing
            // Scan for extra 2 minutes to wait for logs syncing to disk
            if (endEpoch > 0 && epoch > endEpoch + 120) {
              break
            }
            if (startEpoch > 0 && epoch < startEpoch) || (endEpoch > 0 && epoch > endEpoch) {
              lastLineIncluded = false
              continue
            }
          }
        }
      }

      // Match patterns and write to file
      lastLineIncluded = true
      if utils.MatchAnyRegex(line, regexArray) {
        hasSth = true
        if needToWriteHeader {
          if _, err = fw.WriteString(">>>>>>>>>" + logInfo + "\n"); err != nil {
            panic(err)
          }
          needToWriteHeader = false
        }
        if !lastLineMatched {
          // Print num lines of leading context before each match
          lastLineMatched = true
          afterMatch = after
	        for e := buff.Front(); e != nil; e = e.Next() {
            str = utils.Interface2Str(e.Value)
            if _, err = fw.WriteString(str); err != nil {
              panic(err)
            }
          }
          buff.Init()
        }
        if _, err = fw.WriteString(line); err != nil {
          panic(err)
        }
      } else {
        lastLineMatched = false
        if afterMatch > 0 {
          // Print num lines of trailing context after each match
          if _, err = fw.WriteString(line); err != nil {
            panic(err)
          }
          afterMatch--
        } else {
          buff = appendList(buff, line, before)
        }
      }
    }

    f.Close()
  }

  // remove output file if it is empty.
  if (!hasSth) {
    os.Remove(outFile)
  }
}

// Get all the logs between startEpoch and endEpoch for a specific file
func GrepLogFile(filename, outFile string, patterns []string, before, after int) {
  var regexArray []*regexp.Regexp
  var hasSth bool = false
  afterMatch := 0
  buff := list.New()

  for _, pattern:= range patterns {
    re := regexp.MustCompile(pattern)
    regexArray = append(regexArray, re)
  }

  // Open output file for write
  fw, err := os.OpenFile(outFile, os.O_WRONLY|os.O_APPEND|os.O_CREATE, os.ModePerm)
  if err != nil {
    panic(err)
  }
  defer fw.Close()

  // Open log file for read
  f , err := os.OpenFile(filename, os.O_RDONLY, 0)
  if err != nil{
    panic(err)
  }
  br := bufio.NewReader(f)
  lastLineMatched := false
  needToWriteHeader := true

  // Read each line of the log
  for {
    line, err := br.ReadString('\n')
    if err == io.EOF {
      break
    }

    // Match patterns and write to file
    if utils.MatchAnyRegex(line, regexArray) {
      hasSth = true
      if needToWriteHeader {
        if _, err = fw.WriteString(">>>>>>>>>" + filename + "\n"); err != nil {
          panic(err)
        }
        needToWriteHeader = false
      }
      if !lastLineMatched {
        // Print num lines of leading context before each match
        lastLineMatched = true
        afterMatch = after
	      for e := buff.Front(); e != nil; e = e.Next() {
          str := utils.Interface2Str(e.Value)
          if _, err = fw.WriteString(str); err != nil {
            panic(err)
          }
        }
        buff.Init()
      }
      if _, err = fw.WriteString(line); err != nil {
        panic(err)
      }
    } else {
      lastLineMatched = false
      if afterMatch > 0 {
        // Print num lines of trailing context after each match
        if _, err = fw.WriteString(line); err != nil {
          panic(err)
        }
        afterMatch--
      } else {
        buff = appendList(buff, line, before)
      }
    }
  }
  f.Close()

  // remove output file if it is empty.
  if (!hasSth) {
    os.Remove(outFile)
  }
}

// Get all the logs between startEpoch and endEpoch from logDir
// The first parameter is a function which is used to parse timestamp from each line of the log
func FilterLogsFunc(line2Epoch func(string) int64, logDir, outFile, prefix, suffix string,
  patterns []string, startEpoch, endEpoch int64, before, after, head int) {
  var regexArray []*regexp.Regexp
  var hasSth bool = false
  afterMatch := 0
  buff := list.New()

  if !utils.CheckDirExisted(logDir) {
    return
  }

  for _, pattern:= range patterns {
    re := regexp.MustCompile(pattern)
    regexArray = append(regexArray, re)
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

  for _, file := range files {
    if file.IsDir() {
      continue
    }
    logInfo := file.Name()
    // Ignore files not starting with specified prefix
    if prefix != "" && !strings.HasPrefix(logInfo, prefix) {
      continue
    }

    // Ignore files not ending with specified suffix 
    if suffix != "" && !strings.HasSuffix(logInfo, suffix) {
      continue
    }

    logInfo = logDir + "/" + logInfo

    // Check the time of the last log in the file.
    t := utils.GetFileModTime(logInfo)
    epoch := t.Unix()
    if epoch > 0 && startEpoch > epoch {
      continue
    }

    // Check the time of the first log in the file.
    // The first line may not contains timestamp,
    // so we need to check several lines util we get one
    str := utils.LocalRun("head -" + strconv.Itoa(head) + " " + logInfo)
    for _, line := range strings.Split(str, "\n") {
      epoch = line2Epoch(line)
      if epoch > 0 {
        break
      }
    }
    if epoch > 0 && endEpoch < epoch {
      continue
    }

    // Open log file for read
    f , err := os.OpenFile(logInfo, os.O_RDONLY, 0)
    if err != nil{
      panic(err)
    }
    br := bufio.NewReader(f)
    lastLineMatched := false
    needToWriteHeader := true
    lastLineIncluded := false

    // Read each line of the log
    for {
      line, err := br.ReadString('\n')
      if err == io.EOF {
        break
      }

      if startEpoch > 0 || endEpoch > 0 {
        // For lines that have no timestamp detected,
        // only include it when the last line is also printed
        epoch = line2Epoch(line)
        if epoch < 0 {
          if !lastLineIncluded {
            continue
          }
        } else {
          // Some logs may be out of order due to disk flushing
          // Scan for extra 2 minutes to wait for logs syncing to disk
          if (endEpoch > 0 && epoch > endEpoch + 120) {
            break
          }
          if (startEpoch > 0 && epoch < startEpoch) || (endEpoch > 0 && epoch > endEpoch) {
            continue
          }
        }
      }

      // Match patterns and write to file
      if utils.MatchAnyRegex(line, regexArray) {
        hasSth = true
        if needToWriteHeader {
          if _, err = fw.WriteString(">>>>>>>>>" + logInfo + "\n"); err != nil {
            panic(err)
          }
          needToWriteHeader = false
        }
        if !lastLineMatched {
          // Print num lines of leading context before each match
          lastLineMatched = true
          afterMatch = after
	        for e := buff.Front(); e != nil; e = e.Next() {
            str = utils.Interface2Str(e.Value)
            if _, err = fw.WriteString(str); err != nil {
              panic(err)
            }
          }
          buff.Init()
        }
        if _, err = fw.WriteString(line); err != nil {
          panic(err)
        }
      } else {
        lastLineMatched = false
        if afterMatch > 0 {
          // Print num lines of trailing context after each match
          if _, err = fw.WriteString(line); err != nil {
            panic(err)
          }
          afterMatch--
        } else {
          buff = appendList(buff, line, before)
        }
      }
    }
    f.Close()
  }

  // remove output file if it is empty.
  if (!hasSth) {
    os.Remove(outFile)
  }
}

// Logs filter for GSQL
func GsqlLine2Epoch(line string) int64 {
  // I@20190522 20:50:36.739 tigergraph|127.0.0.1:58266|249820701 (Util.java:1124) Reset Kafka...
  const timeFormat = "20060102 15:04:05"
  if len(line) < 20 {
    return -1
  }
  ts := line[2:19]
  t, err := time.Parse(timeFormat, ts)
  if err != nil {
    return -1
  }
  return t.Unix()
}

func GsqlFilterLogs(logDir, outFile string, patterns []string,
  startEpoch, endEpoch int64, before, after int) {
  FilterLogsFunc(GsqlLine2Epoch, logDir, outFile, "GSQL_LOG", "",
    patterns, startEpoch, endEpoch, before, after, 50)
}

// Logs filter for Nginx
func NginxLine2Epoch(line string) int64 {
  const timeFormat = "02/Jan/2006:15:04:05 +0000"
  start := strings.Index(line, "[")
  end := strings.Index(line, "]")
  ts := line[start+1:end]
  t, err := time.Parse(timeFormat, ts)
  if err != nil {
    log.Fatalf("Failed to convert \"%s\" to epoch with error: %s", ts, err)
  }
  return t.Unix()
}

func NginxFilterLogs(logDir, outFile string, patterns []string,
  startEpoch, endEpoch int64, before, after int) {
  FilterLogsFunc(NginxLine2Epoch, logDir, outFile, "", "access.log",
    patterns, startEpoch, endEpoch, before, after, 10)
}

// Logs filter for Kafka
func kafkaLine2Epoch(line string) int64 {
  const timeFormat = "2006-01-02 15:04:05"
  start := strings.Index(line, "[")
  end := strings.Index(line, "]")
  if start < 0 || end < 0 || start > end - 5 {
    return -1
  }
  ts := line[start+1:end-4]
  t, err := time.Parse(timeFormat, ts)
  if err != nil {
    return -1
  }
  return t.Unix()
}

func KafkaFilterLogs(logDir, outFile, prefix string, patterns []string,
  startEpoch, endEpoch int64, before, after int) {
  FilterLogsFunc(kafkaLine2Epoch, logDir, outFile, prefix, "", patterns,
    startEpoch, endEpoch, before, after, 10)
}

// Logs filter for GraphStudio logs
func guiLine2Epoch(line string) int64 {
  tokens := strings.Split(line, "|")
  length := len(tokens)
  if length < 2 {
    return -1
  }
  str := strings.Split(tokens[length - 1], ".")[0]
  timeFormat := "2006-01-02T15:04:05"
  if (strings.Index(str, "T") < 0) {
    timeFormat = "2006-01-02 15:04:05"
  }
  t, err := time.Parse(timeFormat, str)
  if err != nil {
    return -1
  }
  return t.Unix()
}

func GuiFilterLogs(logDir, outFile, prefix string, patterns []string,
  startEpoch, endEpoch int64, before, after int) {
  FilterLogsFunc(guiLine2Epoch, logDir, outFile, prefix, "", patterns,
    startEpoch, endEpoch, before, after, 10)
}

