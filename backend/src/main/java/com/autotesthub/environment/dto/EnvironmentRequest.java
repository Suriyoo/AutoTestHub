package com.autotesthub.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnvironmentRequest(

        @NotNull(message = "项目ID不能为空")
        Long projectId,

        @NotBlank(message = "环境名称不能为空")
        @Size(max = 64, message = "环境名称不能超过64个字符")
        String name,

        @NotBlank(message = "Base URL不能为空")
        @Size(max = 500, message = "Base URL不能超过500个字符")
        String baseUrl,

        String variablesJson
) {
}