SELECT
    date_trunc('day', startDate) AS 'startDate:DATE',
    150 - salt*5 AS 'lengthThreshold:INT',
    string_agg(lng, ';') AS 'languages:STRING[]'
FROM (SELECT
        salt,
        startDate,
        lang_perm,
        ROW_NUMBER() OVER(PARTITION BY lang_perm, salt ORDER BY md5(concat(lng, lang_perm))) rn,
        lng
    FROM (SELECT
            salt,
            startDate,
            lang_perm,
            lng
        FROM (SELECT
                    salt,
                    startDate,
                    lang.language AS lng
                FROM
                    (SELECT
                        (SELECT percentile_disc(0.79) WITHIN GROUP (ORDER BY creationDay) AS anchorDate FROM creationDayNumMessages)
                            + INTERVAL (salt*3) DAY
                            AS startDate,
                            salt
                    FROM (SELECT unnest(generate_series(1, 20)) AS salt)
                    ) sd,
                    (SELECT
                        language,
                        frequency AS freq,
                        abs(frequency - (SELECT percentile_disc(0.98) WITHIN GROUP (ORDER BY frequency) FROM languageNumPosts)) AS diff
                    FROM languageNumPosts
                    ORDER BY diff, language
                    LIMIT 10
                    ) lang
                ORDER BY salt, md5(concat(lang.language, salt))
            ),
            (SELECT unnest(generate_series(1, 4)) AS lang_perm)
        ORDER BY startDate, salt, md5(3532569367*salt + 342663089*lang_perm)
    )
)
WHERE rn <= 3
GROUP BY startDate, salt, lang_perm
ORDER BY md5(startDate), lang_perm
