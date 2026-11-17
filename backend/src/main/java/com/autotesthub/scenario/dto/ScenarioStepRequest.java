package com.autotesthub.scenario.dto;

import jakarta.validation.constraints.NotNull;

public record ScenarioStepRequest(

        @NotNull(message = "API用例ID不能为空")
        Long caseId

) {
}