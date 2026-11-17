package com.autotesthub.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestScenarioRequest(

        @NotNull(message = "项目ID不能为空")
        Long projectId,

        @NotBlank(message = "场景名称不能为空")
        @Size(max = 150, message = "场景名称不能超过150个字符")
        String name,

        @Size(max = 500, message = "场景描述不能超过500个字符")
        String description

) {
}