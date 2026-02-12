#!/bin/bash
# 清理旧的失败标志文件

rm -f .spotless_failed.flag
rm -f framework-dependencies/.spotless_failure.json
rm -f framework-infra/.spotless_failure.json
