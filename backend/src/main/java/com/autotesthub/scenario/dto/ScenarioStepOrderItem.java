package com.autotesthub.scenario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScenarioStepOrderItem(

        @NotNull(message = "步骤ID不能为空")
        Long stepId,

        @NotNull(message = "执行顺序不能为空")
        @Positive(message = "执行顺序必须大于0")
        Integer sortOrder

) {
}