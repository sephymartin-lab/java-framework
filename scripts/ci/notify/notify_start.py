#!/usr/bin/env python3
"""
构建开始通知脚本
在 CI 构建开始时发送飞书通知
"""

import os
import sys

# 添加当前目录到 Python 路径，以便导入 feishu_notify 模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from feishu_notify import send_message


def main():
    """主函数"""
    # 从环境变量获取配置
    webhook_url = os.environ.get('FEISHU_BOT_URL')
    secret = os.environ.get('FEISHU_BOT_SECRET', '')
    
    if not webhook_url:
        print("错误: FEISHU_BOT_URL 环境变量未设置", file=sys.stderr)
        sys.exit(1)
    
    # 构建消息
    message = f"""🏗️ 开始构建 {os.environ.get('CI_REPO_NAME', 'unknown')} #{os.environ.get('CI_PIPELINE_NUMBER', 'unknown')}

- 提交: {os.environ.get('CI_COMMIT_SHA', 'unknown')}
- 分支: {os.environ.get('CI_COMMIT_BRANCH', 'unknown')}
- 提交信息: {os.environ.get('CI_COMMIT_MESSAGE', 'unknown')}
- 事件: {os.environ.get('CI_PIPELINE_EVENT', 'unknown')}

📦️ [查看构建详情]({os.environ.get('CI_PIPELINE_URL', '#')})"""
    
    # 发送消息
    success = send_message(webhook_url, secret, message)
    
    if not success:
        sys.exit(1)


if __name__ == "__main__":
    main()
