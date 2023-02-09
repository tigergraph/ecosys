SELECT
    country1Name AS 'country1:STRING',
    country2Name AS 'country2:STRING'
FROM countryPairsNumFriends
ORDER BY md5(concat(country1Name, country2Name))
LIMIT 400
