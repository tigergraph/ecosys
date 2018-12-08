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

class SplitEdges {
public:
  vector<string> _organisationIsLocatedInPlaceEdgePath;
  vector<string> _placeIsPartOfPlaceEdgePath;
  string _countryVertexPath;
  string _universityIsLocatedInCity;
  string _companyIsLocatedInCountry;
  string _cityIsPartOfCountry;
  string _countryIsPartOfContinent;
  int _totalFileCount = 6;
  unordered_set<string> _countryId;
  
  SplitEdges (string dataPath, string totalFileCount) {
    _totalFileCount = stoi(totalFileCount);
    int tmpCount = stoi(totalFileCount);
    while (--tmpCount >= 0) {
      string tmpOrganisationIsLocatedInPlaceEdgePath = dataPath 
	      + "organisation_isLocatedIn_place_" + to_string(tmpCount) + "_0.csv";
      string tmpPlaceIsPartOfPlaceEdgePath = dataPath
	      + "place_isPartOf_place_" + to_string(tmpCount) + "_0.csv";
      _organisationIsLocatedInPlaceEdgePath.push_back(tmpOrganisationIsLocatedInPlaceEdgePath);
      _placeIsPartOfPlaceEdgePath.push_back(tmpPlaceIsPartOfPlaceEdgePath);
    }
   
    _countryVertexPath = dataPath + "country.csv";
    _universityIsLocatedInCity = dataPath + "university_isLocatedIn_city.csv";
    _companyIsLocatedInCountry = dataPath + "company_isLocatedIn_Country.csv";
    _cityIsPartOfCountry = dataPath + "city_isPartOf_country.csv";
    _countryIsPartOfContinent = dataPath + "country_isPartOf_continent.csv";
  }

  /** this function first construct a hashmap with contries
   *  based on the hashmap, distinguish and generate data files for
   *  company_isLocatedIn_country, university_isLocatedIn_city
   *  city_isPartOf_country, country_isPartOf_continent
   */
  void generateEdge() {
    // initialize outputFile and file for construct hashmap
    ifstream countryVertexFile(_countryVertexPath);
    ofstream universityIsLocatedInCityFile(_universityIsLocatedInCity);
    ofstream companyIsLocatedInCountryFile(_companyIsLocatedInCountry);
    ofstream cityIsPartOfCountryFile(_cityIsPartOfCountry);
    ofstream countryIsPartOfContinentFile(_countryIsPartOfContinent);

    // construct hashset of country
    if (countryVertexFile.is_open()) {
      string vertexHeader;
      string countryRecord;
      getline(countryVertexFile, vertexHeader);
      while (getline(countryVertexFile, countryRecord)) {
        istringstream isCountry(countryRecord);
        string countryAttribute;
        vector<string> curCountryRecord;

	while(getline(isCountry, countryAttribute, '|')) {
	  curCountryRecord.push_back(countryAttribute);
	}
        _countryId.emplace(curCountryRecord[0]);
      }
    }
    countryVertexFile.close();

    // processing header
    universityIsLocatedInCityFile << "Organisation.id|Place.id\n";
    companyIsLocatedInCountryFile << "Organisation.id|Place.id\n";
    cityIsPartOfCountryFile << "Place.id|Place.id\n";
    countryIsPartOfContinentFile << "Place.id|Place.id\n";

    for (int i = 0; i < _totalFileCount; i++) {
      ifstream organisationIsLocatedInPlaceEdgePath(_organisationIsLocatedInPlaceEdgePath[i]);
      ifstream placeIsPartOfPlaceEdgePath(_placeIsPartOfPlaceEdgePath[i]);
      // split organisation_isLocatedIn_place
      splitAndWrite(organisationIsLocatedInPlaceEdgePath, companyIsLocatedInCountryFile, universityIsLocatedInCityFile);
      // split place_isPartOf_place
      splitAndWrite(placeIsPartOfPlaceEdgePath, cityIsPartOfCountryFile, countryIsPartOfContinentFile);
    }
    companyIsLocatedInCountryFile.close();
    universityIsLocatedInCityFile.close();
    cityIsPartOfCountryFile.close();
    countryIsPartOfContinentFile.close();
  }

  /**
   * this function write generated edge files into disk
   **/
  void splitAndWrite(ifstream& edgeFile, ofstream& edge1, ofstream& edge2) {
    if (edgeFile.is_open()) {
      string edgeRecord;
      // skip header in each file
      getline(edgeFile, edgeRecord);

      // read file line by line
      while(getline(edgeFile, edgeRecord)) {
        istringstream isEdge(edgeRecord);
        string curEdge;
        vector<string> fromIdToId;
        while(getline(isEdge, curEdge, '|')) {
          fromIdToId.push_back(curEdge);
        }

        string locationId = fromIdToId[1];
        if(_countryId.find(locationId) != _countryId.end()) {
          // write companyIsLocatedInCountry file
	  // write cityIsPartOfCountry File
          edge1 << edgeRecord;
          edge1 << "\n";
        } else {
          // write universityIsLocatedInCityFile file
	  // write countryIsPartOfContinent File
          edge2 << edgeRecord;
          edge2 << "\n";
        }
      }
    }
    edgeFile.close();
  } 
};

int main(int argc, char *argv[]) {
  SplitEdges splitEdges(argv[1], argv[2]);
  splitEdges.generateEdge();
}
