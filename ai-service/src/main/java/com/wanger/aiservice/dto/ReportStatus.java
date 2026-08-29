package com.wanger.aiservice.dto;

/**
 * 报告任务状态。
 * PENDING：生成中；DONE：Dify 生成成功；FALLBACK：Dify 失败已降级为规则模板。
 */
public enum ReportStatus {
    PENDING, DONE, FALLBACK
}
