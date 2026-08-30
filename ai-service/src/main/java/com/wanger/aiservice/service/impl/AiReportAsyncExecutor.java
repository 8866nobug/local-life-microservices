package com.wanger.aiservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanger.aiservice.client.DifyClient;
import com.wanger.aiservice.dto.ReportResult;
import com.wanger.aiservice.dto.ReportStatus;
import com.wanger.aiservice.dto.ShopDTO;
import com.wanger.aiservice.exception.DifyException;
import com.wanger.aiservice.feign.ShopFeignClient;
import com.wanger.common.constants.RedisConstants;
import com.wanger.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 报告异步生成器。
 * 独立 bean 承载 @Async 方法，避免在 {@link AiReportServiceImpl} 内自调用导致的代理失效。
 * 主流程调 Dify；Dify 失败则降级为规则模板报告（Feign 拉店铺基础数据拼接）。
 */
@Slf4j
@Component
public class AiReportAsyncExecutor {

    @Resource
    private DifyClient difyClient;
    @Resource
    private ShopFeignClient shopFeignClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("aiReportExecutor")
    public void generateAsync(Long taskId, Long shopId) {
        String key = RedisConstants.AI_REPORT_KEY + taskId;
        try {
            Map<String, Object> outputs = difyClient.runWorkflow(
                    DifyClient.APP_SHOP_ANALYSIS, Map.of("shopId", String.valueOf(shopId)));
            String report = outputs == null ? null : (String) outputs.get("report");
            if (report == null || report.isBlank()) {
                throw new DifyException("workflow 未返回 report 字段");
            }
            save(key, new ReportResult(ReportStatus.DONE, report));
        } catch (Exception e) {
            log.warn("Dify 分析失败，降级为规则报告。taskId={}, shopId={}", taskId, shopId, e);
            save(key, new ReportResult(ReportStatus.FALLBACK, fallbackReport(shopId)));
        }
    }

    /**
     * 降级：Dify 不可用时，用 Feign 拉店铺基础数据拼一段规则模板报告。
     */
    private String fallbackReport(Long shopId) {
        try {
            Result result = shopFeignClient.getShopById(shopId);
            ShopDTO shop = objectMapper.convertValue(result.getData(), ShopDTO.class);
            double score = shop.getScore() == null ? 0 : shop.getScore() / 10.0;
            return String.format(
                    "【%s】经营快照（AI 分析暂不可用，以下为基础数据汇总）\n\n" +
                            "- 评分：%.1f 分\n" +
                            "- 销量：%s\n" +
                            "- 评论数：%s\n" +
                            "- 均价：%s 元\n" +
                            "- 商圈：%s\n\n" +
                            "AI 分析服务暂时不可用，已为您降级展示基础数据，请稍后重试获取完整洞察。",
                    shop.getName(), score, shop.getSold(), shop.getComments(), shop.getAvgPrice(), shop.getArea());
        } catch (Exception e) {
            log.warn("降级拉取店铺数据失败，shopId={}", shopId, e);
            return "AI 分析服务暂时不可用，请稍后重试。";
        }
    }

    private void save(String key, ReportResult reportResult) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(reportResult),
                    RedisConstants.AI_REPORT_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("写入报告结果失败，key={}", key, e);
        }
    }
}
