/*
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as it is for benchmark reproducible purpose.
* anyone can use it for benchmark purpose with the
* acknowledgement to TigerGraph.
* Author: Litong Shen litong.shen@tigergraph.com
*/
#include <iostream>
#include <unordered_set>
#include <fstream>
#include <stdio.h>
#include <sstream>
#include <vector>

using namespace std;

class SplitPlace{
public:
  vector<string> _inputPath;
  string _cityPath;
  string _countryPath;
  string _continentPath;
  int _totalFileCount = 6;

  SplitPlace(string inputAndOutputPath, string totalFileCount){
     _totalFileCount = stoi(totalFileCount);
     int tmpCount = stoi(totalFileCount);
     while(--tmpCount >= 0) {
	// e.g. format of place file is place_2_0.csv
     	string tmpInputPath = inputAndOutputPath + "place_" + to_string(tmpCount) + "_0.csv" ;
	_inputPath.push_back(tmpInputPath);
     }
     _cityPath = inputAndOutputPath + "city.csv";
     _countryPath = inputAndOutputPath + "country.csv";
     _continentPath = inputAndOutputPath + "continent.csv";
  }

  /** this function read place_*_0.csv vertex files
   *  then generate city, contry, continent vertex files
   */
  void generateHashSet( ) {
  // initialize read inputFile
    ofstream cityFile(_cityPath);
    ofstream countryFile(_countryPath);
    ofstream continentFile(_continentPath);

    // process header
    string header = "id|name|url|type";
    cityFile << header << "\n";
    countryFile << header << "\n";
    continentFile << header << "\n";

    for (int i = 0; i < _totalFileCount; i++) {
      ifstream file(_inputPath[i]);
      if (file.is_open()) {
        string line;
        // skip header for each input file
        getline(file, line);

        // read file line by line
        while(getline(file, line)) {
          istringstream is(line);
          string content;
	  vector<string> tmp;
          while(getline(is, content, '|')) {
            tmp.push_back(content);
          }

          string type = tmp[3];
	  if(type == "country") {
	    //write country vertex file
	    countryFile << line;
	    countryFile << "\n";
	  } else if (type == "city") {
	    // write city vertex file
	    cityFile << line;
	    cityFile << "\n";
	  } else {
	    // write continent vertex file
	    continentFile << line;
	    continentFile << "\n";
	  }
        }
      }
      file.close();
    }
    countryFile.close();
    cityFile.close();
    continentFile.close();
  }
};

int main(int argc, char *argv[]) {
  SplitPlace cityCountryContinent(argv[1], argv[2]);
  cityCountryContinent.generateHashSet();
}
