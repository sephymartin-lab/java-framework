#!/usr/bin/env python3
"""
Spotless 格式化失败通知脚本
发送飞书通知并记录到数据库
"""

import json
import os
import sys
import subprocess
from pathlib import Path

# 添加当前目录到 Python 路径
script_dir = os.path.dirname(os.path.abspath(__file__))
ci_dir = os.path.dirname(os.path.dirname(script_dir))
sys.path.insert(0, ci_dir)

from notify.feishu_notify import send_post_message


def parse_failed_files_from_output(output_text):
    """从 spotless 输出中解析失败的文件列表"""
    failed_files = []
    lines = output_text.split('\n')
    
    for line in lines:
        line = line.strip()
        # Spotless 输出通常包含文件路径
        if '.java' in line and ('error' in line.lower() or 'violation' in line.lower()):
            # 尝试提取文件路径
            # 格式可能是: "path/to/file.java:1:1: error: ..."
            parts = line.split(':')
            if len(parts) >= 2:
                potential_file = parts[0].strip()
                if potential_file.endswith('.java') and os.path.exists(potential_file):
                    failed_files.append(potential_file)
    
    return failed_files


def load_failure_info():
    """从文件加载失败信息，合并多个模块的失败文件列表"""
    failed_files = []
    modules = ['framework-dependencies', 'framework-infra']
    
    # 查找项目根目录
    root_dir = Path.cwd()
    # 通过查找包含 .git 或 .woodpecker.yml 的目录
    current = Path.cwd()
    while current != current.parent:
        if (current / '.git').exists() or (current / '.woodpecker.yml').exists():
            root_dir = current
            break
        current = current.parent
    
    # 检查标志文件是否存在
    flag_file = root_dir / '.spotless_failed.flag'
    if not flag_file.exists():
        return None
    
    # 读取每个模块的失败信息
    for module in modules:
        module_failure_file = root_dir / module / '.spotless_failure.json'
        if module_failure_file.exists():
            try:
                with open(module_failure_file, 'r', encoding='utf-8') as f:
                    module_info = json.load(f)
                    if 'failed_files' in module_info:
                        failed_files.extend(module_info['failed_files'])
            except Exception as e:
                print(f"警告: 读取模块 {module} 的失败信息失败: {e}", file=sys.stderr)
    
    # 去重
    failed_files = list(dict.fromkeys(failed_files))
    
    return {'failed_files': failed_files} if failed_files else None


def get_failed_files_from_git_diff():
    """从 git diff 获取变更的 Java 文件（作为失败文件的备选）"""
    prev_sha = os.environ.get('CI_PREV_COMMIT_SHA')
    current_sha = os.environ.get('CI_COMMIT_SHA')
    
    if not prev_sha or not current_sha:
        return []
    
    try:
        result = subprocess.run(
            ['git', 'diff', '--name-only', '--diff-filter=ACMR', f'{prev_sha}..{current_sha}'],
            capture_output=True,
            text=True,
            check=True
        )
        java_files = [f.strip() for f in result.stdout.strip().split('\n') 
                     if f.strip().endswith('.java')]
        return java_files
    except Exception:
        return []


def main():
    """主函数"""
    # 获取飞书配置
    webhook_url = os.environ.get('FEISHU_BOT_URL')
    secret = os.environ.get('FEISHU_BOT_SECRET', '')
    
    if not webhook_url:
        print("警告: FEISHU_BOT_URL 环境变量未设置，跳过飞书通知", file=sys.stderr)
    
    # 获取 CI 环境变量
    repo_name = os.environ.get('CI_REPO_NAME', 'unknown')
    pipeline_number = os.environ.get('CI_PIPELINE_NUMBER', 'unknown')
    commit_sha = os.environ.get('CI_COMMIT_SHA', 'unknown')
    commit_branch = os.environ.get('CI_COMMIT_BRANCH', 'unknown')
    commit_message = os.environ.get('CI_COMMIT_MESSAGE', 'unknown')
    commit_author = os.environ.get('CI_COMMIT_AUTHOR', 'unknown')
    commit_author_email = os.environ.get('CI_COMMIT_AUTHOR_EMAIL', 'unknown@example.com')
    pipeline_url = os.environ.get('CI_PIPELINE_URL', '#')
    
    # 获取失败文件列表
    # 优先级：文件 > 环境变量 > git diff
    failed_files = []
    
    # 首先尝试从文件读取
    failure_info = load_failure_info()
    if failure_info and failure_info.get('failed_files'):
        failed_files = failure_info['failed_files']
        print(f"从文件读取到 {len(failed_files)} 个失败文件")
    else:
        # 尝试从环境变量读取
        failed_files_str = os.environ.get('SPOTLESS_FAILED_FILES')
        if failed_files_str:
            try:
                failed_files = json.loads(failed_files_str)
            except json.JSONDecodeError:
                failed_files = [f.strip() for f in failed_files_str.split(',') if f.strip()]
        
        # 如果还是没有，从 git diff 获取变更的 Java 文件
        if not failed_files:
            failed_files = get_failed_files_from_git_diff()
            print(f"从 git diff 获取到 {len(failed_files)} 个变更的 Java 文件")
    
    if not failed_files:
        print("警告: 无法获取失败文件列表", file=sys.stderr)
        failed_files = []
    
    # 构建飞书消息
    title = f"❌ 代码格式化校验失败 {repo_name} #{pipeline_number}"
    
    content_lines = []
    
    # 开发者信息
    content_lines.append([
        {"tag": "text", "text": "【开发者】", "style": ["bold"]},
        f"{commit_author} ({commit_author_email})"
    ])
    
    # 分支信息
    content_lines.append([
        {"tag": "text", "text": "【分支】", "style": ["bold"]},
        commit_branch
    ])
    
    # 提交信息
    commit_short = commit_sha[:8] if len(commit_sha) >= 8 else commit_sha
    content_lines.append([
        {"tag": "text", "text": "【提交】", "style": ["bold"]},
        commit_short
    ])
    
    # 提交信息（只显示第一行）
    commit_msg_first_line = commit_message.split('\n')[0] if commit_message else 'unknown'
    content_lines.append([
        {"tag": "text", "text": "【提交信息】", "style": ["bold"]},
        commit_msg_first_line
    ])
    
    # 失败文件列表
    if failed_files:
        content_lines.append([])  # 空行
        content_lines.append([
            {"tag": "text", "text": "【失败文件】", "style": ["bold"]}
        ])
        # 最多显示前10个文件
        display_files = failed_files[:10]
        for file_path in display_files:
            content_lines.append([f"  • {file_path}"])
        if len(failed_files) > 10:
            content_lines.append([
                f"  ... 还有 {len(failed_files) - 10} 个文件"
            ])
    
    # 空行分隔
    content_lines.append([])
    
    # 修复提示
    content_lines.append([
        {"tag": "text", "text": "💡 修复方法：", "style": ["bold"]},
        "运行 'mvn spotless:apply' 自动修复格式问题"
    ])
    
    # 构建链接
    content_lines.append([])
    content_lines.append([
        "📦 ",
        {"tag": "a", "text": "查看构建详情", "href": pipeline_url}
    ])
    
    # 发送飞书通知
    if webhook_url:
        success = send_post_message(webhook_url, secret, title, content_lines)
        if not success:
            print("警告: 飞书通知发送失败", file=sys.stderr)
    
    # 记录到 DevOps 平台（通过 HTTP API）
    try:
        import json
        failed_files_json = json.dumps(failed_files, ensure_ascii=False)
        env = os.environ.copy()
        env['CI_COMMIT_SHA'] = commit_sha
        env['CI_COMMIT_BRANCH'] = commit_branch
        env['CI_COMMIT_MESSAGE'] = commit_message
        env['CI_COMMIT_AUTHOR'] = commit_author
        env['CI_COMMIT_AUTHOR_EMAIL'] = commit_author_email
        env['CI_PIPELINE_NUMBER'] = pipeline_number or ''
        env['CI_PIPELINE_URL'] = pipeline_url or ''
        env['SPOTLESS_FAILED_FILES'] = failed_files_json
        
        # 添加仓库信息到环境变量
        env['CI_REPO_NAME'] = repo_name
        env['CI_REPO_OWNER'] = os.environ.get('CI_REPO_OWNER', '')
        env['CI_REPO'] = os.environ.get('CI_REPO', '')
        
        # 优先使用 API 方式
        api_url = os.environ.get('DEVOPS_API_URL')
        if api_url:
            # 使用 HTTP API 方式
            subprocess.run(
                [sys.executable, os.path.join(script_dir, 'record_failure_api.py')],
                env=env,
                check=False
            )
        else:
            # 如果没有配置 API，记录警告（数据库方式已废弃，建议使用 API 方式）
            print("警告: 未配置 DEVOPS_API_URL，跳过记录到 DevOps 平台", file=sys.stderr)
            print("提示: 请配置 DEVOPS_API_URL 和 DEVOPS_API_TOKEN 以启用统计功能", file=sys.stderr)
    except Exception as e:
        print(f"警告: 记录失败信息失败: {e}", file=sys.stderr)
        # 不影响主流程


if __name__ == '__main__':
    main()
