package com.autotesthub.api.controller;

import com.autotesthub.api.entity.ApiExecutionRecord;
import com.autotesthub.api.service.ApiExecutionRecordService;
import com.autotesthub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/execution-records")
public class ApiExecutionRecordController {

    private final ApiExecutionRecordService recordService;

    public ApiExecutionRecordController(ApiExecutionRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 查询当前用户的 API 执行历史。
     * projectId 不传时查询当前用户全部项目；传入时查询指定项目。
     */
    @GetMapping
    public ApiResponse<List<ApiExecutionRecord>> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long caseId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        Long userId = getUserId(request);

        List<ApiExecutionRecord> records = recordService.list(
                projectId, caseId, result, startDate, endDate, userId
        );

        return ApiResponse.ok(records);
    }

    /**
     * 查询单条执行记录详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<ApiExecutionRecord> getById(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = getUserId(request);
        ApiExecutionRecord record = recordService.getById(id, userId);

        return ApiResponse.ok(record);
    }

    /**
     * 获取 JWT 解析后的用户ID。
     */
    private Long getUserId(HttpServletRequest request) {
        Object userIdObject = request.getAttribute("userId");

        if (!(userIdObject instanceof Number number)) {
            throw new RuntimeException("请先登录");
        }

        return number.longValue();
    }
}
