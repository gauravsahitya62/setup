package com.flowset.marketplace.product.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.ProductStatus;
import com.flowset.common.RoleName;
import com.flowset.common.VendorStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.marketplace.category.entity.CategoryEntity;
import com.flowset.marketplace.category.repository.CategoryRepository;
import com.flowset.marketplace.product.dto.AttributeRequest;
import com.flowset.marketplace.product.dto.AttributeResponse;
import com.flowset.marketplace.product.dto.ImageResponse;
import com.flowset.marketplace.product.dto.ProductRequest;
import com.flowset.marketplace.product.dto.ProductResponse;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.entity.ProductAttributeEntity;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductImageEntity;
import com.flowset.marketplace.product.entity.ProductTagEntity;
import com.flowset.marketplace.product.mapper.ProductMapper;
import com.flowset.marketplace.product.repository.ProductAttributeRepository;
import com.flowset.marketplace.product.repository.ProductImageRepository;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.marketplace.product.repository.ProductTagRepository;
import com.flowset.marketplace.product.repository.ProductVariantRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.SlugUtils;
import com.flowset.shared.dto.PageResponse;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductTagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final ProductMapper productMapper;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            ProductAttributeRepository attributeRepository,
            ProductTagRepository tagRepository,
            CategoryRepository categoryRepository,
            VendorRepository vendorRepository,
            ProductMapper productMapper,
            StorageService storageService,
            AuditLogService auditLogService
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.attributeRepository = attributeRepository;
        this.tagRepository = tagRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
        this.productMapper = productMapper;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        UserPrincipal principal = requirePrincipal();
        VendorEntity vendor = resolveVendorForMutation(principal, request.vendorId());
        CategoryEntity category = requireCategory(request.categoryId());
        CategoryEntity subcategory = request.subcategoryId() == null ? null : requireCategory(request.subcategoryId());

        ProductEntity product = new ProductEntity();
        product.setVendor(vendor);
        applyRequest(product, request, category, subcategory, null);
        ProductEntity saved = productRepository.save(product);
        return toFullResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        ProductEntity product = requireEditable(id);
        CategoryEntity category = requireCategory(request.categoryId());
        CategoryEntity subcategory = request.subcategoryId() == null ? null : requireCategory(request.subcategoryId());
        applyRequest(product, request, category, subcategory, product.getId());
        ProductEntity saved = productRepository.save(product);
        return toFullResponse(saved);
    }

    @Transactional
    public void softDelete(UUID id) {
        ProductEntity product = requireEditable(id);
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse getById(UUID id) {
        ProductEntity product = requireActive(id);
        assertCanView(product);
        maybeIncrementView(product);
        return toFullResponse(requireActive(id));
    }

    @Transactional
    public ProductResponse getBySlug(String slug) {
        ProductEntity product = productRepository
                .findFirstActiveBySlugIgnoreCase(slug)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        assertCanView(product);
        maybeIncrementView(product);
        return toFullResponse(productRepository
                .findFirstActiveBySlugIgnoreCase(slug)
                .orElseThrow(() -> new NotFoundException("Product not found")));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listByVendor(UUID vendorId, int page, int size) {
        UserPrincipal principal = optionalPrincipal();
        boolean includeAll = principal != null
                && (principal.getRole() == RoleName.ADMIN
                        || (principal.getRole() == RoleName.VENDOR && isOwner(vendorId, principal)));

        Page<ProductEntity> result = productRepository.findByVendor(
                vendorId,
                includeAll,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));

        return new PageResponse<>(
                result.getContent().stream().map(productMapper::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public ImageResponse addImage(UUID productId, MultipartFile file, String altText) {
        ProductEntity product = requireEditable(productId);
        StoredObject stored = storageService.store(file, "products/" + productId + "/images");
        ProductImageEntity image = new ProductImageEntity();
        image.setProduct(product);
        image.setImageUrl(stored.publicUrl());
        image.setAltText(trimToNull(altText));
        image.setDisplayOrder((int) imageRepository.countByProductId(productId));
        image.setFeatured(imageRepository.countByProductId(productId) == 0);
        ProductImageEntity saved = imageRepository.save(image);
        if (saved.isFeatured() && !StringUtils.hasText(product.getFeaturedImage())) {
            product.setFeaturedImage(saved.getImageUrl());
            productRepository.save(product);
        }
        return productMapper.toImage(saved);
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        requireEditable(productId);
        ProductImageEntity image = imageRepository
                .findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Image not found"));
        storageService.delete(extractRelative(image.getImageUrl()));
        imageRepository.delete(image);
    }

    @Transactional
    public List<ImageResponse> reorderImages(UUID productId, List<UUID> orderedIds) {
        requireEditable(productId);
        List<ProductImageEntity> images = imageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        if (images.size() != orderedIds.size()) {
            throw new BadRequestException("Reorder list must include every image for this product");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID imageId = orderedIds.get(i);
            ProductImageEntity image = images.stream()
                    .filter(img -> img.getId().equals(imageId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Unknown image id in reorder list: " + imageId));
            image.setDisplayOrder(i);
        }
        imageRepository.saveAll(images);
        return imageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .map(productMapper::toImage)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttributeResponse> listAttributes(UUID productId) {
        requireActive(productId);
        return attributeRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .map(productMapper::toAttribute)
                .toList();
    }

    @Transactional
    public AttributeResponse addAttribute(UUID productId, AttributeRequest request) {
        ProductEntity product = requireEditable(productId);
        ProductAttributeEntity attribute = new ProductAttributeEntity();
        attribute.setProduct(product);
        attribute.setAttributeName(request.attributeName().trim());
        attribute.setAttributeValue(request.attributeValue().trim());
        attribute.setDisplayOrder(
                request.displayOrder() == null
                        ? (int) attributeRepository.countByProductId(productId)
                        : request.displayOrder());
        return productMapper.toAttribute(attributeRepository.save(attribute));
    }

    @Transactional
    public AttributeResponse updateAttribute(UUID productId, UUID attributeId, AttributeRequest request) {
        requireEditable(productId);
        ProductAttributeEntity attribute = attributeRepository
                .findByIdAndProductId(attributeId, productId)
                .orElseThrow(() -> new NotFoundException("Attribute not found"));
        attribute.setAttributeName(request.attributeName().trim());
        attribute.setAttributeValue(request.attributeValue().trim());
        if (request.displayOrder() != null) {
            attribute.setDisplayOrder(request.displayOrder());
        }
        return productMapper.toAttribute(attributeRepository.save(attribute));
    }

    @Transactional
    public void deleteAttribute(UUID productId, UUID attributeId) {
        requireEditable(productId);
        ProductAttributeEntity attribute = attributeRepository
                .findByIdAndProductId(attributeId, productId)
                .orElseThrow(() -> new NotFoundException("Attribute not found"));
        attributeRepository.delete(attribute);
    }

    ProductEntity requireActive(UUID id) {
        return productRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    ProductEntity requireEditable(UUID id) {
        ProductEntity product = requireActive(id);
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return product;
        }
        if (principal.getRole() != RoleName.VENDOR || !product.getVendor().getOwnerUserId().equals(principal.getId())) {
            throw new ForbiddenException("Only the owning vendor can modify this product");
        }
        return product;
    }

    private VendorEntity resolveVendorForMutation(UserPrincipal principal, UUID requestedVendorId) {
        if (principal.getRole() == RoleName.ADMIN) {
            if (requestedVendorId == null) {
                throw new BadRequestException("vendorId is required when an admin creates a product");
            }
            return vendorRepository
                    .findActiveById(requestedVendorId)
                    .orElseThrow(() -> new NotFoundException("Vendor not found"));
        }
        if (principal.getRole() != RoleName.VENDOR) {
            throw new ForbiddenException("Only vendors can create products");
        }
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .orElseThrow(() -> new NotFoundException("Vendor storefront not found"));
    }

    @Transactional
    public ProductResponse moderate(UUID id, ProductStatus target, String reason, Boolean featured) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Only admins can moderate products");
        }
        ProductEntity product = requireActive(id);
        if (featured != null) {
            product.setFeatured(featured);
            auditLogService.log(
                    featured ? "PRODUCT_FEATURED" : "PRODUCT_UNFEATURED",
                    principal.getId().toString(),
                    id.toString(),
                    product.getName());
        }
        if (target != null) {
            if (target == ProductStatus.REJECTED && !StringUtils.hasText(reason)) {
                throw new BadRequestException("A reason is required to reject a product");
            }
            product.setStatus(target);
            product.setModerationReason(trimToNull(reason));
            String action = switch (target) {
                case PUBLISHED -> product.getModerationReason() == null && reason == null
                        ? "PRODUCT_APPROVED"
                        : "PRODUCT_RESTORED";
                case REJECTED -> "PRODUCT_REJECTED";
                case SUSPENDED -> "PRODUCT_SUSPENDED";
                default -> "PRODUCT_MODERATED";
            };
            if (target == ProductStatus.PUBLISHED) {
                product.setModerationReason(null);
                action = "PRODUCT_APPROVED";
            }
            auditLogService.log(action, principal.getId().toString(), id.toString(), reason);
        }
        productRepository.save(product);
        return toFullResponse(product);
    }

    private void maybeIncrementView(ProductEntity product) {
        if (!isPubliclyVisible(product)) {
            return;
        }
        UserPrincipal principal = optionalPrincipal();
        if (principal != null && principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal != null
                && principal.getRole() == RoleName.VENDOR
                && product.getVendor().getOwnerUserId().equals(principal.getId())) {
            return;
        }
        productRepository.incrementViewCount(product.getId());
    }

    private boolean isOwner(UUID vendorId, UserPrincipal principal) {
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .map(v -> v.getId().equals(vendorId))
                .orElse(false);
    }

    private boolean isPubliclyVisible(ProductEntity product) {
        VendorEntity vendor = product.getVendor();
        return product.getStatus() == ProductStatus.PUBLISHED
                && product.getDeletedAt() == null
                && vendor != null
                && vendor.getDeletedAt() == null
                && vendor.getStatus() != VendorStatus.SUSPENDED;
    }

    private void assertCanView(ProductEntity product) {
        if (isPubliclyVisible(product)) {
            return;
        }
        UserPrincipal principal = optionalPrincipal();
        if (principal == null) {
            throw new NotFoundException("Product not found");
        }
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.VENDOR && product.getVendor().getOwnerUserId().equals(principal.getId())) {
            return;
        }
        throw new NotFoundException("Product not found");
    }

    private void applyRequest(
            ProductEntity product,
            ProductRequest request,
            CategoryEntity category,
            CategoryEntity subcategory,
            UUID currentId
    ) {
        String name = request.name().trim();
        product.setName(name);
        product.setSlug(resolveSlug(request.slug(), name, product.getVendor().getId(), currentId));
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setShortDescription(trimToNull(request.shortDescription()));
        product.setDescription(trimToNull(request.description()));
        product.setPricePerDay(request.pricePerDay());
        product.setSecurityDeposit(request.securityDeposit() == null ? java.math.BigDecimal.ZERO : request.securityDeposit());
        product.setMinimumRentalDays(request.minimumRentalDays() == null ? 1 : request.minimumRentalDays());
        product.setDeliveryAvailable(Boolean.TRUE.equals(request.deliveryAvailable()));
        product.setSetupAvailable(Boolean.TRUE.equals(request.setupAvailable()));
        product.setPickupAvailable(request.pickupAvailable() == null ? true : request.pickupAvailable());

        UserPrincipal principal = optionalPrincipal();
        boolean admin = principal != null && principal.getRole() == RoleName.ADMIN;
        if (admin) {
            product.setFeatured(Boolean.TRUE.equals(request.featured()));
        } else if (currentId == null) {
            product.setFeatured(false);
        }

        ProductStatus requested = request.status() == null ? ProductStatus.DRAFT : request.status();
        if (!admin) {
            if (requested == ProductStatus.PENDING_REVIEW
                    || requested == ProductStatus.REJECTED
                    || requested == ProductStatus.SUSPENDED) {
                throw new BadRequestException("Vendors cannot set this product status");
            }
            if (currentId != null
                    && (product.getStatus() == ProductStatus.SUSPENDED || product.getStatus() == ProductStatus.REJECTED)
                    && requested != product.getStatus()
                    && requested != ProductStatus.ARCHIVED) {
                throw new ForbiddenException("This product is under moderation and cannot be republished");
            }
        }
        product.setStatus(requested);

        if (request.tagIds() != null) {
            Set<ProductTagEntity> tags = new HashSet<>();
            for (UUID tagId : request.tagIds()) {
                tags.add(tagRepository.findById(tagId).orElseThrow(() -> new NotFoundException("Tag not found: " + tagId)));
            }
            product.setTags(tags);
        }
    }

    private String resolveSlug(String requested, String name, UUID vendorId, UUID currentId) {
        String base = StringUtils.hasText(requested) ? requested.trim().toLowerCase() : name;
        return SlugUtils.uniqueSlug(base, candidate -> {
            if (currentId == null) {
                return productRepository.existsByVendorIdAndSlugIgnoreCaseAndDeletedAtIsNull(vendorId, candidate);
            }
            return productRepository.existsByVendorIdAndSlugIgnoreCaseAndIdNotAndDeletedAtIsNull(
                    vendorId, candidate, currentId);
        });
    }

    private CategoryEntity requireCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private ProductResponse toFullResponse(ProductEntity product) {
        UUID id = product.getId();
        return productMapper.toResponse(
                product,
                variantRepository.findByProductIdOrderByCreatedAtAsc(id),
                imageRepository.findByProductIdOrderByDisplayOrderAsc(id),
                attributeRepository.findByProductIdOrderByDisplayOrderAsc(id));
    }

    private UserPrincipal requirePrincipal() {
        UserPrincipal principal = optionalPrincipal();
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    private UserPrincipal optionalPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
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
