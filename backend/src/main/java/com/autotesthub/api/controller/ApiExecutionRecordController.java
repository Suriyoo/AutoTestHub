
package com.autotesthub.api.controller;

import com.autotesthub.api.entity.ApiExecutionRecord;
import com.autotesthub.api.service.ApiExecutionRecordService;
import com.autotesthub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/execution-records")
public class ApiExecutionRecordController {

    private final ApiExecutionRecordService recordService;

    public ApiExecutionRecordController(
            ApiExecutionRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 查询项目执行历史。
     */
    @GetMapping
    public ApiResponse<List<ApiExecutionRecord>> list(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long caseId,
            HttpServletRequest request) {

        Long userId = getUserId(request);

        List<ApiExecutionRecord> records =
                recordService.list(projectId, caseId, userId);

        return ApiResponse.ok(records);
    }

    /**
     * 查询执行记录详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<ApiExecutionRecord> getById(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = getUserId(request);

        ApiExecutionRecord record =
                recordService.getById(id, userId);

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
