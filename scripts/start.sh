#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_info "Starting library system..."

# 检查环境
if [ ! -f .env ]; then
    log_info "Creating .env from .env.example..."
    cp .env.example .env
fi

# 启动服务
docker-compose up -d

log_info "Services started successfully!"
docker-compose ps

log_info "Access the application at:"
log_info "  - Frontend: http://localhost"
log_info "  - Backend API: http://localhost:8080/api/v1"
log_info "  - API Documentation: http://localhost:8080/api/v1/swagger-ui.html"
