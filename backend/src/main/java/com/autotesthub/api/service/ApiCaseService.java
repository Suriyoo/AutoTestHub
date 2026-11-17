
package com.autotesthub.api.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.autotesthub.api.dto.ApiCaseRequest;
import com.autotesthub.api.entity.ApiCase;
import com.autotesthub.api.mapper.ApiCaseMapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ApiCaseService {

    private final ApiCaseMapper apiCaseMapper;
    private final ProjectMapper projectMapper;
    private final ObjectMapper objectMapper;

    public ApiCaseService(
            ApiCaseMapper apiCaseMapper,
            ProjectMapper projectMapper,
            ObjectMapper objectMapper) {
        this.apiCaseMapper = apiCaseMapper;
        this.projectMapper = projectMapper;
        this.objectMapper = objectMapper;
    }

    // 检查当前用户是否拥有该项目
    private void checkProjectOwner(Long projectId, Long userId) {
        Project project = projectMapper.selectOne(
                new QueryWrapper<Project>()
                        .eq("id", projectId)
                        .eq("owner_id", userId)
        );

        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
    }

    // 检查 JSON 字符串是否合法，且必须是 JSON 对象
    private void validateJson(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(json);

            if (node == null || !node.isObject()) {
                throw new RuntimeException(fieldName + "必须是JSON对象");
            }
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(fieldName + "不是合法的JSON格式");
        }
    }

    // 新增用例
    @Transactional
    public ApiCase create(ApiCaseRequest request, Long userId) {
        checkProjectOwner(request.projectId(), userId);

        validateJson(request.headersJson(), "请求头");
        validateJson(request.paramsJson(), "请求参数");

        ApiCase apiCase = new ApiCase();
        copyRequest(apiCase, request);

        apiCase.setProjectId(request.projectId());
        apiCase.setMethod(request.method().toUpperCase(Locale.ROOT));

        apiCaseMapper.insert(apiCase);

        return apiCase;
    }

    // 查询项目下的用例列表
    public List<ApiCase> list(Long projectId, Long userId) {
        QueryWrapper<ApiCase> wrapper = new QueryWrapper<>();
        if (projectId != null) {
            checkProjectOwner(projectId, userId);
            wrapper.eq("project_id", projectId);
        } else {
            List<Long> ownedProjectIds = projectMapper.selectList(
                    new QueryWrapper<Project>().eq("owner_id", userId)
            ).stream().map(Project::getId).toList();
            if (ownedProjectIds.isEmpty()) {
                return List.of();
            }
            wrapper.in("project_id", ownedProjectIds);
        }
        return apiCaseMapper.selectList(wrapper.orderByDesc("updated_at"));
    }

    // 查询用例详情
    public ApiCase getById(Long id, Long userId) {
        ApiCase apiCase = apiCaseMapper.selectById(id);

        if (apiCase == null) {
            throw new RuntimeException("用例不存在");
        }

        checkProjectOwner(apiCase.getProjectId(), userId);

        return apiCase;
    }

    // 修改用例
    @Transactional
    public ApiCase update(Long id, ApiCaseRequest request, Long userId) {
        ApiCase existing = getById(id, userId);

        if (!existing.getProjectId().equals(request.projectId())) {
            throw new RuntimeException("不允许修改用例所属项目");
        }

        validateJson(request.headersJson(), "请求头");
        validateJson(request.paramsJson(), "请求参数");

        UpdateWrapper<ApiCase> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .eq("project_id", existing.getProjectId())
                .set("name", request.name())
                .set("method", request.method().toUpperCase(Locale.ROOT))
                .set("url", request.url())
                .set("headers_json", request.headersJson())
                .set("params_json", request.paramsJson())
                .set("body", request.body())
                .set("expected_status", request.expectedStatus());

        apiCaseMapper.update(null, wrapper);

        return getById(id, userId);
    }

    // 删除用例
    @Transactional
    public void delete(Long id, Long userId) {
        ApiCase apiCase = getById(id, userId);

        apiCaseMapper.delete(
                new QueryWrapper<ApiCase>()
                        .eq("id", id)
                        .eq("project_id", apiCase.getProjectId())
        );
    }

    private void copyRequest(ApiCase apiCase, ApiCaseRequest request) {
        apiCase.setName(request.name());
        apiCase.setMethod(request.method());
        apiCase.setUrl(request.url());
        apiCase.setHeadersJson(request.headersJson());
        apiCase.setParamsJson(request.paramsJson());
        apiCase.setBody(request.body());
        apiCase.setExpectedStatus(request.expectedStatus());
    }
}
