package com.wanger.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报告任务结果，序列化为 JSON 存 Redis。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResult {

    private ReportStatus status;

    /** markdown 报告正文 */
    private String content;
}
