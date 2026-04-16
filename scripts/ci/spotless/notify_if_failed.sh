#!/bin/bash
# Spotless 失败通知脚本
# 检查是否有失败标志文件，如果有则发送通知并记录到 DevOps 平台

set -e

# 检查是否有失败标志文件
if [ -f .spotless_failed.flag ]; then
    echo "检测到 Spotless 校验失败，发送通知..."
    python3 scripts/ci/spotless/notify_spotless_failure.py
    exit 1
else
    echo "Spotless check passed，无需发送通知"
    exit 0
fi
