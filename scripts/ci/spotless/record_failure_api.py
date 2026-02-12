#!/usr/bin/env python3
"""
通过 HTTP API 记录 Spotless 格式化失败信息到 DevOps 平台
"""

import json
import os
import sys
import urllib.request
import urllib.error
import urllib.parse
from datetime import datetime


def record_failure_via_api(commit_sha, commit_branch, commit_message, author_name, author_email,
                           failed_files, repo_name=None, repo_owner=None, repo=None,
                           pipeline_number=None, pipeline_url=None):
    """
    通过 HTTP API 记录格式化失败信息
    
    Args:
        commit_sha: 提交 SHA
        commit_branch: 分支名称
        commit_message: 提交信息
        author_name: 作者名称
        author_email: 作者邮箱
        failed_files: 失败文件列表（列表或 JSON 字符串）
        repo_name: 仓库名称（如：my-repo）
        repo_owner: 仓库所有者（如：john-doe）
        repo: 仓库全名（如：john-doe/my-repo），如果提供则优先使用
        pipeline_number: 流水线编号
        pipeline_url: 流水线 URL
    
    Returns:
        bool: 是否记录成功
    """
    api_url = os.environ.get('DEVOPS_API_URL')
    api_token = os.environ.get('DEVOPS_API_TOKEN')
    
    if not api_url:
        print("警告: DEVOPS_API_URL 环境变量未设置，跳过 API 记录", file=sys.stderr)
        return False
    
    if not api_token:
        print("警告: DEVOPS_API_TOKEN 环境变量未设置，跳过 API 记录", file=sys.stderr)
        return False
    
    # 准备请求数据
    if isinstance(failed_files, list):
        failed_files_list = failed_files
    else:
        try:
            failed_files_list = json.loads(failed_files)
        except (json.JSONDecodeError, TypeError):
            failed_files_list = []
    
    # 确定仓库信息：优先使用 repo（完整名称），否则组合 repo_owner 和 repo_name
    if repo:
        repo_full_name = repo
    elif repo_owner and repo_name:
        repo_full_name = f"{repo_owner}/{repo_name}"
    elif repo_name:
        repo_full_name = repo_name
    else:
        repo_full_name = None
    
    data = {
        'commit_sha': commit_sha,
        'commit_branch': commit_branch,
        'commit_message': commit_message,
        'author_name': author_name,
        'author_email': author_email,
        'failed_files': failed_files_list,
        'pipeline_number': pipeline_number,
        'pipeline_url': pipeline_url,
        'timestamp': datetime.now().isoformat()
    }
    
    # 添加仓库信息（如果提供）
    if repo_full_name:
        data['repo'] = repo_full_name
    if repo_name:
        data['repo_name'] = repo_name
    if repo_owner:
        data['repo_owner'] = repo_owner
    
    try:
        # 构建请求
        url = f"{api_url.rstrip('/')}/api/v1/spotless/failures"
        req_data = json.dumps(data).encode('utf-8')
        
        req = urllib.request.Request(
            url,
            data=req_data,
            headers={
                'Content-Type': 'application/json',
                'Authorization': f'Bearer {api_token}'
            },
            method='POST'
        )
        
        # 发送请求
        with urllib.request.urlopen(req, timeout=10) as response:
            response_data = json.loads(response.read().decode('utf-8'))
            
            if response.status == 200 or response.status == 201:
                print(f"已通过 API 记录格式化失败信息: {commit_sha[:8]} - {author_name}")
                return True
            else:
                print(f"警告: API 返回非成功状态码: {response.status}", file=sys.stderr)
                return False
                
    except urllib.error.HTTPError as e:
        error_body = ""
        try:
            error_body = e.read().decode('utf-8')
        except:
            pass
        
        print(f"警告: HTTP API 请求失败: {e.code} {e.reason}", file=sys.stderr)
        if error_body:
            print(f"错误详情: {error_body}", file=sys.stderr)
        return False
        
    except urllib.error.URLError as e:
        print(f"警告: 无法连接到 DevOps API: {e.reason}", file=sys.stderr)
        return False
        
    except Exception as e:
        print(f"警告: API 记录失败: {type(e).__name__}: {e}", file=sys.stderr)
        return False


def main():
    """主函数 - 从环境变量读取信息并记录"""
    commit_sha = os.environ.get('CI_COMMIT_SHA')
    commit_branch = os.environ.get('CI_COMMIT_BRANCH')
    commit_message = os.environ.get('CI_COMMIT_MESSAGE', '')
    author_name = os.environ.get('CI_COMMIT_AUTHOR', 'unknown')
    author_email = os.environ.get('CI_COMMIT_AUTHOR_EMAIL', 'unknown@example.com')
    repo_name = os.environ.get('CI_REPO_NAME')
    repo_owner = os.environ.get('CI_REPO_OWNER')
    repo = os.environ.get('CI_REPO')  # 完整仓库名称，如 owner/repo
    pipeline_number = os.environ.get('CI_PIPELINE_NUMBER')
    pipeline_url = os.environ.get('CI_PIPELINE_URL')
    
    # 从命令行参数或环境变量获取失败文件列表
    if len(sys.argv) > 1:
        # 从命令行参数读取 JSON 格式的文件列表
        try:
            failed_files = json.loads(sys.argv[1])
        except json.JSONDecodeError:
            # 如果不是 JSON，当作逗号分隔的字符串处理
            failed_files = [f.strip() for f in sys.argv[1].split(',') if f.strip()]
    else:
        # 从环境变量读取
        failed_files_str = os.environ.get('SPOTLESS_FAILED_FILES', '[]')
        try:
            failed_files = json.loads(failed_files_str)
        except json.JSONDecodeError:
            failed_files = []
    
    if not commit_sha:
        print("错误: CI_COMMIT_SHA 环境变量未设置", file=sys.stderr)
        sys.exit(1)
    
    if not failed_files:
        print("警告: 没有失败文件信息，跳过记录", file=sys.stderr)
        sys.exit(0)
    
    success = record_failure_via_api(
        commit_sha=commit_sha,
        commit_branch=commit_branch,
        commit_message=commit_message,
        author_name=author_name,
        author_email=author_email,
        failed_files=failed_files,
        repo_name=repo_name,
        repo_owner=repo_owner,
        repo=repo,
        pipeline_number=pipeline_number,
        pipeline_url=pipeline_url
    )
    
    # 即使记录失败也不退出，不影响 CI 流程
    sys.exit(0)


if __name__ == '__main__':
    main()
