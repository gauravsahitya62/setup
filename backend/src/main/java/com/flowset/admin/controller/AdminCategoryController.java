package com.flowset.admin.controller;

import com.flowset.admin.dto.CategoryReorderRequest;
import com.flowset.admin.dto.FeaturedRequest;
import com.flowset.marketplace.category.dto.CategoryRequest;
import com.flowset.marketplace.category.dto.CategoryResponse;
import com.flowset.marketplace.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "Admin Categories", description = "ADMIN authorization required. Archive keeps products assigned.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all categories including hidden and archived")
    public List<CategoryResponse> list() {
        return categoryService.listTree(true);
    }

    @PostMapping
    @Operation(summary = "Create category")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive category without removing products")
    public CategoryResponse archive(@PathVariable UUID id) {
        return categoryService.archive(id);
    }

    @PostMapping("/reorder")
    @Operation(summary = "Reorder categories")
    public void reorder(@Valid @RequestBody CategoryReorderRequest request) {
        categoryService.reorder(request.orderedIds());
    }

    @PostMapping("/{id}/feature")
    @Operation(summary = "Feature or unfeature category")
    public CategoryResponse feature(@PathVariable UUID id, @RequestBody FeaturedRequest request) {
        return categoryService.setFeatured(id, request.featured());
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload category image")
    public CategoryResponse image(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return categoryService.uploadImage(id, file);
    }
}
