package com.wanger.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权相关配置，从 Nacos 配置中心读取。
 * 配置变更时由 ConfigurationPropertiesRebinder 自动重绑定，无需 @RefreshScope（二者别同时用）。
 */
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * 无需登录即可访问的路径，支持 Ant 风格（如 /shop/**）
     */
    private List<String> whitelist = new ArrayList<>();

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
