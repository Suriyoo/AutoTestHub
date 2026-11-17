
package com.autotesthub.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("api_case")
public class ApiCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    private String name;
    private String method;
    private String url;

    @TableField("headers_json")
    private String headersJson;

    @TableField("params_json")
    private String paramsJson;

    private String body;

    @TableField("expected_status")
    private Integer expectedStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getHeadersJson() { return headersJson; }
    public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }

    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Integer getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
