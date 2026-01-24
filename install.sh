#!/bin/bash

# Java Framework 快速安装脚本
# 执行 mvn install，跳过测试
# 这是一个多模块项目，需要先安装 framework-dependencies，再安装 framework-infra

set -e

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Java Framework 本地安装"
echo "=========================================="
echo ""

# 检查 mise 是否可用
if command -v mise &> /dev/null; then
    echo "使用 mise 管理的 Maven 环境..."
    echo ""
    
    # 先安装 framework-dependencies 模块
    echo "步骤 1/2: 安装 framework-dependencies 模块..."
    echo "执行: cd framework-dependencies && mise exec -- mvn clean install -DskipTests"
    echo ""
    cd framework-dependencies
    mise exec -- mvn clean install -DskipTests "$@"
    cd ..
    
    echo ""
    echo "步骤 2/2: 安装 framework-infra 模块..."
    echo "执行: cd framework-infra && mise exec -- mvn clean install -DskipTests"
    echo ""
    cd framework-infra
    mise exec -- mvn clean install -DskipTests "$@"
    cd ..
else
    echo "警告: 未找到 mise，使用系统 Maven..."
    echo "建议: 安装 mise 并使用项目配置的 Maven 版本"
    echo ""
    
    # 先安装 framework-dependencies 模块
    echo "步骤 1/2: 安装 framework-dependencies 模块..."
    echo "执行: cd framework-dependencies && mvn clean install -DskipTests"
    echo ""
    cd framework-dependencies
    mvn clean install -DskipTests "$@"
    cd ..
    
    echo ""
    echo "步骤 2/2: 安装 framework-infra 模块..."
    echo "执行: cd framework-infra && mvn clean install -DskipTests"
    echo ""
    cd framework-infra
    mvn clean install -DskipTests "$@"
    cd ..
fi

echo ""
echo "=========================================="
echo "安装完成！"
echo "=========================================="
