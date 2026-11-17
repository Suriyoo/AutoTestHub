
package com.autotesthub.scenario.execution.controller;

import com.autotesthub.scenario.execution.service.ScenarioExecutionReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioExecutionReportController {

    private final ScenarioExecutionReportService reportService;

    public ScenarioExecutionReportController(
            ScenarioExecutionReportService reportService
    ) {
        this.reportService = reportService;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");

        if (!(userId instanceof Number number)) {
            throw new RuntimeException("请先登录");
        }

        return number.longValue();
    }

    @GetMapping("/{id}/executions/{executionId}/report")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable Long id,
            @PathVariable Long executionId,
            HttpServletRequest request
    ) {
        byte[] file = reportService.generateReport(
                id,
                executionId,
                getUserId(request)
        );

        String filename = "AutoTestHub-测试报告-"
                + executionId + ".xlsx";

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentLength(file.length)
                .body(file);
    }
}
