ALTER TABLE book ADD FULLTEXT INDEX ft_title_author (title, author) WITH PARSER ngram;
