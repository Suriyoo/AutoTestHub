
package com.autotesthub.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApiCaseRequest(

        @NotNull(message = "所属项目不能为空")
        Long projectId,

        @NotBlank(message = "用例名称不能为空")
        @Size(max = 150, message = "用例名称不能超过150个字符")
        String name,

        @NotBlank(message = "请求方法不能为空")
        @Pattern(
                regexp = "(?i)GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS",
                message = "不支持的HTTP请求方法"
        )
        String method,

        @NotBlank(message = "请求URL不能为空")
        @Size(max = 1000, message = "URL不能超过1000个字符")
        String url,

        String headersJson,

        String paramsJson,

        String body,

        @NotNull(message = "预期状态码不能为空")
        @Min(value = 100, message = "HTTP状态码不能小于100")
        @Max(value = 599, message = "HTTP状态码不能大于599")
        Integer expectedStatus

) {
}
