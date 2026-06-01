#!/bin/bash

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境变量文件
check_env_file() {
    if [ ! -f .env ]; then
        log_warn ".env file not found. Creating from .env.example..."
        cp .env.example .env
        log_info "Please edit .env file with your configuration"
        exit 1
    fi
}

# 检查Docker是否运行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker first."
        exit 1
    fi
}

# 部署函数
deploy() {
    local environment=$1
    
    log_info "Starting deployment to $environment environment..."
    
    check_env_file
    check_docker
    
    if [ "$environment" == "prod" ]; then
        log_info "Deploying to production..."
        
        # 拉取最新镜像
        log_info "Pulling latest images..."
        docker-compose -f docker-compose.prod.yml pull
        
        # 重建并重启应用服务（短暂停机）
        log_info "Rebuilding and restarting: backend..."
        docker-compose -f docker-compose.prod.yml up -d --no-deps --build backend
        
        log_info "Rebuilding and restarting: frontend..."
        docker-compose -f docker-compose.prod.yml up -d --no-deps --build frontend
        
        log_info "Production deployment complete!"
        log_info "Services:"
        docker-compose -f docker-compose.prod.yml ps
    else
        log_info "Deploying to development..."
        
        # 重建并重启应用服务（短暂停机）
        log_info "Rebuilding and restarting: backend..."
        docker-compose up -d --no-deps --build backend
        
        log_info "Rebuilding and restarting: frontend..."
        docker-compose up -d --no-deps --build frontend
        
        log_info "Development deployment complete!"
        log_info "Services:"
        docker-compose ps
    fi
}

# 回滚函数
rollback() {
    local environment=$1
    
    log_warn "Rolling back $environment environment..."
    
    check_docker
    
    if [ "$environment" == "prod" ]; then
        # 重启应用服务（使用现有镜像，不重新构建）
        log_info "Restarting previous version of backend..."
        docker-compose -f docker-compose.prod.yml up -d --no-deps backend
        
        log_info "Restarting previous version of frontend..."
        docker-compose -f docker-compose.prod.yml up -d --no-deps frontend
        
        log_info "Rollback complete!"
        docker-compose -f docker-compose.prod.yml ps
    else
        docker-compose up -d --no-deps backend
        docker-compose up -d --no-deps frontend
        
        log_info "Rollback complete!"
        docker-compose ps
    fi
}

# 查看日志
show_logs() {
    local environment=$1
    local service=$2
    
    if [ "$environment" == "prod" ]; then
        if [ -z "$service" ]; then
            docker-compose -f docker-compose.prod.yml logs -f
        else
            docker-compose -f docker-compose.prod.yml logs -f "$service"
        fi
    else
        if [ -z "$service" ]; then
            docker-compose logs -f
        else
            docker-compose logs -f "$service"
        fi
    fi
}

# 主函数
main() {
    case "$1" in
        dev)
            deploy "dev"
            ;;
        prod)
            deploy "prod"
            ;;
        rollback-dev)
            rollback "dev"
            ;;
        rollback-prod)
            rollback "prod"
            ;;
        logs-dev)
            show_logs "dev" "$2"
            ;;
        logs-prod)
            show_logs "prod" "$2"
            ;;
        *)
            echo "Usage: $0 {dev|prod|rollback-dev|rollback-prod|logs-dev|logs-prod} [service_name]"
            echo ""
            echo "Commands:"
            echo "  dev              Deploy to development environment"
            echo "  prod             Deploy to production environment"
            echo "  rollback-dev     Rollback development environment"
            echo "  rollback-prod    Rollback production environment"
            echo "  logs-dev [svc]   Show logs for development (optionally for specific service)"
            echo "  logs-prod [svc]  Show logs for production (optionally for specific service)"
            exit 1
            ;;
    esac
}

main "$@"
