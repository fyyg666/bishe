#!/bin/bash
# JVM启动脚本 - Linux/Mac
# 配置堆内存、GC参数和优化选项

JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof -Xlog:gc*:file=./logs/gc.log:time,uptime,level,tags:filecount=10,filesize=10M"

echo "Starting Library System with JVM Options: $JAVA_OPTS"
java $JAVA_OPTS -jar target/library-system-2.0.0.jar --spring.profiles.active=prod
