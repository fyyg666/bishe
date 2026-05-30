#!/usr/bin/env bash
# ============================================
# ESLint Code Quality Check Script
# 用于 CI/CD 流水线中阻断存在 Error 级别的代码违规
# ============================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_DIR/frontend"

echo "============================================"
echo "  ESLint 代码质量检查"
echo "  扫描目录: src/"
echo "  文件类型: .js, .jsx"
echo "============================================"
echo ""

# 切换到前端目录
cd "$FRONTEND_DIR"

# 检查 node_modules 是否存在
if [ ! -d "node_modules" ]; then
    echo "[INSTALL] 安装依赖..."
    npm ci --silent 2>/dev/null || npm install --silent
fi

# 运行 ESLint
# ESLint 9.x flat config 自动识别 configured plugin 的文件类型
# --quiet: 仅显示错误级别（过滤警告），有错误时退出码非零
# --format stylish: 可读性好的输出格式
echo "[RUNNING] npx eslint src/ --format stylish --quiet"
echo ""

npx eslint src/ \
    --format stylish \
    --quiet 2>&1 || exit $?

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ 代码检查通过，未发现错误级别违规。"
else
    echo "❌ 代码检查未通过！存在错误级别违规，请修复后重试。"
fi

echo ""

exit $EXIT_CODE
