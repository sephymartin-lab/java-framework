#!/usr/bin/env python3
"""
构建完成通知脚本
在 CI 构建完成时发送飞书通知
"""

import os
import sys
import time

# 添加当前目录到 Python 路径，以便导入 feishu_notify 模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from feishu_notify import send_post_message


def format_duration(seconds: float) -> str:
    """
    格式化耗时为人性化字符串
    
    Args:
        seconds: 秒数
        
    Returns:
        格式化后的字符串，如 "2分35秒"、"45秒"、"1小时12分"
    """
    if seconds < 60:
        return f"{int(seconds)}秒"
    elif seconds < 3600:
        minutes = int(seconds // 60)
        secs = int(seconds % 60)
        if secs == 0:
            return f"{minutes}分"
        return f"{minutes}分{secs}秒"
    else:
        hours = int(seconds // 3600)
        minutes = int((seconds % 3600) // 60)
        if minutes == 0:
            return f"{hours}小时"
        return f"{hours}小时{minutes}分"


def main():
    """主函数"""
    # 从环境变量获取配置
    webhook_url = os.environ.get('FEISHU_BOT_URL')
    secret = os.environ.get('FEISHU_BOT_SECRET', '')
    
    if not webhook_url:
        print("错误: FEISHU_BOT_URL 环境变量未设置", file=sys.stderr)
        sys.exit(1)
    
    # 获取构建状态
    # 注意: CI_PIPELINE_STATUS 在 Woodpecker CI 3.0+ 中已移除
    # 状态通过命令行参数传递，默认为 success
    status = sys.argv[1] if len(sys.argv) > 1 else "success"
    status_icon = "✅" if status == "success" else "❌"
    status_text = "成功" if status == "success" else "失败"
    
    # 获取仓库和构建信息
    repo_name = os.environ.get('CI_REPO_NAME', 'unknown')
    pipeline_number = os.environ.get('CI_PIPELINE_NUMBER', 'unknown')
    commit_sha = os.environ.get('CI_COMMIT_SHA', 'unknown')
    commit_branch = os.environ.get('CI_COMMIT_BRANCH', 'unknown')
    commit_message = os.environ.get('CI_COMMIT_MESSAGE', 'unknown')
    pipeline_event = os.environ.get('CI_PIPELINE_EVENT', 'unknown')
    pipeline_url = os.environ.get('CI_PIPELINE_URL', '#')
    
    # 计算耗时
    # 注意: CI_PIPELINE_FINISHED 在 Woodpecker CI 3.0+ 中已移除（执行时恒为空）
    # 使用当前时间减去 CI_PIPELINE_STARTED 来计算耗时
    duration_text = None
    pipeline_started = os.environ.get('CI_PIPELINE_STARTED')
    if pipeline_started:
        try:
            started_time = int(pipeline_started)
            current_time = int(time.time())
            duration_seconds = current_time - started_time
            duration_text = format_duration(duration_seconds)
        except (ValueError, TypeError):
            pass
    
    # 构建标题
    title = f"{status_icon} 构建完成 {repo_name} #{pipeline_number}"
    
    # 构建富文本内容
    content_lines = []
    
    # 状态行
    content_lines.append([
        {"tag": "text", "text": "【状态】", "style": ["bold"]},
        status_text
    ])
    
    # 耗时行（如果可用）
    if duration_text:
        content_lines.append([
            {"tag": "text", "text": "【耗时】", "style": ["bold"]},
            duration_text
        ])
    
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
