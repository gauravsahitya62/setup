package com.flowset.planner.projects.service;

import com.flowset.common.ProjectActivityType;
import com.flowset.exceptions.NotFoundException;
import com.flowset.planner.projects.dto.CreateNoteRequest;
import com.flowset.planner.projects.dto.ProjectNoteResponse;
import com.flowset.planner.projects.dto.UpdateNoteRequest;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectNoteEntity;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectNoteRepository;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectNoteService {

    private final ProjectNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final ProjectActivityService activityService;
    private final ProjectMapper mapper;

    public ProjectNoteService(
            ProjectNoteRepository noteRepository,
            UserRepository userRepository,
            ProjectAccessService accessService,
            ProjectActivityService activityService,
            ProjectMapper mapper
    ) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.activityService = activityService;
        this.mapper = mapper;
    }

    @Transactional
    public ProjectNoteResponse create(UUID projectId, CreateNoteRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        UUID userId = accessService.requirePrincipal().getId();
        ProjectNoteEntity note = new ProjectNoteEntity();
        note.setProject(project);
        note.setTitle(request.title().trim());
        note.setContent(request.content().trim());
        note.setPinned(Boolean.TRUE.equals(request.pinned()));
        note.setCreatedBy(userId);
        ProjectNoteEntity saved = noteRepository.save(note);
        activityService.record(project, ProjectActivityType.NOTE_CREATED, "Added note \"" + saved.getTitle() + "\"", Map.of());
        return mapper.toNote(saved, userRepository.findActiveById(userId).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ProjectNoteResponse> list(UUID projectId) {
        accessService.requireReadable(projectId);
        return noteRepository.findByProjectIdOrderByPinnedDescUpdatedAtDesc(projectId).stream()
                .map(note -> mapper.toNote(note, authorOf(note.getCreatedBy())))
                .toList();
    }

    @Transactional
    public ProjectNoteResponse update(UUID projectId, UUID noteId, UpdateNoteRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectNoteEntity note = noteRepository
                .findByIdAndProjectId(noteId, projectId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        if (StringUtils.hasText(request.title())) {
            note.setTitle(request.title().trim());
        }
        if (request.content() != null) {
            note.setContent(request.content().trim());
        }
        if (request.pinned() != null) {
            note.setPinned(request.pinned());
        }
        ProjectNoteEntity saved = noteRepository.save(note);
        activityService.record(project, ProjectActivityType.NOTE_UPDATED, "Updated note \"" + saved.getTitle() + "\"", Map.of());
        return mapper.toNote(saved, authorOf(saved.getCreatedBy()));
    }

    @Transactional
    public void delete(UUID projectId, UUID noteId) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectNoteEntity note = noteRepository
                .findByIdAndProjectId(noteId, projectId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        String title = note.getTitle();
        noteRepository.delete(note);
        activityService.record(project, ProjectActivityType.NOTE_DELETED, "Deleted note \"" + title + "\"", Map.of());
    }

    private UserEntity authorOf(UUID userId) {
        return userRepository.findActiveById(userId).orElse(null);
    }
}
