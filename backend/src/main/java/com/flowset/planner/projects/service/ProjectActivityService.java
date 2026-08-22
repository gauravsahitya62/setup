package com.flowset.planner.projects.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.common.ProjectActivityType;
import com.flowset.planner.projects.dto.ProjectActivityResponse;
import com.flowset.planner.projects.entity.ProjectActivityEntity;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectActivityRepository;
import com.flowset.shared.dto.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectActivityService {

    private static final Logger log = LoggerFactory.getLogger(ProjectActivityService.class);

    private final ProjectActivityRepository activityRepository;
    private final ProjectAccessService accessService;
    private final ProjectMapper mapper;
    private final ObjectMapper objectMapper;

    public ProjectActivityService(
            ProjectActivityRepository activityRepository,
            ProjectAccessService accessService,
            ProjectMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.activityRepository = activityRepository;
        this.accessService = accessService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(ProjectEntity project, ProjectActivityType type, String description, Map<String, Object> metadata) {
        UUID userId = null;
        try {
            userId = accessService.requirePrincipal().getId();
        } catch (RuntimeException ignored) {
            // system-generated rows may omit an actor
        }
        ProjectActivityEntity row = new ProjectActivityEntity();
        row.setProject(project);
        row.setUserId(userId);
        row.setActivityType(type);
        row.setDescription(description);
        row.setMetadata(serialize(metadata));
        activityRepository.save(row);
        log.debug("project activity type={} projectId={}", type, project.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectActivityResponse> list(UUID projectId, int page, int size) {
        accessService.requireReadable(projectId);
        Page<ProjectActivityEntity> result = activityRepository.findByProjectIdOrderByCreatedAtDesc(
                projectId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toActivity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ProjectActivityResponse> recent(UUID projectId) {
        return activityRepository.findTop8ByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(mapper::toActivity)
                .toList();
    }

    private String serialize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
