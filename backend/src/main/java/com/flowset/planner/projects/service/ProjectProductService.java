package com.flowset.planner.projects.service;

import com.flowset.common.ProductStatus;
import com.flowset.common.ProjectActivityType;
import com.flowset.common.VariantStatus;
import com.flowset.common.VendorStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.marketplace.product.repository.ProductVariantRepository;
import com.flowset.planner.projects.dto.AddProjectProductRequest;
import com.flowset.planner.projects.dto.ProjectProductResponse;
import com.flowset.planner.projects.dto.UpdateProjectProductRequest;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectProductEntity;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectProductRepository;
import com.flowset.vendor.entity.VendorEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shortlist products onto a project. This never calls
 * {@code InventoryAvailabilityService#reserveInventory}.
 */
@Service
public class ProjectProductService {

    private final ProjectProductRepository projectProductRepository;
    private final ProductVariantRepository variantRepository;
    private final ProjectAccessService accessService;
    private final ProjectActivityService activityService;
    private final ProjectMapper mapper;

    public ProjectProductService(
            ProjectProductRepository projectProductRepository,
            ProductVariantRepository variantRepository,
            ProjectAccessService accessService,
            ProjectActivityService activityService,
            ProjectMapper mapper
    ) {
        this.projectProductRepository = projectProductRepository;
        this.variantRepository = variantRepository;
        this.accessService = accessService;
        this.activityService = activityService;
        this.mapper = mapper;
    }

    @Transactional
    public ProjectProductResponse add(UUID projectId, AddProjectProductRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProductVariantEntity variant = variantRepository
                .findWithProductAndVendorById(request.variantId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        ProductEntity product = variant.getProduct();
        if (product.getDeletedAt() != null || product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BadRequestException("This product is not available to add");
        }
        if (variant.getStatus() != VariantStatus.ACTIVE) {
            throw new BadRequestException("This variant is not available");
        }
        VendorEntity vendor = product.getVendor();
        if (vendor.getDeletedAt() != null || vendor.getStatus() == VendorStatus.SUSPENDED) {
            throw new BadRequestException("Vendor unavailable");
        }
        if (projectProductRepository.existsByProjectIdAndVariantId(projectId, variant.getId())) {
            throw new ConflictException("This product is already in the project");
        }

        int quantity = request.quantity() == null ? 1 : request.quantity();
        BigDecimal price = variant.getPricePerDay() != null ? variant.getPricePerDay() : product.getPricePerDay();

        ProjectProductEntity row = new ProjectProductEntity();
        row.setProject(project);
        row.setVariant(variant);
        row.setVendor(vendor);
        row.setQuantity(quantity);
        row.setNotes(request.notes());
        row.setProductName(product.getName());
        row.setProductSlug(product.getSlug());
        row.setVariantName(variant.getVariantName());
        row.setSku(variant.getSku());
        row.setImageUrl(product.getFeaturedImage());
        row.setUnitPricePerDay(price);
        row.setCurrency("INR");
        ProjectProductEntity saved = projectProductRepository.save(row);

        activityService.record(
                project,
                ProjectActivityType.PRODUCT_ADDED,
                "Added " + product.getName(),
                Map.of("variantId", variant.getId().toString(), "quantity", quantity));
        return mapper.toProduct(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectProductResponse> list(UUID projectId) {
        accessService.requireReadable(projectId);
        return projectProductRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(mapper::toProduct)
                .toList();
    }

    @Transactional
    public ProjectProductResponse update(UUID projectId, UUID itemId, UpdateProjectProductRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectProductEntity row = projectProductRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Project product not found"));
        if (request.quantity() != null) {
            row.setQuantity(request.quantity());
        }
        if (request.notes() != null) {
            row.setNotes(request.notes());
        }
        ProjectProductEntity saved = projectProductRepository.save(row);
        activityService.record(
                project,
                ProjectActivityType.PRODUCT_UPDATED,
                "Updated " + row.getProductName(),
                Map.of("quantity", saved.getQuantity()));
        return mapper.toProduct(saved);
    }

    @Transactional
    public void remove(UUID projectId, UUID itemId) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectProductEntity row = projectProductRepository
                .findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Project product not found"));
        String name = row.getProductName();
        projectProductRepository.delete(row);
        activityService.record(project, ProjectActivityType.PRODUCT_REMOVED, "Removed " + name, Map.of());
    }
}
