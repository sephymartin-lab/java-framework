/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.consts;

/**
 * HTTP请求头常量定义
 * <p>
 * 定义了系统中使用的各种HTTP头部字段常量，包括网络代理、认证授权、链路追踪、请求信息和业务标识等类型
 * </p>
 */
public abstract class HttpHeaderConstants {

    // ==================== 网络代理相关 ====================

    /**
     * X-Forwarded-For头，用于获取经过代理服务器的客户端真实IP地址
     * <p>
     * 当请求经过多层代理时，该头部包含客户端和各级代理的IP地址链
     * </p>
     */
    public static final String HEADER_X_FORWARDED_FOR = "x-forwarded-for";

    /**
     * X-Real-IP头，用于获取客户端真实IP地址
     * <p>
     * 通常由反向代理（如Nginx）设置，包含客户端的真实IP地址
     * </p>
     */
    public static final String HEADER_X_REAL_IP = "x-real-ip";

    /**
     * Host头，标识请求的目标主机名和端口
     * <p>
     * HTTP/1.1协议必需的头部，用于虚拟主机路由
     * </p>
     */
    public static final String HEADER_HOST = "host";

    /**
     * X-Original-Scheme头，保存原始请求的协议方案（http/https）
     * <p>
     * 用于在代理环境中保持原始请求的协议信息，便于构建完整的原始URL
     * </p>
     */
    public static final String HEADER_X_ORIGINAL_SCHEME = "x-original-scheme";

    /**
     * X-Original-Port头，保存原始请求的端口号
     * <p>
     * 用于在代理环境中保持原始请求的端口信息，便于构建完整的原始URL
     * </p>
     */
    public static final String HEADER_X_ORIGINAL_PORT = "x-original-port";

    /**
     * X-Original-URI头，保存原始请求的URI路径
     * <p>
     * 用于在代理环境中保持原始请求的URI信息，便于构建完整的原始URL
     * </p>
     */
    public static final String HEADER_X_ORIGINAL_URI = "x-original-uri";

    // ==================== 认证授权相关 ====================

    /**
     * 认证用户ID头，传递已认证用户的唯一标识
     * <p>
     * 用于微服务间传递用户身份信息，避免重复认证
     * </p>
     */
    public static final String HEADER_AUTH_USERID = "auth-userid";

    /**
     * 认证用户名头，传递已认证用户的用户名
     * <p>
     * 用于微服务间传递用户身份信息，便于日志记录和审计
     * </p>
     */
    public static final String HEADER_AUTH_USERNAME = "auth-username";

    /**
     * OAuth客户端ID头，标识OAuth认证的客户端应用
     * <p>
     * 用于OAuth2.0认证流程中标识发起请求的客户端应用
     * </p>
     */
    public static final String HEADER_OAUTH_CLIENTID = "oauth-clientid";

    // ==================== 链路追踪相关 ====================

    /**
     * 链路追踪ID头，用于分布式系统的请求链路追踪
     * <p>
     * 在微服务调用链中传递，用于关联同一个请求在不同服务中的日志和监控数据
     * </p>
     */
    public static final String HEADER_TRACE_ID = "trace-id";

    /**
     * 链路追踪跳过标识头，用于标识是否跳过链路追踪
     * <p>
     * 当设置此头部时，可以跳过某些链路追踪逻辑，用于特殊场景下的性能优化
     * </p>
     */
    public static final String HEADER_TRACE_SKIP = "trace-skip";

    // ==================== 请求信息相关 ====================

    /**
     * User-Agent头，标识发起请求的客户端应用信息
     * <p>
     * 包含浏览器、操作系统等客户端环境信息，用于统计分析和兼容性处理
     * </p>
     */
    public static final String HEADER_USER_AGENT = "user-agent";

    // ==================== 业务标识相关 ====================

    /**
     * 业务类型头，标识请求所属的业务类型或业务模块
     * <p>
     * 用于业务逻辑路由、监控统计和业务隔离等场景
     * </p>
     */
    public static final String HEADER_BIZ_TYPE = "biz-type";

}
