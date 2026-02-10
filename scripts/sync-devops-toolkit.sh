#!/bin/bash
# 同步 devops-toolkit 配置
# 从 devops-toolkit 仓库同步配置目录到当前项目
# - code-style/java -> style
# - scripts/ci -> scripts/ci

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
DEVOPS_TOOLKIT_REPO="git@github.com:sephymartin/devops-toolkit.git"
DEVOPS_TOOLKIT_BRANCH="main"
REMOTE_NAME="devops-toolkit"
# 保留的备份数量（保留最近 N 个备份）
KEEP_BACKUPS=3

# 定义同步配置：源目录 -> 目标目录
declare -a SYNC_CONFIGS=(
    "code-style/java:style"
    "scripts/ci:scripts/ci"
)

echo -e "${GREEN}开始同步 devops-toolkit 配置...${NC}"

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
# 查找最近的包含 devops-toolkit@ 的 commit message
CURRENT_COMMIT=$(git log --grep="devops-toolkit@" --format="%B" -1 | sed -nE 's/.*devops-toolkit@([a-f0-9]+).*/\1/p' | head -1 || echo "")

# 清理旧备份函数（提前定义，以便在检测到已是最新版本时也能清理）
cleanup_old_backups() {
    local target_dir=$1
    local target_base=$(basename "$target_dir")
    local target_parent=$(dirname "$target_dir")
    
    # 查找所有备份目录并按时间排序（最新的在前）
    local backups=$(find "$target_parent" -maxdepth 1 -type d -name "${target_base}.backup.*" 2>/dev/null | sort -r)
    
    if [ -z "$backups" ]; then
        return 0
    fi
    
    # 计算需要删除的备份数量
    local backup_count=$(echo "$backups" | wc -l | tr -d ' ')
    local to_delete=$((backup_count - KEEP_BACKUPS))
    
    if [ $to_delete -gt 0 ]; then
        echo -e "${GREEN}清理旧备份（保留最近 ${KEEP_BACKUPS} 个）...${NC}"
        echo "$backups" | tail -n $to_delete | while read -r backup; do
            if [ -d "$backup" ]; then
                echo -e "  删除: $(basename "$backup")"
                rm -rf "$backup"
            fi
        done
    fi
}

if [ "$CURRENT_COMMIT" = "$LATEST_COMMIT" ]; then
    echo -e "${GREEN}devops-toolkit 配置已经是最新的 (${LATEST_COMMIT:0:8})${NC}"
    # 即使已是最新版本，也清理旧备份
    for config in "${SYNC_CONFIGS[@]}"; do
        IFS=':' read -r source_dir target_dir <<< "$config"
        cleanup_old_backups "$target_dir"
    done
    exit 0
fi

echo -e "${GREEN}发现新版本: ${LATEST_COMMIT:0:8}${NC}"
if [ -n "$CURRENT_COMMIT" ]; then
    echo -e "当前版本: ${CURRENT_COMMIT:0:8}"
fi

# 创建临时目录
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# 从远程仓库获取指定目录的文件
sync_directory() {
    local source_dir=$1
    local target_dir=$2
    local failed=0
    
    echo -e "${GREEN}同步 ${source_dir} -> ${target_dir}...${NC}"
    
    # 获取文件列表
    local files=$(git ls-tree -r --name-only ${REMOTE_NAME}/${DEVOPS_TOOLKIT_BRANCH} ${source_dir}/ 2>/dev/null || echo "")
    
    if [ -z "$files" ]; then
        echo -e "${YELLOW}警告: ${source_dir} 目录不存在或为空${NC}"
        return 1
    fi
    
    # 逐个获取文件
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            local dest_file="${TEMP_DIR}/${file}"
            local dest_dir="$(dirname "$dest_file")"
            mkdir -p "$dest_dir" || {
                echo -e "${RED}错误: 无法创建目录 ${dest_dir}${NC}"
                failed=1
                continue
            }
            if ! git show ${REMOTE_NAME}/${DEVOPS_TOOLKIT_BRANCH}:${file} > "$dest_file" 2>/dev/null; then
                echo -e "${RED}错误: 无法获取文件 ${file}${NC}"
                failed=1
            fi
        fi
    done <<< "$files"
    
    if [ $failed -eq 1 ]; then
        return 1
    fi
    
    # 复制到目标目录
    if [ -d "${TEMP_DIR}/${source_dir}" ]; then
        # 备份当前配置（如果存在）
        if [ -d "${target_dir}" ]; then
            echo -e "${GREEN}备份当前配置 ${target_dir}...${NC}"
            cp -r ${target_dir} ${target_dir}.backup.$(date +%Y%m%d_%H%M%S)
        fi
        
        mkdir -p "$(dirname "${target_dir}")"
        cp -r ${TEMP_DIR}/${source_dir}/* ${target_dir}/
        return 0
    else
        echo -e "${RED}错误: 无法从 devops-toolkit 获取 ${source_dir}${NC}"
        return 1
    fi
}


# 同步所有配置目录
SYNC_FAILED=0
for config in "${SYNC_CONFIGS[@]}"; do
    IFS=':' read -r source_dir target_dir <<< "$config"
    if ! sync_directory "$source_dir" "$target_dir"; then
        SYNC_FAILED=1
    fi
done

if [ $SYNC_FAILED -eq 1 ]; then
    echo -e "${RED}错误: 部分目录同步失败${NC}"
    exit 1
fi

# 清理旧备份
for config in "${SYNC_CONFIGS[@]}"; do
    IFS=':' read -r source_dir target_dir <<< "$config"
    cleanup_old_backups "$target_dir"
done

# 显示变更
echo -e "${GREEN}文件变更:${NC}"
CHANGED_FILES=()
for config in "${SYNC_CONFIGS[@]}"; do
    IFS=':' read -r source_dir target_dir <<< "$config"
    if [ -d "${target_dir}" ]; then
        git status --short ${target_dir}/ || true
        CHANGED_FILES+=("${target_dir}/")
    fi
done

# 提交更改
if [ ${#CHANGED_FILES[@]} -gt 0 ]; then
    echo -e "${GREEN}提交更改...${NC}"
    git add "${CHANGED_FILES[@]}"
    git commit -m "chore: sync configs from devops-toolkit

- Sync code-style from devops-toolkit@${LATEST_COMMIT}
- Sync CI scripts from devops-toolkit@${LATEST_COMMIT}
- Source: ${DEVOPS_TOOLKIT_REPO}
- Branch: ${DEVOPS_TOOLKIT_BRANCH}" || {
        echo -e "${YELLOW}没有变更需要提交${NC}"
        exit 0
    }
fi

echo -e "${GREEN}✓ devops-toolkit 配置已同步到 ${LATEST_COMMIT:0:8}${NC}"
