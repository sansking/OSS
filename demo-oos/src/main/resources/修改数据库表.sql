DELETE FROM remote_dir;

DELETE FROM remote_file;

SELECT * FROM remote_dir;

SELECT * FROM remote_file;

SELECT * FROM remote_file;

UPDATE remote_file SET download_state=0;

SELECT COUNT(*) FROM remote_dir
UNION ALL
SELECT COUNT(*) FROM remote_file;

