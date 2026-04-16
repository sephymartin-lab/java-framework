#!/usr/bin/env python3
"""
飞书 Webhook 通知模块
提供签名生成和消息发送功能
"""

import base64
import hmac
import json
import os
import time
import urllib.request
from hashlib import sha256


def generate_signature(secret: str) -> tuple[str, str]:
    """
    生成飞书 webhook v2 签名
    
    Args:
        secret: 签名密钥
        
    Returns:
        (timestamp, sign) 元组，timestamp 为字符串格式的时间戳，sign 为 Base64 编码的签名
    """
    timestamp = str(round(time.time()))
    key = f'{timestamp}\n{secret}'
    key_enc = key.encode('utf-8')
    msg = ""  # 待签名字符串为空
    msg_enc = msg.encode('utf-8')
    hmac_code = hmac.new(key_enc, msg_enc, digestmod=sha256).digest()
    sign = base64.b64encode(hmac_code).decode('utf-8')
    return timestamp, sign


def send_message(webhook_url: str, secret: str, message: str) -> bool:
    """
    发送文本消息到飞书 webhook
    
    Args:
        webhook_url: webhook URL
        secret: 签名密钥
        message: 要发送的消息文本
        
    Returns:
        是否发送成功
    """
    try:
        # 生成签名
        timestamp, sign = generate_signature(secret)
        
        # 构建请求体
        data = {
            "timestamp": timestamp,
            "sign": sign,
            "msg_type": "text",
            "content": {
                "text": message
            }
        }
        
        # 发送请求
        req = urllib.request.Request(
            webhook_url,
            data=json.dumps(data).encode('utf-8'),
            headers={'Content-Type': 'application/json'}
        )
        
        response = urllib.request.urlopen(req)
        response_data = json.loads(response.read().decode('utf-8'))
        
        if response_data.get('code') == 0:
            return True
        else:
            print(f"错误: {response_data.get('msg')}", file=os.sys.stderr)
            return False
            
    except urllib.error.HTTPError as e:
        print(f"HTTP 错误: {e.code} {e.reason}", file=os.sys.stderr)
        try:
            error_body = e.read().decode('utf-8')
            print(f"错误响应: {error_body}", file=os.sys.stderr)
        except:
            pass
        return False
    except Exception as e:
        print(f"发生错误: {type(e).__name__}: {e}", file=os.sys.stderr)
        return False


def send_post_message(webhook_url: str, secret: str, title: str, content_lines: list) -> bool:
    """
    发送富文本 post 消息到飞书 webhook
    
    Args:
        webhook_url: webhook URL
        secret: 签名密钥
        title: 消息标题
        content_lines: 内容行列表，每个元素是一个列表，包含该行的文本元素
                      每个文本元素可以是字符串（普通文本）或字典（带样式的文本）
                      例如: [
                          [{"tag": "text", "text": "【状态】", "style": ["bold"]}, "成功"],
                          [{"tag": "text", "text": "【分支】", "style": ["bold"]}, "main"]
                      ]
        
    Returns:
        是否发送成功
    """
    try:
        # 生成签名
        timestamp, sign = generate_signature(secret)
        
        # 处理内容行，将字符串转换为文本元素
        processed_content = []
        for line in content_lines:
            processed_line = []
            for item in line:
                if isinstance(item, str):
                    # 字符串直接作为普通文本
                    processed_line.append({"tag": "text", "text": item})
                elif isinstance(item, dict):
                    # 字典直接使用（应包含 tag、text、style 等字段）
                    processed_line.append(item)
            processed_content.append(processed_line)
        
        # 构建请求体
        data = {
            "timestamp": timestamp,
            "sign": sign,
            "msg_type": "post",
            "content": {
                "post": {
                    "zh_cn": {
                        "title": title,
                        "content": processed_content
                    }
                }
            }
        }
        
        # 发送请求
        req = urllib.request.Request(
            webhook_url,
            data=json.dumps(data).encode('utf-8'),
            headers={'Content-Type': 'application/json'}
        )
        
        response = urllib.request.urlopen(req)
        response_data = json.loads(response.read().decode('utf-8'))
        
        if response_data.get('code') == 0:
            return True
        else:
            print(f"错误: {response_data.get('msg')}", file=os.sys.stderr)
            return False
            
    except urllib.error.HTTPError as e:
        print(f"HTTP 错误: {e.code} {e.reason}", file=os.sys.stderr)
        try:
            error_body = e.read().decode('utf-8')
            print(f"错误响应: {error_body}", file=os.sys.stderr)
        except:
            pass
        return False
    except Exception as e:
        print(f"发生错误: {type(e).__name__}: {e}", file=os.sys.stderr)
        return False
