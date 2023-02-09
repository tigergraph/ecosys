SELECT
    country1Name AS 'country1:STRING',
    country2Name AS 'country2:STRING'
FROM (
    SELECT
        country1Name,
        country2Name,
        frequency AS freq,
        abs(frequency - (SELECT percentile_disc(0.98) WITHIN GROUP (ORDER BY frequency) FROM countryPairsNumFriends)) AS diff
    FROM countryPairsNumFriends
    ORDER BY diff, country1Name, country2Name
    LIMIT 50
)
ORDER BY md5(concat(country1Name, country2Name))
