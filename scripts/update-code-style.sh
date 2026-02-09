#!/bin/bash
# 更新代码格式化配置
# 从 devops-toolkit 仓库同步 code-style/java 目录到当前项目的 style 目录

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
DEVOPS_TOOLKIT_REPO="git@github.com:sephymartin/devops-toolkit.git"
DEVOPS_TOOLKIT_BRANCH="main"
SOURCE_DIR="code-style/java"
TARGET_DIR="style"
REMOTE_NAME="devops-toolkit"

echo -e "${GREEN}开始更新代码格式化配置...${NC}"

# 检查是否在 git 仓库中
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${RED}错误: 当前目录不是 git 仓库${NC}"
    exit 1
fi

# 检查工作区是否干净
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}警告: 工作区有未提交的更改，建议先提交或暂存${NC}"
    read -p "是否继续? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 确保 remote 存在
if ! git remote | grep -q "^${REMOTE_NAME}$"; then
    echo -e "${GREEN}添加 devops-toolkit 作为 remote...${NC}"
    git remote add ${REMOTE_NAME} ${DEVOPS_TOOLKIT_REPO}
fi

# 获取最新的 devops-toolkit
echo -e "${GREEN}获取最新的 devops-toolkit 代码...${NC}"
git fetch ${REMOTE_NAME} ${DEVOPS_TOOLKIT_BRANCH}

# 获取最新的 commit hash
LATEST_COMMIT=$(git rev-parse ${REMOTE_NAME}/${DEVOPS_TOOLKIT_BRANCH})
CURRENT_COMMIT=$(git log -1 --format="%B" | grep -oP 'devops-toolkit@\K[a-f0-9]+' | head -1 || echo "")

if [ "$CURRENT_COMMIT" = "$LATEST_COMMIT" ]; then
    echo -e "${GREEN}代码格式化配置已经是最新的 (${LATEST_COMMIT:0:8})${NC}"
    exit 0
fi

echo -e "${GREEN}发现新版本: ${LATEST_COMMIT:0:8}${NC}"
if [ -n "$CURRENT_COMMIT" ]; then
    echo -e "当前版本: ${CURRENT_COMMIT:0:8}"
fi

# 创建临时目录
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# 检出 devops-toolkit 的指定目录
echo -e "${GREEN}检出 devops-toolkit 的代码格式化配置...${NC}"
git archive --remote=${REMOTE_NAME} ${DEVOPS_TOOLKIT_BRANCH} ${SOURCE_DIR}/ | tar -x -C ${TEMP_DIR}

# 检查文件是否存在
if [ ! -d "${TEMP_DIR}/${SOURCE_DIR}" ]; then
    echo -e "${RED}错误: 无法从 devops-toolkit 获取配置文件${NC}"
    exit 1
fi

# 备份当前配置（如果存在）
if [ -d "${TARGET_DIR}" ]; then
    echo -e "${GREEN}备份当前配置...${NC}"
    cp -r ${TARGET_DIR} ${TARGET_DIR}.backup.$(date +%Y%m%d_%H%M%S)
fi

# 更新配置文件
echo -e "${GREEN}更新配置文件...${NC}"
mkdir -p ${TARGET_DIR}
cp -r ${TEMP_DIR}/${SOURCE_DIR}/* ${TARGET_DIR}/

# 显示变更
echo -e "${GREEN}文件变更:${NC}"
git status --short ${TARGET_DIR}/

# 提交更改
echo -e "${GREEN}提交更改...${NC}"
git add ${TARGET_DIR}/
git commit -m "chore: update code style config from devops-toolkit

- Update from devops-toolkit@${LATEST_COMMIT}
- Source: ${DEVOPS_TOOLKIT_REPO}
- Branch: ${DEVOPS_TOOLKIT_BRANCH}" || {
    echo -e "${YELLOW}没有变更需要提交${NC}"
    exit 0
}

echo -e "${GREEN}✓ 代码格式化配置已更新到 ${LATEST_COMMIT:0:8}${NC}"
