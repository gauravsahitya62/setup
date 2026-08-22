package com.flowset.planner.projects.service;

import com.flowset.common.ProjectActivityType;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.planner.projects.dto.AddMoodBoardItemRequest;
import com.flowset.planner.projects.dto.MoodBoardItemResponse;
import com.flowset.planner.projects.dto.MoodBoardReorderRequest;
import com.flowset.planner.projects.dto.MoodBoardResponse;
import com.flowset.planner.projects.dto.UpdateMoodBoardItemRequest;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectMoodBoardEntity;
import com.flowset.planner.projects.entity.ProjectMoodBoardItemEntity;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectMoodBoardItemRepository;
import com.flowset.planner.projects.repository.ProjectMoodBoardRepository;
import com.flowset.storage.AllowedContentTypes;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProjectMoodBoardService {

    private final ProjectMoodBoardRepository boardRepository;
    private final ProjectMoodBoardItemRepository itemRepository;
    private final ProjectAccessService accessService;
    private final ProjectActivityService activityService;
    private final ProjectMapper mapper;
    private final StorageService storageService;

    public ProjectMoodBoardService(
            ProjectMoodBoardRepository boardRepository,
            ProjectMoodBoardItemRepository itemRepository,
            ProjectAccessService accessService,
            ProjectActivityService activityService,
            ProjectMapper mapper,
            StorageService storageService
    ) {
        this.boardRepository = boardRepository;
        this.itemRepository = itemRepository;
        this.accessService = accessService;
        this.activityService = activityService;
        this.mapper = mapper;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public MoodBoardResponse get(UUID projectId) {
        accessService.requireReadable(projectId);
        ProjectMoodBoardEntity board = requireBoard(projectId);
        return mapper.toMoodBoard(board, itemRepository.findByMoodBoardIdOrderByDisplayOrderAscCreatedAtAsc(board.getId()));
    }

    @Transactional
    public MoodBoardItemResponse addUploaded(UUID projectId, MultipartFile file, String caption, String sourceUrl) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectMoodBoardEntity board = requireBoard(projectId);
        StoredObject stored = storageService.store(
                file, "projects/" + projectId + "/mood-board", AllowedContentTypes.IMAGES);
        return persistItem(project, board, stored.publicUrl(), sourceUrl, caption);
    }

    @Transactional
    public MoodBoardItemResponse addUrl(UUID projectId, AddMoodBoardItemRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        if (!StringUtils.hasText(request.imageUrl())) {
            throw new BadRequestException("Image URL or file is required");
        }
        if (!request.imageUrl().startsWith("http://") && !request.imageUrl().startsWith("https://")) {
            throw new BadRequestException("Image URL must be http or https");
        }
        ProjectMoodBoardEntity board = requireBoard(projectId);
        return persistItem(project, board, request.imageUrl().trim(), request.sourceUrl(), request.caption());
    }

    @Transactional
    public MoodBoardItemResponse update(UUID projectId, UUID itemId, UpdateMoodBoardItemRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectMoodBoardEntity board = requireBoard(projectId);
        ProjectMoodBoardItemEntity item = itemRepository
                .findByIdAndMoodBoardId(itemId, board.getId())
                .orElseThrow(() -> new NotFoundException("Mood board item not found"));
        if (request.caption() != null) {
            item.setCaption(request.caption());
        }
        if (request.sourceUrl() != null) {
            item.setSourceUrl(request.sourceUrl());
        }
        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }
        ProjectMoodBoardItemEntity saved = itemRepository.save(item);
        activityService.record(project, ProjectActivityType.MOOD_BOARD_ITEM_UPDATED, "Updated mood board image", Map.of());
        return mapper.toMoodItem(saved);
    }

    @Transactional
    public void reorder(UUID projectId, MoodBoardReorderRequest request) {
        accessService.requireWritable(projectId);
        ProjectMoodBoardEntity board = requireBoard(projectId);
        List<ProjectMoodBoardItemEntity> items =
                itemRepository.findByMoodBoardIdOrderByDisplayOrderAscCreatedAtAsc(board.getId());
        if (items.size() != request.itemIds().size()) {
            throw new BadRequestException("Item order must include every mood board image");
        }
        Map<UUID, Integer> order = new java.util.LinkedHashMap<>();
        for (int i = 0; i < request.itemIds().size(); i++) {
            order.put(request.itemIds().get(i), i);
        }
        for (ProjectMoodBoardItemEntity item : items) {
            Integer next = order.get(item.getId());
            if (next == null) {
                throw new BadRequestException("Unknown mood board item in reorder list");
            }
            item.setDisplayOrder(next);
        }
        itemRepository.saveAll(items);
    }

    @Transactional
    public void delete(UUID projectId, UUID itemId) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectMoodBoardEntity board = requireBoard(projectId);
        ProjectMoodBoardItemEntity item = itemRepository
                .findByIdAndMoodBoardId(itemId, board.getId())
                .orElseThrow(() -> new NotFoundException("Mood board item not found"));
        storageService.delete(ProjectService.extractRelative(item.getImageUrl()));
        itemRepository.delete(item);
        activityService.record(project, ProjectActivityType.MOOD_BOARD_ITEM_REMOVED, "Removed mood board image", Map.of());
    }

    private MoodBoardItemResponse persistItem(
            ProjectEntity project,
            ProjectMoodBoardEntity board,
            String imageUrl,
            String sourceUrl,
            String caption
    ) {
        int nextOrder = (int) itemRepository.countByMoodBoardId(board.getId());
        ProjectMoodBoardItemEntity item = new ProjectMoodBoardItemEntity();
        item.setMoodBoard(board);
        item.setImageUrl(imageUrl);
        item.setSourceUrl(StringUtils.hasText(sourceUrl) ? sourceUrl.trim() : null);
        item.setCaption(StringUtils.hasText(caption) ? caption.trim() : null);
        item.setDisplayOrder(nextOrder);
        ProjectMoodBoardItemEntity saved = itemRepository.save(item);
        activityService.record(project, ProjectActivityType.MOOD_BOARD_ITEM_ADDED, "Uploaded mood board image", Map.of());
        return mapper.toMoodItem(saved);
    }

    private ProjectMoodBoardEntity requireBoard(UUID projectId) {
        return boardRepository.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Mood board not found"));
    }
}
