#!/bin/bash

# 图书馆管理系统V2.0 - Linux/Mac 启动脚本
# 包含优化的JVM参数配置
# 执行日期: 2026-04-24

# =========================================
# JVM参数配置
# =========================================

# 基础内存设置（根据服务器内存调整）
# 推荐：服务器内存的50-75%
JAVA_OPTS="
  -Xms2g
  -Xmx2g

  # 元空间设置（类元数据）
  -XX:MetaspaceSize=256m
  -XX:MaxMetaspaceSize=512m

  # G1垃圾收集器（适合堆内存>4GB，如果-Xmx<4g，改用 -XX:+UseParallelGC）
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:G1HeapRegionSize=16m

  # 禁用显式GC（System.gc()）
  -XX:+DisableExplicitGC

  # OOM时生成堆转储
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=./logs/heapdump.hprof

  # GC日志配置（Java 11+）
  -Xlog:gc*:./logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m

  # OOM时退出（避免服务假死）
  -XX:+ExitOnOutOfMemoryError

  # 编码和时区
  -Dfile.encoding=UTF-8
  -Duser.timezone=Asia/Shanghai

  # 网络参数（高并发时需要）
  -Djava.net.preferIPv4Stack=true

  # 关闭JMX远程连接（生产环境安全配置）
  -Dcom.sun.management.jmxremote=false
"

# =========================================
# 前置检查
# =========================================

# 创建日志目录
mkdir -p ./logs

# 检查JAR文件是否存在
if [ ! -f "library-system-v2.jar" ]; then
    echo "错误：找不到 library-system-v2.jar 文件"
    echo "请确保当前目录下存在该文件，或修改脚本中的路径"
    exit 1
fi

# 检查端口是否被占用（假设使用8080端口）
PORT=8080
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "警告：端口 $PORT 已被占用"
    echo "请停止占用该端口的进程，或修改 application.yml 中的端口配置"
    read -p "是否继续启动？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# =========================================
# 启动应用
# =========================================

echo "========================================="
echo "图书馆管理系统V2.0 - 正在启动..."
echo "JVM参数："
echo "$JAVA_OPTS" | tr ' ' '\n' | grep -v '^$'
echo "========================================="

# 后台启动（生产环境）
# nohup java $JAVA_OPTS -jar library-system-v2.jar --spring.profiles.active=prod > ./logs/app.log 2>&1 &

# 前台启动（开发/调试环境）
java $JAVA_OPTS -jar library-system-v2.jar --spring.profiles.active=prod

# =========================================
# 启动后验证
# =========================================

# 如果使用后台启动，可以取消注释以下代码进行验证
# sleep 10  # 等待应用启动
# 
# # 检查应用是否启动成功
# if curl -s http://localhost:8080/api/v1/actuator/health | grep -q "UP"; then
#     echo "应用启动成功！"
#     echo "进程ID: $(cat ./app.pid)"
# else
#     echo "应用启动失败，请查看日志：./logs/app.log"
#     exit 1
# fi

# =========================================
# 说明
# =========================================

# 1. 根据服务器内存调整 -Xms 和 -Xmx 参数
#    推荐值：
#    - 测试环境：1g ~ 2g
#    - 生产环境（8GB内存服务器）：4g ~ 6g
#    - 生产环境（16GB内存服务器）：8g ~ 12g
#
# 2. 如果堆内存 < 4GB，建议将 -XX:+UseG1GC 改为 -XX:+UseParallelGC
#
# 3. 如果使用 Java 8，GC日志配置需要改为：
#    -XX:+PrintGCDetails
#    -XX:+PrintGCDateStamps
#    -Xloggc:./logs/gc.log
#    -XX:+UseGCLogFileRotation
#    -XX:NumberOfGCLogFiles=5
#    -XX:GCLogFileSize=100M
#
# 4. 生产环境建议使用 systemd 或 docker 管理进程，而非 nohup
#
# 5. 监控JVM：jstat -gcutil <pid> 2000  (每2秒打印一次GC信息)
