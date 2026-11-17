
package com.autotesthub.scenario.execution.service;

import com.autotesthub.api.entity.ApiExecutionRecord;
import com.autotesthub.api.mapper.ApiExecutionRecordMapper;
import com.autotesthub.environment.entity.TestEnvironment;
import com.autotesthub.environment.mapper.TestEnvironmentMapper;
import com.autotesthub.scenario.entity.TestScenario;
import com.autotesthub.scenario.mapper.TestScenarioMapper;
import com.autotesthub.scenario.execution.entity.ScenarioExecution;
import com.autotesthub.scenario.execution.entity.ScenarioExecutionStep;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ScenarioExecutionReportService {

    private final ScenarioExecutionService executionService;
    private final TestScenarioMapper scenarioMapper;
    private final TestEnvironmentMapper environmentMapper;
    private final ApiExecutionRecordMapper apiExecutionRecordMapper;

    public ScenarioExecutionReportService(
            ScenarioExecutionService executionService,
            TestScenarioMapper scenarioMapper,
            TestEnvironmentMapper environmentMapper,
            ApiExecutionRecordMapper apiExecutionRecordMapper) {
        this.executionService = executionService;
        this.scenarioMapper = scenarioMapper;
        this.environmentMapper = environmentMapper;
        this.apiExecutionRecordMapper = apiExecutionRecordMapper;
    }

    public byte[] generateReport(Long scenarioId, Long executionId, Long userId) {
        try {
            Map<String, Object> detail =
                    executionService.getExecutionDetail(scenarioId, executionId, userId);

            ScenarioExecution execution =
                    (ScenarioExecution) detail.get("execution");

            @SuppressWarnings("unchecked")
            List<ScenarioExecutionStep> steps =
                    (List<ScenarioExecutionStep>) detail.get("steps");

            TestScenario scenario = scenarioMapper.selectById(scenarioId);
            TestEnvironment environment =
                    environmentMapper.selectById(execution.getEnvironmentId());

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);

                createSummarySheet(
                        workbook, headerStyle, scenario, environment, execution);

                createStepsSheet(workbook, headerStyle, steps);

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("生成场景执行报告失败：" + e.getMessage(), e);
        }
    }

    private void createSummarySheet(
            Workbook workbook,
            CellStyle headerStyle,
            TestScenario scenario,
            TestEnvironment environment,
            ScenarioExecution execution) {

        Sheet sheet = workbook.createSheet("执行概况");

        String[][] rows = {
                {"报告项目", "执行结果"},
                {"场景名称", scenario == null ? "" : scenario.getName()},
                {"场景ID", String.valueOf(execution.getScenarioId())},
                {"执行ID", String.valueOf(execution.getId())},
                {"环境名称", environment == null ? "" : environment.getName()},
                {"执行状态", execution.getStatus()},
                {"总步骤数", String.valueOf(execution.getTotalSteps())},
                {"通过步骤数", String.valueOf(execution.getPassedSteps())},
                {"失败步骤数", String.valueOf(execution.getFailedSteps())},
                {"通过率", calculatePassRate(execution)},
                {"总耗时(ms)", String.valueOf(execution.getDuration())},
                {"开始时间", String.valueOf(execution.getStartedAt())},
                {"结束时间", String.valueOf(execution.getFinishedAt())}
        };

        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);

            for (int j = 0; j < rows[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(rows[i][j]);

                if (i == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
        }

        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 9000);
    }

    private void createStepsSheet(
            Workbook workbook,
            CellStyle headerStyle,
            List<ScenarioExecutionStep> steps) {

        Sheet sheet = workbook.createSheet("步骤执行明细");

        String[] headers = {
                "步骤ID", "排序", "API用例ID", "执行状态",
                "耗时(ms)", "失败原因", "API执行记录ID",
                "请求方法", "请求URL", "请求头",
                "请求参数", "请求体", "预期状态码",
                "实际状态码", "响应内容", "API错误信息"
        };

        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;

        for (ScenarioExecutionStep step : steps) {
            Row row = sheet.createRow(rowIndex++);

            ApiExecutionRecord record = null;
            if (step.getExecutionRecordId() != null) {
                record = apiExecutionRecordMapper.selectById(
                        step.getExecutionRecordId());
            }

            String[] values = {
                    value(step.getId()),
                    value(step.getSortOrder()),
                    value(step.getCaseId()),
                    value(step.getStatus()),
                    value(step.getDuration()),
                    value(step.getErrorMessage()),
                    value(step.getExecutionRecordId()),
                    record == null ? "" : value(record.getMethod()),
                    record == null ? "" : value(record.getRequestUrl()),
                    record == null ? "" : value(record.getRequestHeaders()),
                    record == null ? "" : value(record.getRequestParams()),
                    record == null ? "" : value(record.getRequestBody()),
                    record == null ? "" : value(record.getExpectedStatus()),
                    record == null ? "" : value(record.getActualStatus()),
                    record == null ? "" : value(record.getResponseBody()),
                    record == null ? "" : value(record.getErrorMessage())
            };

            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 5500);
        }

        sheet.setColumnWidth(5, 10000);
        sheet.setColumnWidth(8, 10000);
        sheet.setColumnWidth(9, 10000);
        sheet.setColumnWidth(10, 10000);
        sheet.setColumnWidth(11, 10000);
        sheet.setColumnWidth(14, 12000);
        sheet.setColumnWidth(15, 10000);
    }

    private String calculatePassRate(ScenarioExecution execution) {
        int total = execution.getTotalSteps() == null
                ? 0 : execution.getTotalSteps();

        int passed = execution.getPassedSteps() == null
                ? 0 : execution.getPassedSteps();

        if (total == 0) {
            return "0%";
        }

        return String.format("%.2f%%", passed * 100.0 / total);
    }

    private String value(Object object) {
        return object == null ? "" : String.valueOf(object);
    }
}
