package com.wanger.aiservice.controller;

import com.wanger.aiservice.service.AiReportService;
import com.wanger.common.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 店铺经营分析报告接口。
 * POST 生成（需登录，走网关鉴权）；GET 轮询结果（taskId 即凭证，网关白名单放行）。
 */
@RestController
@RequestMapping("/ai/report")
public class AiReportController {

    @Resource
    private AiReportService aiReportService;

    /**
     * 提交店铺分析报告生成任务，返回 taskId。
     */
    @PostMapping("/shop/{shopId}")
    public Result generateReport(@PathVariable("shopId") Long shopId) {
        Long taskId = aiReportService.submitReport(shopId);
        return Result.ok(taskId);
    }

    /**
     * 按 taskId 轮询报告结果。
     */
    @GetMapping("/{taskId}")
    public Result getReport(@PathVariable("taskId") Long taskId) {
        return aiReportService.getReport(taskId);
    }
}
