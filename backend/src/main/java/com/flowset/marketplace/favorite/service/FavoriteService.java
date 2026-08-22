package com.flowset.marketplace.favorite.service;

import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.marketplace.favorite.entity.FavoriteProductEntity;
import com.flowset.marketplace.favorite.repository.FavoriteProductRepository;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.mapper.ProductMapper;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final FavoriteProductRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public FavoriteService(
            FavoriteProductRepository favoriteRepository,
            ProductRepository productRepository,
            ProductMapper productMapper
    ) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public void add(UUID productId) {
        UUID plannerUserId = requirePrincipal().getId();
        ProductEntity product = productRepository
                .findActiveById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (favoriteRepository.existsByPlannerUserIdAndProductId(plannerUserId, productId)) {
            throw new ConflictException("Product already in favorites");
        }
        FavoriteProductEntity favorite = new FavoriteProductEntity();
        favorite.setPlannerUserId(plannerUserId);
        favorite.setProduct(product);
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void remove(UUID productId) {
        UUID plannerUserId = requirePrincipal().getId();
        favoriteRepository.deleteByPlannerUserIdAndProductId(plannerUserId, productId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> list(int page, int size) {
        UUID plannerUserId = requirePrincipal().getId();
        Page<FavoriteProductEntity> result = favoriteRepository.findByPlannerUserId(
                plannerUserId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));

        return new PageResponse<>(
                result.getContent().stream().map(f -> productMapper.toSummary(f.getProduct())).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }
}
