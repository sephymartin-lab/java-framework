#!/usr/bin/env python3
"""
构建开始通知脚本
在 CI 构建开始时发送飞书通知
"""

import os
import sys

# 添加当前目录到 Python 路径，以便导入 feishu_notify 模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from feishu_notify import send_post_message


def main():
    """主函数"""
    # 从环境变量获取配置
    webhook_url = os.environ.get('FEISHU_BOT_URL')
    secret = os.environ.get('FEISHU_BOT_SECRET', '')
    
    if not webhook_url:
        print("错误: FEISHU_BOT_URL 环境变量未设置", file=sys.stderr)
        sys.exit(1)
    
    # 获取仓库和构建信息
    repo_name = os.environ.get('CI_REPO_NAME', 'unknown')
    pipeline_number = os.environ.get('CI_PIPELINE_NUMBER', 'unknown')
    commit_sha = os.environ.get('CI_COMMIT_SHA', 'unknown')
    commit_branch = os.environ.get('CI_COMMIT_BRANCH', 'unknown')
    commit_message = os.environ.get('CI_COMMIT_MESSAGE', 'unknown')
    pipeline_event = os.environ.get('CI_PIPELINE_EVENT', 'unknown')
    pipeline_url = os.environ.get('CI_PIPELINE_URL', '#')
    
    # 构建标题
    title = f"🏗️ 开始构建 {repo_name} #{pipeline_number}"
    
    # 构建富文本内容
    content_lines = []
    
    # 分支行
    content_lines.append([
        {"tag": "text", "text": "【分支】", "style": ["bold"]},
        commit_branch
    ])
    
    # 提交 SHA（显示前8位）
    commit_short = commit_sha[:8] if len(commit_sha) >= 8 else commit_sha
    content_lines.append([
        {"tag": "text", "text": "【提交】", "style": ["bold"]},
        commit_short
    ])
    
    # 提交信息（只显示第一行，避免过长）
    commit_msg_first_line = commit_message.split('\n')[0] if commit_message else 'unknown'
    content_lines.append([
        {"tag": "text", "text": "【提交信息】", "style": ["bold"]},
        commit_msg_first_line
    ])
    
    # 事件行
    content_lines.append([
        {"tag": "text", "text": "【事件】", "style": ["bold"]},
        pipeline_event
    ])
    
    # 空行分隔
    content_lines.append([])
    
    # 查看构建详情链接
    content_lines.append([
        "📦 ",
        {"tag": "a", "text": "查看构建详情", "href": pipeline_url}
    ])
    
    # 发送消息
    success = send_post_message(webhook_url, secret, title, content_lines)
    
    if not success:
        sys.exit(1)


if __name__ == "__main__":
    main()
