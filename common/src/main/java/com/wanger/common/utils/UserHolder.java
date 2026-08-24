package com.wanger.common.utils;

/**
 * 当前登录用户上下文，基于 ThreadLocal。
 * 身份由 Gateway 校验后通过 X-User-Id 头透传，这里只保存 userId。
 */
public class UserHolder {
    private static final ThreadLocal<Long> tl = new ThreadLocal<>();

    public static void saveUserId(Long userId){
        tl.set(userId);
    }

    public static Long getUserId(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
