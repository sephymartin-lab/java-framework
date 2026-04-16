#!/bin/bash
# Spotless 增量文件校验脚本（Shell 版本）
# 检查指定模块中变更的 Java 文件是否符合格式化规范

set -e

MODULE_NAME="$1"
if [ -z "$MODULE_NAME" ]; then
    echo "用法: check_changed_files.sh <module_name>" >&2
    echo "示例: check_changed_files.sh framework-dependencies" >&2
    exit 1
fi

# 检查模块目录是否存在
if [ ! -d "$MODULE_NAME" ]; then
    echo "错误: 模块目录不存在: $MODULE_NAME" >&2
    exit 1
fi

# 获取提交信息
CURRENT_SHA="${CI_COMMIT_SHA:-}"
if [ -z "$CURRENT_SHA" ]; then
    echo "错误: CI_COMMIT_SHA 环境变量未设置" >&2
    exit 1
fi

# 获取上一个提交
PREV_SHA="${CI_PREV_COMMIT_SHA:-}"
if [ -z "$PREV_SHA" ]; then
    # 尝试获取上一个提交
    PREV_SHA=$(git rev-list --max-count=1 HEAD~1 2>/dev/null || echo "")
    if [ -z "$PREV_SHA" ]; then
        echo "警告: 无法获取上一个提交，可能是首次提交，跳过校验"
        exit 0
    fi
fi

# 获取变更的文件列表
CHANGED_FILES=$(git diff --name-only --diff-filter=ACMR "${PREV_SHA}..${CURRENT_SHA}" 2>/dev/null || echo "")

if [ -z "$CHANGED_FILES" ]; then
    echo "没有变更的文件，跳过校验"
    exit 0
fi

# 过滤出该模块的 Java 文件
JAVA_FILES=""
while IFS= read -r file; do
    if [[ "$file" == *.java ]] && [[ "$file" == ${MODULE_NAME}/* ]]; then
        if [ -z "$JAVA_FILES" ]; then
            JAVA_FILES="$file"
        else
            JAVA_FILES="$JAVA_FILES,$file"
        fi
    fi
done <<< "$CHANGED_FILES"

if [ -z "$JAVA_FILES" ]; then
    echo "模块 $MODULE_NAME 没有变更的 Java 文件，跳过校验"
    exit 0
fi

# 显示要检查的文件
echo "检查以下文件:"
echo "$JAVA_FILES" | tr ',' '\n' | sed 's/^/  - /'

# 运行 spotless check
cd "$MODULE_NAME"
EXIT_CODE=0
mvn spotless:check -DspotlessFiles="$JAVA_FILES" || EXIT_CODE=$?

# 如果失败，写入失败信息到文件
if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "❌ Spotless 校验失败！请运行 'mvn spotless:apply' 修复格式问题" >&2
    
    # 回到项目根目录
    cd ..
    
    # 写入模块目录下的失败信息文件
    FAILURE_FILE="${MODULE_NAME}/.spotless_failure.json"
    TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date +"%Y-%m-%dT%H:%M:%SZ")
    
    # 将文件列表转换为 JSON 数组格式
    JSON_FILES="["
    FIRST=true
    IFS=',' read -ra FILES <<< "$JAVA_FILES"
    for file in "${FILES[@]}"; do
        if [ "$FIRST" = true ]; then
            FIRST=false
        else
            JSON_FILES="$JSON_FILES,"
        fi
        # 转义双引号
        ESCAPED_FILE=$(echo "$file" | sed 's/"/\\"/g')
        JSON_FILES="$JSON_FILES\"$ESCAPED_FILE\""
    done
    JSON_FILES="$JSON_FILES]"
    
    cat > "$FAILURE_FILE" <<EOF
{
  "module": "$MODULE_NAME",
  "failed_files": $JSON_FILES,
  "timestamp": "$TIMESTAMP"
}
EOF
    
    # 在项目根目录创建标志文件
    FLAG_FILE=".spotless_failed.flag"
    touch "$FLAG_FILE"
    
    echo "已写入失败信息到: $FAILURE_FILE"
    echo "已创建失败标志文件: $FLAG_FILE"
fi

exit $EXIT_CODE
