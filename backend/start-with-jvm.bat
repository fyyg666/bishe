@echo off
REM ============================================================================
REM 图书馆系统V2.0 启动脚本
REM FIXED: PERF-005 添加JVM优化参数
REM ============================================================================

REM 设置应用名称和端口
set APP_NAME=library-system
set APP_PORT=8080

REM JVM优化参数配置
REM -Xms512m: 初始堆内存
REM -Xmx2g: 最大堆内存
REM -Xmn256m: 年轻代大小（建议为堆的1/4-1/3）
REM -XX:+UseG1GC: 使用G1垃圾收集器
REM -XX:MaxGCPauseMillis=200: 最大GC停顿时间
REM -XX:+HeapDumpOnOutOfMemoryError: OOM时生成堆转储
REM -XX:HeapDumpPath: 堆转储路径
REM -XX:+PrintGCDetails: 打印GC详情
REM -XX:+PrintGCDateStamps: 打印GC时间戳
REM -Xloggc: GC日志文件
REM -Dspring.profiles.active: 激活Spring Profile

set JVM_OPTS=-Xms512m -Xmx2g -Xmn256m ^
    -XX:+UseG1GC ^
    -XX:MaxGCPauseMillis=200 ^
    -XX:+HeapDumpOnOutOfMemoryError ^
    -XX:HeapDumpPath="./logs/heapdump.hprof" ^
    -XX:+PrintGCDetails ^
    -XX:+PrintGCDateStamps ^
    -Xloggc:"./logs/gc-%date:~0,4%%date:~5,2%%date:~8,2%.log"

REM 编码配置
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dclient.encoding.override=UTF-8 -Duser.language=zh_CN -Duser.region=zh_CN

REM Spring配置
set SPRING_OPTS=--spring.profiles.active=dev --server.port=%APP_PORT%

echo ==========================================
echo 图书馆系统V2.0 启动中...
echo JVM参数: %JVM_OPTS%
echo 激活Profile: dev
echo ==========================================

REM 确保logs目录存在
if not exist "logs" mkdir logs

REM 启动应用
java %JVM_OPTS% %JAVA_OPTS% %SPRING_OPTS% -jar target/%APP_NAME%.jar

pause
