
/* Token Functon to convert time to INT
 * We did not use TigerGraph DATETIME because it does not have millisecond precision
 * We did not use std functions (memset and strptime) because they make loading slow
 */
#include <stdio.h>
#include <stdlib.h>
#include <cstring>
#include <TokenLib.hpp>
uint32_t nextInt(char*& timestamp_ptr, const char* timestamp_end_ptr){
  uint32_t int_value = 0;
  while (*timestamp_ptr >= '0' && *timestamp_ptr <= '9'
         && timestamp_ptr < timestamp_end_ptr) {
    int_value = int_value * 10 + *timestamp_ptr - '0';
    timestamp_ptr++;
  }
  timestamp_ptr++;  // jump over separator
  return int_value;
}

extern "C" int64_t ToMiliSeconds(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum) {
  const int mon_days[]
      = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};
  int64_t tyears, tdays, leaps1, leaps2, leaps3, sign, tm_msec;
  int64_t tm_year, tm_mon, tm_mday, tm_hour, tm_min, tm_sec;
  char* timestamp_ptr_ = const_cast<char*>(iToken[0]);
  char* timestamp_end_ptr_ = timestamp_ptr_ + 23;
  tm_year = nextInt(timestamp_ptr_, timestamp_end_ptr_);
  tm_mon = nextInt(timestamp_ptr_, timestamp_end_ptr_);
  tm_mday = nextInt(timestamp_ptr_, timestamp_end_ptr_);
  tm_hour = nextInt(timestamp_ptr_, timestamp_end_ptr_);
  tm_min = nextInt(timestamp_ptr_, timestamp_end_ptr_);
  tm_sec = nextInt(timestamp_ptr_, timestamp_end_ptr_);
  tm_msec = nextInt(timestamp_ptr_, timestamp_end_ptr_);

  tyears = tm_year - 1970;  // base time is 1970
  sign = (tyears >= 0) ? 1 : -1;
  leaps1 = (tyears + sign * 2) / 4;  // no of next two lines until year 2100.
  leaps2 = (tyears - 30)
           / 100;  // every 100 hundred years, this is an additional common year
  leaps3 = (tyears - 30)
           / 400;  // every 400 hundred years, this is an additional leap year
  tdays = mon_days[tm_mon - 1];
  tdays += tm_mday - 1;  // days of month passed.
  tdays = tdays + (tyears * 365) + leaps1 - leaps2 + leaps3;
  if (tm_mon <= 2 && ((tyears + 2) % 4 == 0)) {
    // leaf year: the 1st two months need -1 day.
    if (!((tyears - 30) % 100 == 0 && (tyears - 30) % 400 != 0)) {
      tdays--;
    }
  }
  // when it is a leap year before 1970/01/01, leaps1 is one more than it should
  // be.
  if (sign < 0 && (tyears + 2) % 4 == 0
      && !((tyears - 30) % 100 == 0 && (tyears - 30) % 400 != 0)) {
    tdays++;
  }
  return ((tdays * 86400) + (tm_hour * 3600) + (tm_min * 60) + tm_sec) * 1000 + tm_msec;
}
