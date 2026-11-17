package com.autotesthub.environment.service;

import com.autotesthub.environment.dto.EnvironmentRequest;
import com.autotesthub.environment.entity.TestEnvironment;
import com.autotesthub.environment.mapper.TestEnvironmentMapper;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EnvironmentService {

    private final TestEnvironmentMapper environmentMapper;
    private final ProjectMapper projectMapper;
    private final ObjectMapper objectMapper;

    public EnvironmentService(
            TestEnvironmentMapper environmentMapper,
            ProjectMapper projectMapper,
            ObjectMapper objectMapper) {
        this.environmentMapper = environmentMapper;
        this.projectMapper = projectMapper;
        this.objectMapper = objectMapper;
    }

    private void checkProjectOwner(Long projectId, Long userId) {
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.eq("id", projectId);
        wrapper.eq("owner_id", userId);

        if (projectMapper.selectOne(wrapper) == null) {
            throw new RuntimeException("项目不存在");
        }
    }

    private void validateVariablesJson(String json) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            Map<String, Object> variables =
                    objectMapper.readValue(
                            json,
                            new TypeReference<Map<String, Object>>() {}
                    );

            if (variables == null) {
                throw new RuntimeException("环境变量必须是JSON对象");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("环境变量不是合法的JSON格式");
        }
    }

    public TestEnvironment create(
            EnvironmentRequest request, Long userId) {

        checkProjectOwner(request.projectId(), userId);
        validateVariablesJson(request.variablesJson());

        TestEnvironment environment = new TestEnvironment();
        environment.setProjectId(request.projectId());
        environment.setName(request.name());
        environment.setBaseUrl(request.baseUrl());
        environment.setVariablesJson(request.variablesJson());

        environmentMapper.insert(environment);
        return environment;
    }

    public List<TestEnvironment> list(Long projectId, Long userId) {
        checkProjectOwner(projectId, userId);

        QueryWrapper<TestEnvironment> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        wrapper.orderByDesc("updated_at");

        return environmentMapper.selectList(wrapper);
    }

    public TestEnvironment getById(Long id, Long userId) {
        TestEnvironment environment = environmentMapper.selectById(id);

        if (environment == null) {
            throw new RuntimeException("环境不存在");
        }

        checkProjectOwner(environment.getProjectId(), userId);
        return environment;
    }

    public TestEnvironment update(
            Long id, EnvironmentRequest request, Long userId) {

        TestEnvironment existing = getById(id, userId);

        if (!existing.getProjectId().equals(request.projectId())) {
            throw new RuntimeException("不允许修改环境所属项目");
        }

        validateVariablesJson(request.variablesJson());

        existing.setName(request.name());
        existing.setBaseUrl(request.baseUrl());
        existing.setVariablesJson(request.variablesJson());

        environmentMapper.updateById(existing);

        return getById(id, userId);
    }

    public void delete(Long id, Long userId) {
        getById(id, userId);
        environmentMapper.deleteById(id);
    }
}