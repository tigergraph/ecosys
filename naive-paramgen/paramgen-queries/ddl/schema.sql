DROP TABLE IF EXISTS cityNumPersons;
DROP TABLE IF EXISTS cityPairsNumFriends;
DROP TABLE IF EXISTS companyNumEmployees;
DROP TABLE IF EXISTS countryNumMessages;
DROP TABLE IF EXISTS countryNumPersons;
DROP TABLE IF EXISTS countryPairsNumFriends;
DROP TABLE IF EXISTS creationDayAndLengthCategoryNumMessages;
DROP TABLE IF EXISTS creationDayAndTagClassNumMessages;
DROP TABLE IF EXISTS creationDayNumMessages;
DROP TABLE IF EXISTS creationDayAndTagNumMessages;
DROP TABLE IF EXISTS languageNumPosts;
DROP TABLE IF EXISTS lengthNumMessages;
DROP TABLE IF EXISTS people2Hops;
DROP TABLE IF EXISTS people4Hops;
DROP TABLE IF EXISTS personDisjointEmployerPairs;
DROP TABLE IF EXISTS personNumFriends;
DROP TABLE IF EXISTS tagClassNumMessages;
DROP TABLE IF EXISTS tagClassNumTags;
DROP TABLE IF EXISTS tagNumMessages;
DROP TABLE IF EXISTS tagNumPersons;

CREATE TABLE cityNumPersons(cityId bigint not null, cityName varchar not null, frequency bigint not null);
CREATE TABLE cityPairsNumFriends(
    city1Id bigint not null, city2Id bigint not null, city1Name varchar not null, city2Name varchar not null,
    country1Id bigint not null, country2Id bigint not null, country1Name varchar not null, country2Name varchar not null,
    frequency bigint not null);
CREATE TABLE companyNumEmployees(companyId bigint not null, companyName varchar not null, frequency bigint not null);
CREATE TABLE countryNumMessages(countryId bigint not null, countryName varchar not null, frequency bigint not null);
CREATE TABLE countryNumPersons(countryId bigint not null, countryName varchar not null, frequency bigint not null);
CREATE TABLE countryPairsNumFriends(country1Id bigint not null, country2Id bigint not null, country1Name varchar not null, country2Name varchar not null, frequency bigint not null);
CREATE TABLE creationDayAndLengthCategoryNumMessages(creationDay date not null, lengthCategory int not null, frequency bigint not null);
CREATE TABLE creationDayAndTagClassNumMessages(creationDay date not null, tagclassId bigint not null, tagClassName varchar not null, frequency bigint not null);
CREATE TABLE creationDayAndTagNumMessages(creationDay date not null, tagId bigint not null, tagName varchar not null, frequency bigint not null);
CREATE TABLE creationDayNumMessages(creationDay date not null, frequency bigint not null);
CREATE TABLE languageNumPosts(language varchar not null, frequency bigint not null);
CREATE TABLE lengthNumMessages(length int not null, frequency bigint not null);
CREATE TABLE people2Hops(person1id bigint not null, person2id bigint not null, person1creationDate date not null, person1deletionDate date not null, person2creationDate date not null, person2deletionDate date not null);
CREATE TABLE people4Hops(person1id bigint not null, person2id bigint not null, person1creationDate date not null, person1deletionDate date not null, person2creationDate date not null, person2deletionDate date not null);
CREATE TABLE personDisjointEmployerPairs(person2id bigint not null, companyName varchar not null, companyId bigint not null, person2creationDate date not null, person2deletionDate date not null);
CREATE TABLE personNumFriends(personId bigint not null, personCreationDate date not null, personDeletionDate date not null, frequency bigint not null);
CREATE TABLE tagClassNumMessages(tagclassId bigint not null, tagclassName varchar not null, frequency bigint not null);
CREATE TABLE tagClassNumTags(tagclassId bigint not null, tagclassName varchar not null, frequency bigint not null);
CREATE TABLE tagNumMessages(tagId bigint not null, tagName varchar not null, frequency bigint not null);
CREATE TABLE tagNumPersons(tagId bigint not null, tagName varchar not null, frequency bigint not null);
