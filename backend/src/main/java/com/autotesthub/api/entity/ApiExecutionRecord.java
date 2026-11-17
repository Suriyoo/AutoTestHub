
package com.autotesthub.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("api_execution_record")
public class ApiExecutionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;
    private Long projectId;
    private Long environmentId;

    private String caseName;
    private String environmentName;

    private String method;
    private String requestUrl;
    private String requestHeaders;
    private String requestParams;
    private String requestBody;

    private Integer success;
    private Integer passed;
    private Integer expectedStatus;
    private Integer actualStatus;

    private Long duration;
    private String responseBody;
    private String errorMessage;

    private Long executedBy;
    private LocalDateTime executedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(Long environmentId) { this.environmentId = environmentId; }

    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }

    public String getEnvironmentName() { return environmentName; }
    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }

    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }

    public Integer getPassed() { return passed; }
    public void setPassed(Integer passed) { this.passed = passed; }

    public Integer getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(Integer expectedStatus) { this.expectedStatus = expectedStatus; }

    public Integer getActualStatus() { return actualStatus; }
    public void setActualStatus(Integer actualStatus) { this.actualStatus = actualStatus; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getExecutedBy() { return executedBy; }
    public void setExecutedBy(Long executedBy) { this.executedBy = executedBy; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
