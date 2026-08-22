package com.flowset.planner.projects.service;

import com.flowset.common.EventType;
import com.flowset.common.ProjectActivityType;
import com.flowset.common.ProjectStatus;
import com.flowset.common.RoleName;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.planner.projects.dto.CreateProjectRequest;
import com.flowset.planner.projects.dto.ProjectCounts;
import com.flowset.planner.projects.dto.ProjectProgressResponse;
import com.flowset.planner.projects.dto.ProjectResponse;
import com.flowset.planner.projects.dto.ProjectSummaryResponse;
import com.flowset.planner.projects.dto.UpdateProjectRequest;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectMoodBoardEntity;
import com.flowset.planner.projects.exception.ProjectNotFoundException;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectFileRepository;
import com.flowset.planner.projects.repository.ProjectMoodBoardItemRepository;
import com.flowset.planner.projects.repository.ProjectMoodBoardRepository;
import com.flowset.planner.projects.repository.ProjectNoteRepository;
import com.flowset.planner.projects.repository.ProjectProductRepository;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.planner.projects.repository.ProjectSpecifications;
import com.flowset.planner.projects.repository.ProjectVendorRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.SlugUtils;
import com.flowset.shared.dto.PageResponse;
import com.flowset.storage.AllowedContentTypes;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectProductRepository productRepository;
    private final ProjectVendorRepository vendorRepository;
    private final ProjectMoodBoardRepository moodBoardRepository;
    private final ProjectMoodBoardItemRepository moodBoardItemRepository;
    private final ProjectNoteRepository noteRepository;
    private final ProjectFileRepository fileRepository;
    private final ProjectAccessService accessService;
    private final ProjectActivityService activityService;
    private final ProjectProgressCalculator progressCalculator;
    private final ProjectMapper mapper;
    private final StorageService storageService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectProductRepository productRepository,
            ProjectVendorRepository vendorRepository,
            ProjectMoodBoardRepository moodBoardRepository,
            ProjectMoodBoardItemRepository moodBoardItemRepository,
            ProjectNoteRepository noteRepository,
            ProjectFileRepository fileRepository,
            ProjectAccessService accessService,
            ProjectActivityService activityService,
            ProjectProgressCalculator progressCalculator,
            ProjectMapper mapper,
            StorageService storageService
    ) {
        this.projectRepository = projectRepository;
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.moodBoardRepository = moodBoardRepository;
        this.moodBoardItemRepository = moodBoardItemRepository;
        this.noteRepository = noteRepository;
        this.fileRepository = fileRepository;
        this.accessService = accessService;
        this.activityService = activityService;
        this.progressCalculator = progressCalculator;
        this.mapper = mapper;
        this.storageService = storageService;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        UUID plannerId = accessService.requirePlannerId();
        if (request.startTime() != null && request.endTime() != null && request.endTime().isBefore(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        ProjectEntity project = new ProjectEntity();
        project.setPlannerId(plannerId);
        project.setName(request.name().trim());
        project.setSlug(SlugUtils.uniqueSlug(
                request.name(),
                candidate -> projectRepository.existsByPlannerIdAndSlugIgnoreCase(plannerId, candidate)));
        project.setEventType(request.eventType());
        project.setDescription(blankToNull(request.description()));
        project.setEventDate(request.eventDate());
        project.setStartTime(request.startTime());
        project.setEndTime(request.endTime());
        project.setVenueName(blankToNull(request.venueName()));
        project.setVenueAddress(blankToNull(request.venueAddress()));
        project.setCity(blankToNull(request.city()));
        project.setState(blankToNull(request.state()));
        project.setCountry(blankToNull(request.country()));
        project.setGuestCount(request.guestCount());
        project.setStatus(request.status() == null ? ProjectStatus.DRAFT : request.status());

        ProjectMoodBoardEntity board = new ProjectMoodBoardEntity();
        project.setMoodBoard(board);
        ProjectEntity saved = projectRepository.save(project);

        activityService.record(saved, ProjectActivityType.PROJECT_CREATED, "Project created", Map.of("name", saved.getName()));
        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectSummaryResponse> list(
            String query,
            ProjectStatus status,
            EventType eventType,
            UUID plannerId,
            String sort,
            String direction,
            int page,
            int size
    ) {
        UserPrincipal principal = accessService.requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER && principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Access denied");
        }
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProp = switch (sort == null ? "eventDate" : sort) {
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "name" -> "name";
            default -> "eventDate";
        };
        Sort sortSpec = Sort.by(dir, sortProp).and(Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ProjectEntity> result = projectRepository.findAll(
                ProjectSpecifications.list(principal, query, status, eventType, plannerId),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sortSpec));

        List<ProjectSummaryResponse> content = new ArrayList<>();
        for (ProjectEntity project : result.getContent()) {
            ProjectCounts counts = countsOf(project.getId());
            content.add(mapper.toSummary(project, counts, progressCalculator.calculate(project, counts).percent()));
        }
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID id) {
        return toDetail(accessService.requireReadable(id));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getBySlug(String slug) {
        UserPrincipal principal = accessService.requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            throw new BadRequestException("Admins must look up projects by id");
        }
        ProjectEntity project = projectRepository
                .findActiveBySlugAndPlannerId(slug, principal.getId())
                .orElseThrow(ProjectNotFoundException::new);
        return toDetail(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        ProjectEntity project = accessService.requireWritable(id);
        Map<String, Object> changes = new LinkedHashMap<>();

        if (StringUtils.hasText(request.name()) && !request.name().trim().equals(project.getName())) {
            project.setName(request.name().trim());
            changes.put("name", project.getName());
        }
        if (request.eventType() != null && request.eventType() != project.getEventType()) {
            project.setEventType(request.eventType());
            changes.put("eventType", request.eventType().name());
        }
        if (request.description() != null) {
            project.setDescription(blankToNull(request.description()));
            changes.put("description", true);
        }
        if (request.eventDate() != null && !request.eventDate().equals(project.getEventDate())) {
            project.setEventDate(request.eventDate());
            activityService.record(project, ProjectActivityType.PROJECT_UPDATED, "Updated event date", Map.of("eventDate", request.eventDate().toString()));
        }
        if (request.startTime() != null) {
            project.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            project.setEndTime(request.endTime());
        }
        if (project.getStartTime() != null && project.getEndTime() != null && project.getEndTime().isBefore(project.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        if (request.venueName() != null) {
            project.setVenueName(blankToNull(request.venueName()));
            changes.put("venueName", project.getVenueName());
        }
        if (request.venueAddress() != null) {
            project.setVenueAddress(blankToNull(request.venueAddress()));
        }
        if (request.city() != null) {
            project.setCity(blankToNull(request.city()));
        }
        if (request.state() != null) {
            project.setState(blankToNull(request.state()));
        }
        if (request.country() != null) {
            project.setCountry(blankToNull(request.country()));
        }
        if (request.guestCount() != null) {
            project.setGuestCount(request.guestCount());
            changes.put("guestCount", request.guestCount());
        }
        if (request.status() != null && request.status() != project.getStatus()) {
            ProjectStatus previous = project.getStatus();
            project.setStatus(request.status());
            activityService.record(
                    project,
                    ProjectActivityType.STATUS_CHANGED,
                    "Status changed to " + request.status().name().toLowerCase(),
                    Map.of("from", previous.name(), "to", request.status().name()));
        }

        ProjectEntity saved = projectRepository.save(project);
        if (!changes.isEmpty() && request.status() == null) {
            activityService.record(saved, ProjectActivityType.PROJECT_UPDATED, "Updated project details", changes);
        }
        return toDetail(saved);
    }

    @Transactional
    public void softDelete(UUID id) {
        ProjectEntity project = accessService.requireWritable(id);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
        activityService.record(project, ProjectActivityType.PROJECT_DELETED, "Project deleted", Map.of());
    }

    @Transactional
    public ProjectResponse uploadCover(UUID id, MultipartFile file) {
        ProjectEntity project = accessService.requireWritable(id);
        if (StringUtils.hasText(project.getCoverImageUrl())) {
            storageService.delete(extractRelative(project.getCoverImageUrl()));
        }
        StoredObject stored = storageService.store(file, "projects/" + id + "/cover", AllowedContentTypes.IMAGES);
        project.setCoverImageUrl(stored.publicUrl());
        ProjectEntity saved = projectRepository.save(project);
        activityService.record(saved, ProjectActivityType.COVER_UPDATED, "Updated cover image", Map.of());
        return toDetail(saved);
    }

    ProjectCounts countsOf(UUID projectId) {
        long moodItems = moodBoardRepository.findByProjectId(projectId)
                .map(board -> moodBoardItemRepository.countByMoodBoardId(board.getId()))
                .orElse(0L);
        return new ProjectCounts(
                productRepository.countByProjectId(projectId),
                vendorRepository.countByProjectId(projectId),
                moodItems,
                noteRepository.countByProjectId(projectId),
                fileRepository.countByProjectId(projectId));
    }

    ProjectResponse toDetail(ProjectEntity project) {
        ProjectCounts counts = countsOf(project.getId());
        ProjectProgressResponse progress = progressCalculator.calculate(project, counts);
        return mapper.toResponse(project, counts, progress, activityService.recent(project.getId()));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static String extractRelative(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return null;
        }
        if (publicUrl.startsWith("http://") || publicUrl.startsWith("https://")) {
            String marker = "/api/v1/files/";
            int idx = publicUrl.indexOf(marker);
            if (idx >= 0) {
                return publicUrl.substring(idx + marker.length());
            }
            return null;
        }
        String marker = "/api/v1/files/";
        int idx = publicUrl.indexOf(marker);
        if (idx >= 0) {
            return publicUrl.substring(idx + marker.length());
        }
        return publicUrl;
    }
}
