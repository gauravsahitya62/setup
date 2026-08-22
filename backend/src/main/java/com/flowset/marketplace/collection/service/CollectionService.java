package com.flowset.marketplace.collection.service;

import com.flowset.common.CollectionStatus;
import com.flowset.common.RoleName;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.marketplace.collection.dto.CollectionItemRequest;
import com.flowset.marketplace.collection.dto.CollectionItemResponse;
import com.flowset.marketplace.collection.dto.CollectionRequest;
import com.flowset.marketplace.collection.dto.CollectionResponse;
import com.flowset.marketplace.collection.entity.ProductCollectionEntity;
import com.flowset.marketplace.collection.entity.ProductCollectionItemEntity;
import com.flowset.marketplace.collection.mapper.CollectionMapper;
import com.flowset.marketplace.collection.repository.ProductCollectionItemRepository;
import com.flowset.marketplace.collection.repository.ProductCollectionRepository;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.SlugUtils;
import com.flowset.shared.dto.PageResponse;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CollectionService {

    private final ProductCollectionRepository collectionRepository;
    private final ProductCollectionItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final CollectionMapper collectionMapper;

    public CollectionService(
            ProductCollectionRepository collectionRepository,
            ProductCollectionItemRepository itemRepository,
            ProductRepository productRepository,
            VendorRepository vendorRepository,
            CollectionMapper collectionMapper
    ) {
        this.collectionRepository = collectionRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.collectionMapper = collectionMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CollectionResponse> search(UUID vendorId, int page, int size) {
        UserPrincipal principal = optionalPrincipal();
        boolean includeHidden = principal != null && principal.getRole() == RoleName.ADMIN;
        Page<ProductCollectionEntity> result = collectionRepository.search(
                vendorId,
                includeHidden,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("name")));

        return new PageResponse<>(
                result.getContent().stream().map(collectionMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CollectionResponse getById(UUID id) {
        return collectionMapper.toResponse(requireVisible(id));
    }

    @Transactional(readOnly = true)
    public CollectionResponse getBySlug(String slug) {
        ProductCollectionEntity collection = collectionRepository
                .findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new NotFoundException("Collection not found"));
        assertCanView(collection);
        return collectionMapper.toResponse(collection);
    }

    @Transactional
    public CollectionResponse create(CollectionRequest request) {
        UserPrincipal principal = requirePrincipal();
        VendorEntity vendor = resolveVendorForMutation(principal, request.vendorId());

        ProductCollectionEntity collection = new ProductCollectionEntity();
        collection.setVendor(vendor);
        applyRequest(collection, request, null);
        return collectionMapper.toResponse(collectionRepository.save(collection));
    }

    @Transactional
    public CollectionResponse update(UUID id, CollectionRequest request) {
        ProductCollectionEntity collection = requireEditable(id);
        applyRequest(collection, request, id);
        return collectionMapper.toResponse(collectionRepository.save(collection));
    }

    @Transactional
    public void delete(UUID id) {
        ProductCollectionEntity collection = requireEditable(id);
        itemRepository.findByCollectionIdOrderByDisplayOrderAsc(id).forEach(itemRepository::delete);
        collectionRepository.delete(collection);
    }

    @Transactional(readOnly = true)
    public List<CollectionItemResponse> listItems(UUID collectionId) {
        requireVisible(collectionId);
        return itemRepository.findByCollectionIdOrderByDisplayOrderAsc(collectionId).stream()
                .map(collectionMapper::toItemResponse)
                .toList();
    }

    @Transactional
    public CollectionItemResponse addItem(UUID collectionId, CollectionItemRequest request) {
        requireEditable(collectionId);
        ProductCollectionEntity collection = collectionRepository
                .findById(collectionId)
                .orElseThrow(() -> new NotFoundException("Collection not found"));
        ProductEntity product = productRepository
                .findActiveById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (itemRepository.existsByCollectionIdAndProductId(collectionId, request.productId())) {
            throw new ConflictException("Product already in this collection");
        }
        ProductCollectionItemEntity item = new ProductCollectionItemEntity();
        item.setCollection(collection);
        item.setProduct(product);
        item.setDisplayOrder(
                request.displayOrder() == null
                        ? (int) itemRepository.countByCollectionId(collectionId)
                        : request.displayOrder());
        return collectionMapper.toItemResponse(itemRepository.save(item));
    }

    @Transactional
    public void removeItem(UUID collectionId, UUID productId) {
        requireEditable(collectionId);
        ProductCollectionItemEntity item = itemRepository
                .findByCollectionIdAndProductId(collectionId, productId)
                .orElseThrow(() -> new NotFoundException("Item not found in this collection"));
        itemRepository.delete(item);
    }

    @Transactional
    public List<CollectionItemResponse> reorderItems(UUID collectionId, List<UUID> orderedProductIds) {
        requireEditable(collectionId);
        List<ProductCollectionItemEntity> items = itemRepository.findByCollectionIdOrderByDisplayOrderAsc(collectionId);
        if (items.size() != orderedProductIds.size()) {
            throw new BadRequestException("Reorder list must include every item in this collection");
        }
        for (int i = 0; i < orderedProductIds.size(); i++) {
            UUID productId = orderedProductIds.get(i);
            ProductCollectionItemEntity item = items.stream()
                    .filter(it -> it.getProduct().getId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Unknown product id in reorder list: " + productId));
            item.setDisplayOrder(i);
        }
        itemRepository.saveAll(items);
        return itemRepository.findByCollectionIdOrderByDisplayOrderAsc(collectionId).stream()
                .map(collectionMapper::toItemResponse)
                .toList();
    }

    private void applyRequest(ProductCollectionEntity collection, CollectionRequest request, UUID currentId) {
        String name = request.name().trim();
        collection.setName(name);
        collection.setSlug(resolveSlug(request.slug(), name, collection.getVendor().getId(), currentId));
        collection.setDescription(trimToNull(request.description()));
        collection.setFeaturedImage(trimToNull(request.featuredImage()));
        collection.setStatus(request.status() == null ? CollectionStatus.ACTIVE : request.status());
    }

    private String resolveSlug(String requested, String name, UUID vendorId, UUID currentId) {
        String base = StringUtils.hasText(requested) ? requested.trim().toLowerCase() : name;
        return SlugUtils.uniqueSlug(base, candidate -> {
            if (currentId == null) {
                return collectionRepository.existsByVendorIdAndSlugIgnoreCase(vendorId, candidate);
            }
            return collectionRepository.existsByVendorIdAndSlugIgnoreCaseAndIdNot(vendorId, candidate, currentId);
        });
    }

    private VendorEntity resolveVendorForMutation(UserPrincipal principal, UUID requestedVendorId) {
        if (principal.getRole() == RoleName.ADMIN) {
            if (requestedVendorId == null) {
                throw new BadRequestException("vendorId is required when an admin creates a collection");
            }
            return vendorRepository
                    .findActiveById(requestedVendorId)
                    .orElseThrow(() -> new NotFoundException("Vendor not found"));
        }
        if (principal.getRole() != RoleName.VENDOR) {
            throw new ForbiddenException("Only vendors can create collections");
        }
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .orElseThrow(() -> new NotFoundException("Vendor storefront not found"));
    }

    private ProductCollectionEntity requireVisible(UUID id) {
        ProductCollectionEntity collection = collectionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Collection not found"));
        assertCanView(collection);
        return collection;
    }

    private void assertCanView(ProductCollectionEntity collection) {
        if (collection.getStatus() == CollectionStatus.ACTIVE) {
            return;
        }
        UserPrincipal principal = optionalPrincipal();
        if (principal == null) {
            throw new NotFoundException("Collection not found");
        }
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.VENDOR
                && collection.getVendor().getOwnerUserId().equals(principal.getId())) {
            return;
        }
        throw new NotFoundException("Collection not found");
    }

    private ProductCollectionEntity requireEditable(UUID id) {
        ProductCollectionEntity collection = collectionRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Collection not found"));
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return collection;
        }
        if (principal.getRole() != RoleName.VENDOR
                || !collection.getVendor().getOwnerUserId().equals(principal.getId())) {
            throw new ForbiddenException("Only the owning vendor can modify this collection");
        }
        return collection;
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
}
