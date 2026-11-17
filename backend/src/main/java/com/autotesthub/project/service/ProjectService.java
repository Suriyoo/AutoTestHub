
package com.autotesthub.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.autotesthub.project.dto.ProjectRequest;
import com.autotesthub.project.entity.Project;
import com.autotesthub.project.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    // 新增项目
    @Transactional
    public Project create(ProjectRequest request, Long ownerId) {

        Project project = new Project();

        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setOwnerId(ownerId);

        projectMapper.insert(project);

        return getById(project.getId(), ownerId);
    }

    // 查询当前用户的项目列表
    public List<Project> list(Long ownerId) {

        return projectMapper.selectList(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getOwnerId, ownerId)
                        .orderByDesc(Project::getUpdatedAt)
        );
    }

    // 查询项目详情
    public Project getById(Long id, Long ownerId) {

        Project project = projectMapper.selectOne(
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getId, id)
                        .eq(Project::getOwnerId, ownerId)
        );

        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        return project;
    }

    // 修改项目
    @Transactional
    public Project update(
            Long id,
            ProjectRequest request,
            Long ownerId
    ) {

        Project project = getById(id, ownerId);

        project.setName(request.name().trim());
        project.setDescription(request.description());

        // 交给数据库自动更新 updated_at
        project.setUpdatedAt(null);

        projectMapper.updateById(project);

        return getById(id, ownerId);
    }

    // 删除项目
    @Transactional
    public void delete(Long id, Long ownerId) {

        Project project = getById(id, ownerId);

        projectMapper.deleteById(project.getId());
    }
}
