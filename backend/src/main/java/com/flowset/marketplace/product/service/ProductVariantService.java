package com.flowset.marketplace.product.service;

import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.marketplace.inventory.service.InventoryService;
import com.flowset.marketplace.product.dto.VariantRequest;
import com.flowset.marketplace.product.dto.VariantResponse;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.marketplace.product.mapper.ProductMapper;
import com.flowset.marketplace.product.repository.ProductVariantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final ProductMapper productMapper;

    public ProductVariantService(
            ProductVariantRepository variantRepository,
            ProductService productService,
            InventoryService inventoryService,
            ProductMapper productMapper
    ) {
        this.variantRepository = variantRepository;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> list(UUID productId) {
        productService.requireActive(productId);
        return variantRepository.findByProductIdOrderByCreatedAtAsc(productId).stream()
                .map(productMapper::toVariant)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse getById(UUID productId, UUID variantId) {
        return productMapper.toVariant(requireByIdAndProduct(productId, variantId));
    }

    @Transactional
    public VariantResponse create(UUID productId, VariantRequest request) {
        ProductEntity product = productService.requireEditable(productId);
        assertSkuAvailable(request.sku(), null);

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        applyRequest(variant, request);
        variant.setAvailableQuantity(request.quantity());
        ProductVariantEntity saved = variantRepository.save(variant);

        // Every new variant starts with an inventory row whose totals mirror the variant quantity.
        inventoryService.createForVariant(saved, saved.getQuantity());

        return productMapper.toVariant(saved);
    }

    @Transactional
    public VariantResponse update(UUID productId, UUID variantId, VariantRequest request) {
        productService.requireEditable(productId);
        ProductVariantEntity variant = requireByIdAndProduct(productId, variantId);
        assertSkuAvailable(request.sku(), variantId);
        applyRequest(variant, request);
        return productMapper.toVariant(variantRepository.save(variant));
    }

    @Transactional
    public void delete(UUID productId, UUID variantId) {
        productService.requireEditable(productId);
        ProductVariantEntity variant = requireByIdAndProduct(productId, variantId);
        variantRepository.delete(variant);
    }

    private ProductVariantEntity requireByIdAndProduct(UUID productId, UUID variantId) {
        return variantRepository
                .findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new NotFoundException("Variant not found"));
    }

    private void assertSkuAvailable(String sku, UUID currentId) {
        boolean exists = currentId == null
                ? variantRepository.existsBySkuIgnoreCase(sku)
                : variantRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentId);
        if (exists) {
            throw new ConflictException("SKU already in use: " + sku);
        }
    }

    private void applyRequest(ProductVariantEntity variant, VariantRequest request) {
        variant.setVariantName(request.variantName().trim());
        variant.setSku(request.sku().trim());
        variant.setColor(request.color());
        variant.setMaterial(request.material());
        variant.setQuantity(request.quantity());
        variant.setPricePerDay(request.pricePerDay());
        variant.setStatus(request.status() == null ? variant.getStatus() : request.status());
    }
}
