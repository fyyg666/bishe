@echo off
chcp 65001 >nul
echo ============================================
echo  图书馆管理系统 - 数据库迁移脚本
echo  将执行 V2.1.0 ~ V2.26.0 所有迁移
echo ============================================
echo.
echo 数据库: library_system
echo 主机: localhost:3306
echo.

set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASS=root
set MYSQL_DB=library_system

set MIGRATION_DIR=..\backend\src\main\resources\db\migration

echo 正在执行迁移脚本...
echo.

for %%f in (
    "%MIGRATION_DIR%\V2.10.0__add_notification_table.sql"
    "%MIGRATION_DIR%\V2.11.0__add_borrow_rule.sql"
    "%MIGRATION_DIR%\V2.12.0__add_book_reservation.sql"
    "%MIGRATION_DIR%\V2.13.0__add_sys_config_credit_data.sql"
    "%MIGRATION_DIR%\V2.14.0__add_marc_tables.sql"
    "%MIGRATION_DIR%\V2.15.0__add_marc_framework.sql"
    "%MIGRATION_DIR%\V2.16.0__add_authority_tables.sql"
    "%MIGRATION_DIR%\V2.17.0__add_z3950_source.sql"
    "%MIGRATION_DIR%\V2.18.0__add_acquisition_tables.sql"
    "%MIGRATION_DIR%\V2.18.1__add_purchase_order_tables.sql"
    "%MIGRATION_DIR%\V2.19.0__add_serial_tables.sql"
    "%MIGRATION_DIR%\V2.20.0__add_digital_resource.sql"
    "%MIGRATION_DIR%\V2.21.0__add_report_template.sql"
    "%MIGRATION_DIR%\V2.22.0__add_serial_claim.sql"
    "%MIGRATION_DIR%\V2.23.0__add_serial_routing.sql"
    "%MIGRATION_DIR%\V2.24.0__add_branch_tables.sql"
    "%MIGRATION_DIR%\V2.25.0__add_performance_indexes.sql"
    "%MIGRATION_DIR%\V2.26.0__add_serial_issue_deleted.sql"
) do (
    if exist %%f (
        echo [OK] %%~nxf
        mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < %%f 2>nul
        if errorlevel 1 (
            echo [WARN] %%~nxf 执行有警告（可能表已存在），继续...
        )
    ) else (
        echo [SKIP] %%~nxf 不存在
    )
)

echo.
echo ============================================
echo  迁移完成！
echo ============================================
pause
