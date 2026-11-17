package com.autotesthub.scenario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScenarioStepOrderRequest(

        @NotEmpty(message = "步骤顺序不能为空")
        List<@Valid ScenarioStepOrderItem> steps

) {
}