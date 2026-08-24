package com.wanger.userservice.interceptor;

import com.wanger.common.constants.HeaderConstants;
import com.wanger.common.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 身份上下文拦截器。
 * 鉴权已由 Gateway 完成，这里只负责把 Gateway 透传的 X-User-Id 放入 ThreadLocal，
 * 并在请求结束后清理，防止线程池复用导致用户串号。
 */
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(HeaderConstants.USER_ID);
        if (userId != null && !userId.isBlank()) {
            try {
                UserHolder.saveUserId(Long.valueOf(userId));
            } catch (NumberFormatException ignored) {
                // 非法身份头，当作未登录处理（正常情况由 Gateway 保证格式）
            }
        }
        // 放行：不在这里做鉴权
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
