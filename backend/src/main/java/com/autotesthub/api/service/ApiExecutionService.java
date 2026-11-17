
package com.autotesthub.api.service;

import com.autotesthub.api.entity.ApiCase;
import com.autotesthub.api.entity.ApiExecutionRecord;
import com.autotesthub.api.mapper.ApiCaseMapper;
import com.autotesthub.api.mapper.ApiExecutionRecordMapper;
import com.autotesthub.environment.entity.TestEnvironment;
import com.autotesthub.environment.mapper.TestEnvironmentMapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ApiExecutionService {

    private final ApiCaseMapper apiCaseMapper;
    private final ProjectMapper projectMapper;
    private final TestEnvironmentMapper environmentMapper;
    private final ApiExecutionRecordMapper executionRecordMapper;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ApiExecutionService(
            ApiCaseMapper apiCaseMapper,
            ProjectMapper projectMapper,
            TestEnvironmentMapper environmentMapper,
            ApiExecutionRecordMapper executionRecordMapper,
            ObjectMapper objectMapper) {

        this.apiCaseMapper = apiCaseMapper;
        this.projectMapper = projectMapper;
        this.environmentMapper = environmentMapper;
        this.executionRecordMapper = executionRecordMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(
            Long caseId, Long userId, Long environmentId) {

        // 1. 查询用例
        ApiCase apiCase = apiCaseMapper.selectById(caseId);

        if (apiCase == null) {
            throw new RuntimeException("用例不存在");
        }

        // 2. 检查项目归属
        Project project = projectMapper.selectById(
                apiCase.getProjectId());

        if (project == null
                || project.getOwnerId() == null
                || !project.getOwnerId().equals(userId)) {
            throw new RuntimeException("用例不存在");
        }

        // 3. 检查环境是否属于当前用例的项目
        TestEnvironment environment = null;

        if (environmentId != null) {
            environment = environmentMapper.selectById(environmentId);

            if (environment == null
                    || !apiCase.getProjectId().equals(
                            environment.getProjectId())) {
                throw new RuntimeException("环境不存在");
            }
        }

        // 4. 读取环境变量
        Map<String, Object> variables;

        try {
            if (environment != null
                    && environment.getVariablesJson() != null
                    && !environment.getVariablesJson().isBlank()) {

                variables = objectMapper.readValue(
                        environment.getVariablesJson(),
                        new TypeReference<Map<String, Object>>() {});
            } else {
                variables = new LinkedHashMap<>();
            }
        } catch (Exception e) {
            throw new RuntimeException("环境变量JSON格式错误");
        }

        long startTime = System.currentTimeMillis();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("caseId", caseId);
        result.put("caseName", apiCase.getName());

        if (environment != null) {
            result.put("environmentId", environment.getId());
            result.put("environmentName", environment.getName());
        }

        // 执行过程中的请求信息，供历史记录保存
        String requestUrl = null;
        String method = apiCase.getMethod();
        String requestBody = null;

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        Map<String, Object> requestParams = new LinkedHashMap<>();

        try {
            // 5. 替换 URL 中的环境变量
            String caseUrl = replaceVariables(
                    apiCase.getUrl(), variables);

            // 6. 拼接环境 Base URL
            requestUrl = buildUrl(
                    caseUrl,
                    environment == null ? null : environment.getBaseUrl());

            // 7. 处理 Headers 和环境变量
            Map<String, String> headers = parseJsonMap(
                    apiCase.getHeadersJson());

            Map<String, String> replacedHeaders =
                    new LinkedHashMap<>();

            headers.forEach((key, value) ->
                    replacedHeaders.put(
                            replaceVariables(key, variables),
                            replaceVariables(value, variables)));

            requestHeaders = replacedHeaders;

            // 8. 处理 Params 和环境变量
            Map<String, Object> params = parseJsonObject(
                    apiCase.getParamsJson());

            Map<String, Object> replacedParams =
                    new LinkedHashMap<>();

            params.forEach((key, value) ->
                    replacedParams.put(
                            replaceVariables(key, variables),
                            replaceValue(value, variables)));

            requestParams = replacedParams;

            requestUrl = appendParams(requestUrl, replacedParams);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(30));

            replacedHeaders.forEach(builder::header);

            // 9. 处理请求方法和 Body
            method = apiCase.getMethod().toUpperCase();

            requestBody = apiCase.getBody() == null
                    ? ""
                    : replaceVariables(apiCase.getBody(), variables);

            switch (method) {
                case "GET", "HEAD", "DELETE", "OPTIONS" ->
                        builder.method(
                                method,
                                HttpRequest.BodyPublishers.noBody());

                case "POST", "PUT", "PATCH" ->
                        builder.method(
                                method,
                                HttpRequest.BodyPublishers.ofString(
                                        requestBody,
                                        StandardCharsets.UTF_8));

                default -> throw new IllegalArgumentException(
                        "暂不支持的请求方法：" + method);
            }

            // 10. 发送请求
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            long duration = System.currentTimeMillis() - startTime;

            // 11. 判断预期状态码
            boolean passed = response.statusCode()
                    == apiCase.getExpectedStatus();

            result.put("success", true);
            result.put("passed", passed);
            result.put("expectedStatus", apiCase.getExpectedStatus());
            result.put("actualStatus", response.statusCode());
            result.put("duration", duration);
            result.put("responseBody", response.body());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            result.put("success", false);
            result.put("passed", false);
            result.put("duration", duration);
            result.put("errorMessage", e.getMessage());
        }

        // 12. 保存执行历史
        ApiExecutionRecord record = new ApiExecutionRecord();

        record.setCaseId(apiCase.getId());
        record.setProjectId(apiCase.getProjectId());

        record.setEnvironmentId(
                environment == null ? null : environment.getId());

        record.setCaseName(apiCase.getName());

        record.setEnvironmentName(
                environment == null ? null : environment.getName());

        record.setMethod(method);
        record.setRequestUrl(requestUrl);

        record.setRequestHeaders(toJson(requestHeaders));
        record.setRequestParams(toJson(requestParams));
        record.setRequestBody(requestBody);

        record.setSuccess(
                Boolean.TRUE.equals(result.get("success")) ? 1 : 0);

        record.setPassed(
                Boolean.TRUE.equals(result.get("passed")) ? 1 : 0);

        record.setExpectedStatus(apiCase.getExpectedStatus());

        Object actualStatus = result.get("actualStatus");
        if (actualStatus instanceof Number number) {
            record.setActualStatus(number.intValue());
        }

        Object duration = result.get("duration");
        if (duration instanceof Number number) {
            record.setDuration(number.longValue());
        }

        record.setResponseBody(
                (String) result.get("responseBody"));

        record.setErrorMessage(
                (String) result.get("errorMessage"));

        record.setExecutedBy(userId);

        // executedAt 使用数据库 DEFAULT CURRENT_TIMESTAMP
        executionRecordMapper.insert(record);

        // 将记录ID返回给前端
        result.put("executionRecordId", record.getId());

        return result;
    }

    /**
     * 将对象转换为 JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将用例 URL 与环境 Base URL 拼接。
     * 如果用例 URL 已经是完整 HTTP URL，则保留原地址。
     */
    private String buildUrl(String url, String baseUrl) {

        URI uri = URI.create(url);

        if (uri.isAbsolute()) {
            return url;
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "用例URL不是完整地址，请指定测试环境");
        }

        String base = baseUrl.replaceAll("/+$", "");
        String path = url.replaceAll("^/+", "");

        return base + "/" + path;
    }

    /**
     * 替换字符串中的 {{变量名}}。
     */
    private String replaceVariables(
            String value, Map<String, Object> variables) {

        if (value == null || variables == null
                || variables.isEmpty()) {
            return value;
        }

        String result = value;

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";

            String replacement = entry.getValue() == null
                    ? ""
                    : String.valueOf(entry.getValue());

            result = result.replace(placeholder, replacement);
        }

        return result;
    }

    /**
     * 递归替换参数对象中的字符串变量。
     */
    private Object replaceValue(
            Object value, Map<String, Object> variables) {

        if (value instanceof String text) {
            return replaceVariables(text, variables);
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> replaced = new LinkedHashMap<>();

            map.forEach((key, item) ->
                    replaced.put(
                            replaceVariables(
                                    String.valueOf(key), variables),
                            replaceValue(item, variables)));

            return replaced;
        }

        if (value instanceof java.util.List<?> list) {
            return list.stream()
                    .map(item -> replaceValue(item, variables))
                    .toList();
        }

        return value;
    }

    private Map<String, String> parseJsonMap(String json)
            throws Exception {

        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }

        return objectMapper.readValue(
                json, new TypeReference<Map<String, String>>() {});
    }

    private Map<String, Object> parseJsonObject(String json)
            throws Exception {

        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }

        return objectMapper.readValue(
                json, new TypeReference<Map<String, Object>>() {});
    }

    private String appendParams(
            String url, Map<String, Object> params) {

        if (params.isEmpty()) {
            return url;
        }

        StringBuilder query = new StringBuilder();

        params.forEach((key, value) -> {
            if (query.length() > 0) {
                query.append("&");
            }

            query.append(URLEncoder.encode(
                    key, StandardCharsets.UTF_8));

            query.append("=");

            query.append(URLEncoder.encode(
                    String.valueOf(value),
                    StandardCharsets.UTF_8));
        });

        return url + (url.contains("?") ? "&" : "?") + query;
    }
}
