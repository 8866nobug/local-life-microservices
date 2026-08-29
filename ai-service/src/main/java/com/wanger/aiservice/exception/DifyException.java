package com.wanger.aiservice.exception;

/**
 * Dify 调用失败异常，作为「主流程失败 → 降级兜底」的边界信号。
 */
public class DifyException extends RuntimeException {

    public DifyException(String message) {
        super(message);
    }

    public DifyException(String message, Throwable cause) {
        super(message, cause);
    }
}
