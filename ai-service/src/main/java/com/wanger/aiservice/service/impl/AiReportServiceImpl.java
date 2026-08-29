package com.wanger.aiservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanger.aiservice.dto.ReportResult;
import com.wanger.aiservice.dto.ReportStatus;
import com.wanger.aiservice.service.AiReportService;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.dto.Result;
import com.wanger.common.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 报告任务提交与查询。
 * 注意：异步生成放在独立的 {@link AiReportAsyncExecutor}，而非本类内部自调用，
 * 以规避 @Async 自调用不走代理导致的失效。
 */
@Slf4j
@Service
public class AiReportServiceImpl implements AiReportService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private AiReportAsyncExecutor asyncExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long submitReport(Long shopId) {
        Long taskId = redisIdWorker.nextId("ai:report");
        String key = RedisConstants.AI_REPORT_KEY + taskId;
        try {
            stringRedisTemplate.opsForValue().set(key,
                    objectMapper.writeValueAsString(new ReportResult(ReportStatus.PENDING, null)),
                    RedisConstants.AI_REPORT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("写入任务初始状态失败，taskId={}", taskId, e);
            throw new RuntimeException("创建报告任务失败", e);
        }
        // 触发异步生成（独立 bean，@Async 生效）
        asyncExecutor.generateAsync(taskId, shopId);
        return taskId;
    }

    @Override
    public Result getReport(Long taskId) {
        String key = RedisConstants.AI_REPORT_KEY + taskId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return Result.fail("报告不存在或已过期，请重新生成");
        }
        try {
            return Result.ok(objectMapper.readValue(json, ReportResult.class));
        } catch (Exception e) {
            log.error("解析报告失败，taskId={}", taskId, e);
            return Result.fail("报告解析失败");
        }
    }
}
