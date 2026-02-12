#!/bin/bash
# 检查所有模块的 Spotless 格式化
# 如果任何模块失败，创建失败标志文件

MODULES=("framework-dependencies" "framework-infra")

for module in "${MODULES[@]}"; do
    echo "检查模块: $module"
    # 即使失败也继续检查其他模块
    bash scripts/ci/spotless/check_changed_files.sh "$module" || true
done

# 检查是否有失败标志文件
if [ -f .spotless_failed.flag ]; then
    echo "Spotless check failed"
    exit 1
else
    echo "Spotless check passed"
    exit 0
fi
