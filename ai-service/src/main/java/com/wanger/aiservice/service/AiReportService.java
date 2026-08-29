package com.wanger.aiservice.service;

import com.wanger.common.dto.Result;

/**
 * 店铺经营分析报告服务。
 */
public interface AiReportService {

    /**
     * 提交报告生成任务，返回 taskId（前端凭此轮询结果）。
     */
    Long submitReport(Long shopId);

    /**
     * 按 taskId 查询报告结果。
     */
    Result getReport(Long taskId);
}
