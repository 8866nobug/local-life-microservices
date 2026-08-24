package com.wanger.common.constants;

/**
 * 服务间透传的请求头常量。
 * Gateway 校验 token 后写入 X-User-Id，下游服务读取该头获取当前用户身份。
 */
public class HeaderConstants {
    public static final String USER_ID = "X-User-Id";
}
