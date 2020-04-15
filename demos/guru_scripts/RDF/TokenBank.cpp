
#include <stdio.h>
#include <stdint.h>
#include <iostream>
#include <cstring>
#include <vector>
#include <string>
#include <stdbool.h>
#include <cstdlib>
#include <algorithm>
#include <TokenLib.hpp>
#include <boost/algorithm/string/replace.hpp>

 /**
  * this function concatenate all input tokens into one big token
  *
  */
extern "C" void _Concat(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum,
    char* const oToken, uint32_t& oTokenLen){
      
  int k = 0;
  for (int i=0; i < iTokenNum; i++){
    for (int j =0; j < iTokenLen[i]; j++) {
           oToken[k++]=iToken[i][j];
    }
  }
  oTokenLen = k;
}

/**
 * This function convert iToken char array of size iTokenLen, reverse order 
 * and put it in oToken.
 *
 * Only one token input.
 */
extern "C"  void Reverse (const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum, 
    char *const oToken, uint32_t& oTokenLen) {

  uint32_t j = 0;
  for (uint32_t i = iTokenLen[0]; i > 0; i--) {
    oToken[j++] = iToken[0][i-1];
  }
  oTokenLen = j;
}

extern "C" void subject (const char* const iToken[], uint32_t iTokenLen[], 
      uint32_t iTokenNum, char* const oToken, uint32_t& oTokenLen) {
  uint32_t j = 0;
  if (iTokenLen[0] > 1000) {oTokenLen=0;return;}
  for (uint32_t i = 0;  i < iTokenLen[0]; i ++) {
    if (iToken[0][i] != '<' && iToken[0][i] != ' ') {
      oToken[j++] = iToken[0][i];
    }
  }
  oTokenLen = j;
}

extern "C" void object (const char* const iToken[], uint32_t iTokenLen[], 
      uint32_t iTokenNum, char* const oToken, uint32_t& oTokenLen) {
  if (iTokenLen[0] > 1000) {oTokenLen=0;return;}

  uint32_t j = 0;
  for (uint32_t i = 2;  i < iTokenLen[0]; i ++) {
    if (iToken[0][i] != '<' && iToken[0][i] != '\"' && !(iToken[0][i] == '.' && i == iTokenLen[0] -1)) {
      oToken[j++] = iToken[0][i];
    }
  }
  oTokenLen = j;
}

extern "C" void predicate (const char* const iToken[], uint32_t iTokenLen[],
      uint32_t iTokenNum, char* const oToken, uint32_t& oTokenLen) {
  if (iTokenLen[0] > 1000) {oTokenLen=0;return;}
  string str = string(iToken[0],iTokenLen[0]);
  str = str.substr(str.find_last_of("/") + 1); 
  uint32_t j = 0;
  for (uint32_t i = 0;  i < str.size(); i ++) {
 //    if (isalpha(str[i]) || isdigit(str[i])) {
       oToken[j++] = str[i];
 //    }
  }
  oTokenLen = j;  
}
/**
 * This function convert iToken to integer and compare with 3.
 * If it is greater than 3, return true. Otherwise, return false.
 *
 * Only one token input.
 */
extern "C"  bool GreaterThan3(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum) {

  int tmp = atoi(iToken[0]);

  if (tmp >3) {
    return true;
  }
  return false;
}

/**
 * This function convert each input token to an integer, sum them and compare 
 * the sum with 3. If it is greater than 3, return true. Otherwise, return false.
 *
 */
extern "C" bool SumGreaterThan3(const char* const iToken[], uint32_t iTokenLen[], uint32_t iTokenNum) 
{
  int k = 0;
  int sum = 0;

  for (int i=0; i < iTokenNum; i++) {
    int tmp = atoi(iToken[i]);
    sum += tmp;
  }

  if (sum > 3) {
    return true;
  }

  return false;
}

/**
 *  Unit testing of the token bank functions
 */ 
int main(){

  char* a[2];
  char d[100]={'a','b','c','d','e','f'};

  a[0]=&d[0];
  a[1]=&d[3];

  uint32_t len[2];

  len[0] = 3;
  len[1] = 3;


  char b[100];
  uint32_t  outlen;
  _Concat(a,len,2, b, outlen);
  for(int i =0; i<outlen; i++){
    std::cout<<b[i]<<",";
  }
  std::cout<<std::endl;

}

