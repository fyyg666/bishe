@echo off
setlocal EnableDelayedExpansion

REM 图书馆管理系统V2.0 - Windows 启动脚本
REM 包含优化的JVM参数配置
REM 执行日期: 2026-04-24

REM =========================================
REM JVM参数配置
REM =========================================

set JAVA_OPTS=-Xms2g -Xmx2g ^
-XX:MetaspaceSize=256m ^
-XX:MaxMetaspaceSize=512m ^
-XX:+UseG1GC ^
-XX:MaxGCPauseMillis=200 ^
-XX:+ParallelRefProcEnabled ^
-XX:G1HeapRegionSize=16m ^
-XX:+DisableExplicitGC ^
-XX:+HeapDumpOnOutOfMemoryError ^
-XX:HeapDumpPath=./logs/heapdump.hprof ^
-Xlog:gc*:./logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m ^
-XX:+ExitOnOutOfMemoryError ^
-Dfile.encoding=UTF-8 ^
-Duser.timezone=Asia/Shanghai ^
-Djava.net.preferIPv4Stack=true ^
-Dcom.sun.management.jmxremote=false

REM =========================================
REM 前置检查
REM =========================================

REM 创建日志目录
if not exist "logs" (
    mkdir logs
)

REM 检查JAR文件是否存在
if not exist "library-system-v2.jar" (
    echo 错误：找不到 library-system-v2.jar 文件
    echo 请确保当前目录下存在该文件，或修改脚本中的路径
    pause
    exit /b 1
)

REM 检查端口是否被占用（假设使用8080端口）
netstat -ano | findstr :8080 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo 警告：端口 8080 已被占用
    echo 请停止占用该端口的进程，或修改 application.yml 中的端口配置
    set /p "continue=是否继续启动？(y/n) "
    if /i not "!continue!"=="y" (
        exit /b 1
    )
)

REM =========================================
REM 启动应用
REM =========================================

echo =========================================
echo 图书馆管理系统V2.0 - 正在启动...
echo JVM参数：
echo %JAVA_OPTS%
echo =========================================

REM 后台启动（生产环境）- 取消注释以下行
REM start /B java %JAVA_OPTS% -jar library-system-v2.jar --spring.profiles.active=prod ^> ./logs/app.log 2^>^&1

REM 前台启动（开发/调试环境）
java %JAVA_OPTS% -jar library-system-v2.jar --spring.profiles.active=prod

REM =========================================
REM 说明
REM =========================================

REM 1. 根据服务器内存调整 -Xms 和 -Xmx 参数
REM    推荐值：
REM    - 测试环境：1g ~ 2g
REM    - 生产环境（8GB内存服务器）：4g ~ 6g
REM    - 生产环境（16GB内存服务器）：8g ~ 12g
REM
REM 2. 如果堆内存 < 4GB，建议将 -XX:+UseG1GC 改为 -XX:+UseParallelGC
REM
REM 3. 如果使用 Java 8，GC日志配置需要改为：
REM    -XX:+PrintGCDetails
REM    -XX:+PrintGCDateStamps
REM    -Xloggc:./logs/gc.log
REM    -XX:+UseGCLogFileRotation
REM    -XX:NumberOfGCLogFiles=5
REM    -XX:GCLogFileSize=100M
REM
REM 4. 生产环境建议使用 NSSM (Non-Sucking Service Manager) 将应用注册为Windows服务
REM
REM 5. 监控JVM：jstat -gcutil <pid> 2000  (每2秒打印一次GC信息)

pause
