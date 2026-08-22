package com.flowset.marketplace.category.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.CategoryStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.marketplace.category.dto.CategoryRequest;
import com.flowset.marketplace.category.dto.CategoryResponse;
import com.flowset.marketplace.category.entity.CategoryEntity;
import com.flowset.marketplace.category.mapper.CategoryMapper;
import com.flowset.marketplace.category.repository.CategoryRepository;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.SlugUtils;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final AuditLogService auditLogService;
    private final StorageService storageService;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CategoryMapper categoryMapper,
            AuditLogService auditLogService,
            StorageService storageService
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
        this.auditLogService = auditLogService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listTree(boolean includeHidden) {
        List<CategoryEntity> roots = categoryRepository.findByParentCategoryIsNullOrderByDisplayOrderAsc();
        return roots.stream()
                .filter(c -> includeHidden || c.getStatus() == CategoryStatus.ACTIVE)
                .map(root -> buildTree(root, includeHidden))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listFlat(boolean includeHidden) {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(c -> includeHidden || c.getStatus() == CategoryStatus.ACTIVE)
                .map(categoryMapper::toResponse)
                .toList();
    }

    private CategoryResponse buildTree(CategoryEntity category, boolean includeHidden) {
        List<CategoryResponse> children = categoryRepository
                .findByParentCategoryIdOrderByDisplayOrderAsc(category.getId())
                .stream()
                .filter(c -> includeHidden || c.getStatus() == CategoryStatus.ACTIVE)
                .map(child -> buildTree(child, includeHidden))
                .toList();
        return categoryMapper.toResponse(category, children);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        return categoryMapper.toResponse(requireById(id));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        return categoryMapper.toResponse(
                categoryRepository
                        .findBySlugIgnoreCase(slug)
                        .orElseThrow(() -> new NotFoundException("Category not found")));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        CategoryEntity category = new CategoryEntity();
        applyRequest(category, request, null);
        CategoryEntity saved = categoryRepository.save(category);
        audit("CATEGORY_CREATED", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        CategoryEntity category = requireById(id);
        applyRequest(category, request, id);
        CategoryEntity saved = categoryRepository.save(category);
        audit("CATEGORY_UPDATED", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse archive(UUID id) {
        CategoryEntity category = requireById(id);
        category.setStatus(CategoryStatus.ARCHIVED);
        CategoryEntity saved = categoryRepository.save(category);
        audit("CATEGORY_ARCHIVED", saved.getId(),
                "Products remain assigned; they are not hidden by this archive");
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public void reorder(List<UUID> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new BadRequestException("orderedIds is required");
        }
        int order = 0;
        for (UUID id : orderedIds) {
            CategoryEntity category = requireById(id);
            category.setDisplayOrder(order++);
            categoryRepository.save(category);
        }
        audit("CATEGORY_REORDERED", orderedIds.get(0), "count=" + orderedIds.size());
    }

    @Transactional
    public CategoryResponse uploadImage(UUID id, MultipartFile file) {
        CategoryEntity category = requireById(id);
        if (StringUtils.hasText(category.getImageUrl())) {
            storageService.delete(extractRelative(category.getImageUrl()));
        }
        StoredObject stored = storageService.store(file, "categories/" + id);
        category.setImageUrl(stored.publicUrl());
        CategoryEntity saved = categoryRepository.save(category);
        audit("CATEGORY_IMAGE_UPDATED", saved.getId(), stored.relativePath());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse setFeatured(UUID id, boolean featured) {
        CategoryEntity category = requireById(id);
        category.setFeatured(featured);
        CategoryEntity saved = categoryRepository.save(category);
        audit(featured ? "CATEGORY_FEATURED" : "CATEGORY_UNFEATURED", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        CategoryEntity category = requireById(id);
        if (categoryRepository.countByParentCategoryId(id) > 0) {
            throw new ConflictException("Cannot delete a category that has subcategories");
        }
        if (productRepository.existsByCategoryIdOrSubcategoryId(id, id)) {
            throw new ConflictException(
                    "Cannot delete a category that has products. Archive it or reassign the products first.");
        }
        categoryRepository.delete(category);
        audit("CATEGORY_DELETED", id, category.getName());
    }

    private void applyRequest(CategoryEntity category, CategoryRequest request, UUID currentId) {
        String name = request.name().trim();
        category.setName(name);
        category.setSlug(resolveSlug(request.slug(), name, currentId));
        category.setDescription(trimToNull(request.description()));
        category.setIcon(trimToNull(request.icon()));
        if (StringUtils.hasText(request.imageUrl())) {
            category.setImageUrl(request.imageUrl().trim());
        }
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        category.setStatus(request.status() == null ? CategoryStatus.ACTIVE : request.status());
        if (request.featured() != null) {
            category.setFeatured(request.featured());
        }

        if (request.parentCategoryId() == null) {
            category.setParentCategory(null);
        } else {
            if (currentId != null && request.parentCategoryId().equals(currentId)) {
                throw new BadRequestException("A category cannot be its own parent");
            }
            CategoryEntity parent = categoryRepository
                    .findById(request.parentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Parent category not found"));
            assertNoCycle(currentId, parent);
            category.setParentCategory(parent);
        }
    }

    private void assertNoCycle(UUID currentId, CategoryEntity parent) {
        if (currentId == null) {
            return;
        }
        CategoryEntity cursor = parent;
        Set<UUID> seen = new HashSet<>();
        while (cursor != null) {
            if (currentId.equals(cursor.getId())) {
                throw new BadRequestException("Circular parent relationship is not allowed");
            }
            if (!seen.add(cursor.getId())) {
                break;
            }
            UUID parentId = cursor.getParentCategory() == null ? null : cursor.getParentCategory().getId();
            cursor = parentId == null ? null : categoryRepository.findById(parentId).orElse(null);
        }
    }

    private String resolveSlug(String requested, String name, UUID currentId) {
        String base = StringUtils.hasText(requested) ? requested.trim().toLowerCase() : name;
        return SlugUtils.uniqueSlug(base, candidate -> {
            if (currentId == null) {
                return categoryRepository.existsBySlugIgnoreCase(candidate);
            }
            return categoryRepository.existsBySlugIgnoreCaseAndIdNot(candidate, currentId);
        });
    }

    private CategoryEntity requireById(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void audit(String action, UUID resourceId, String detail) {
        String actor = "system";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            actor = principal.getId().toString();
        }
        auditLogService.log(action, actor, resourceId.toString(), detail);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String extractRelative(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
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
