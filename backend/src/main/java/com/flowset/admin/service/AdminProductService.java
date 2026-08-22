package com.flowset.admin.service;

import com.flowset.common.ProductStatus;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.mapper.ProductMapper;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.shared.dto.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public AdminProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> list(
            String q, ProductStatus status, UUID vendorId, UUID categoryId, Boolean featured, int page, int size) {
        Specification<ProductEntity> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (vendorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("vendor").get("id"), vendorId));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("category").get("id"), categoryId),
                    cb.equal(root.get("subcategory").get("id"), categoryId)));
        }
        if (featured != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("featured"), featured));
        }
        if (StringUtils.hasText(q)) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("vendor").get("companyName")), pattern)));
        }
        Page<ProductEntity> result = productRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(
                result.getContent().stream().map(productMapper::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
