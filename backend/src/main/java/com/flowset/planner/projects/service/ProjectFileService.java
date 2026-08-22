package com.flowset.planner.projects.service;

import com.flowset.common.ProjectActivityType;
import com.flowset.exceptions.NotFoundException;
import com.flowset.planner.projects.dto.ProjectFileResponse;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectFileEntity;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectFileRepository;
import com.flowset.storage.AllowedContentTypes;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import com.flowset.users.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProjectFileService {

    private final ProjectFileRepository fileRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final ProjectActivityService activityService;
    private final ProjectMapper mapper;
    private final StorageService storageService;

    public ProjectFileService(
            ProjectFileRepository fileRepository,
            UserRepository userRepository,
            ProjectAccessService accessService,
            ProjectActivityService activityService,
            ProjectMapper mapper,
            StorageService storageService
    ) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.activityService = activityService;
        this.mapper = mapper;
        this.storageService = storageService;
    }

    @Transactional
    public ProjectFileResponse upload(UUID projectId, MultipartFile file) {
        ProjectEntity project = accessService.requireWritable(projectId);
        UUID userId = accessService.requirePrincipal().getId();
        StoredObject stored = storageService.store(
                file, "projects/" + projectId + "/files", AllowedContentTypes.PROJECT_DOCUMENTS);
        ProjectFileEntity row = new ProjectFileEntity();
        row.setProject(project);
        row.setFileUrl(stored.publicUrl());
        row.setFileName(stored.originalFilename());
        row.setFileType(stored.contentType());
        row.setFileSize(stored.sizeBytes());
        row.setUploadedBy(userId);
        ProjectFileEntity saved = fileRepository.save(row);
        activityService.record(
                project,
                ProjectActivityType.FILE_UPLOADED,
                "Uploaded " + saved.getFileName(),
                Map.of("fileName", saved.getFileName()));
        return mapper.toFile(saved, userRepository.findActiveById(userId).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> list(UUID projectId) {
        accessService.requireReadable(projectId);
        return fileRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(file -> mapper.toFile(file, userRepository.findActiveById(file.getUploadedBy()).orElse(null)))
                .toList();
    }

    @Transactional
    public void delete(UUID projectId, UUID fileId) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectFileEntity row = fileRepository
                .findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new NotFoundException("File not found"));
        String name = row.getFileName();
        storageService.delete(ProjectService.extractRelative(row.getFileUrl()));
        fileRepository.delete(row);
        activityService.record(project, ProjectActivityType.FILE_DELETED, "Removed " + name, Map.of());
    }
}
