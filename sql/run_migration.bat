@echo off
mysql -u root -proot library_system < "%~dp0migration_20260510_add_violation_count.sql"
echo Exit code: %ERRORLEVEL%
pause
