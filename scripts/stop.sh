#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_info "Stopping library system..."

# 停止服务（优雅关闭）
docker-compose down

log_info "Services stopped successfully!"

log_info "To remove all data volumes, run: docker-compose down -v"
